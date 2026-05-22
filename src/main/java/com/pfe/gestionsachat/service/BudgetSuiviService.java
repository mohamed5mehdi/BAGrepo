package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.dto.*;
import com.pfe.gestionsachat.exception.BudgetInsuffisantException;
import com.pfe.gestionsachat.exception.EquationBudgetaireException;
import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de suivi et d'imputation budgétaire.
 *
 * Équation fondamentale invariante :
 *   budget_initial (opening_val) = budget_engage (consumed_val) + budget_restant (current_val)
 *
 * Toute rupture de cette équation est loggée dans audit_log et lève une EquationBudgetaireException.
 */
@Service
public class BudgetSuiviService {

    private static final Logger log = LoggerFactory.getLogger(BudgetSuiviService.class);

    /** Seuil d'alerte par défaut : 80 % */
    private static final BigDecimal SEUIL_ALERTE = BigDecimal.valueOf(80);

    /** Tolérance d'arrondi pour la validation de l'équation (2 centimes) */
    private static final BigDecimal TOLERANCE = new BigDecimal("0.02");

    @Autowired private FamilyRepository    familyRepository;
    @Autowired private SubFamilyRepository subFamilyRepository;
    @Autowired private DaHeaderRepository  daHeaderRepository;
    @Autowired private AuditLogRepository  auditLogRepository;

    // ══════════════════════════════════════════════════════════════════════════
    // 1. GET /api/budget/suivi
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Retourne le tableau de bord budgétaire complet :
     * toutes les familles avec leurs métriques et leurs sous-familles.
     */
    public List<BudgetFamilleDto> getSuiviBudgetaire() {
        List<Family> familles = familyRepository.findAll();
        return familles.stream()
                       .map(this::toFamilleDto)
                       .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. POST /api/budget/consommer
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Impute une DA validée sur une sous-famille :
     *   - budget_engage  += montant
     *   - budget_restant -= montant
     * Recalcule ensuite la famille parente par agrégation.
     *
     * @throws BudgetInsuffisantException   si budget_restant SF < montant
     * @throws EquationBudgetaireException  si l'équation est rompue après imputation
     */
    @Transactional
    @Retryable(
        retryFor = { ObjectOptimisticLockingFailureException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2.0)
    )
    public ConsommerBudgetResponse consommerBudget(ConsommerBudgetRequest request) {

        // ── Récupération et validation de la DA ────────────────────────────
        DaHeader da = daHeaderRepository.findById(request.getDaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "DA introuvable : id=" + request.getDaId()));

        if (da.getStatut() != StatutDA.VALIDEE &&
            da.getStatut() != StatutDA.PO_CREE  &&
            da.getStatut() != StatutDA.EN_ATTENTE_ACHAT) {
            throw new IllegalStateException(
                    "La DA [id=" + request.getDaId() + "] n'est pas dans un statut permettant l'imputation (statut=" + da.getStatut() + ")");
        }

        // ── Récupération de la sous-famille avec LOCK ──────────────────────
        SubFamily sf = subFamilyRepository.findByIdWithLock(request.getSousFamilleId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sous-famille introuvable : id=" + request.getSousFamilleId()));

        BigDecimal montant       = request.getMontant();
        BigDecimal engageAvant   = orZero(sf.getBudgetEngage());
        BigDecimal restantAvant  = orZero(sf.getBudgetRestant());

        // ── Contrôle de la suffisance ──────────────────────────────────────
        if (restantAvant.compareTo(montant) < 0) {
            throw new BudgetInsuffisantException(sf.getOidSub(), restantAvant, montant);
        }

        // ── Imputation sur la sous-famille ─────────────────────────────────
        BigDecimal engageApres  = engageAvant.add(montant);
        BigDecimal restantApres = restantAvant.subtract(montant);

        sf.setBudgetEngage(engageApres);
        sf.setBudgetRestant(restantApres);
        subFamilyRepository.save(sf);

        // ── Validation de l'équation sur la sous-famille ───────────────────
        validerEquation("SubFamily", sf.getOidSub(),
                sf.getBudgetInitial(), engageApres, restantApres);

        // ── Recalcul de la famille parente par agrégation ──────────────────
        recalculerFamille(sf.getFamily());

        // ── Construction de la réponse ──────────────────────────────────────
        ConsommerBudgetResponse response = new ConsommerBudgetResponse();
        response.setDaId(da.getOidDa());
        response.setSousFamilleId(sf.getOidSub());
        response.setLibelleSousFamille(sf.getLibelle());
        response.setMontantImpute(montant);
        response.setBudgetEngageAvant(engageAvant);
        response.setBudgetRestantAvant(restantAvant);
        response.setBudgetEngageApres(engageApres);
        response.setBudgetRestantApres(restantApres);
        response.setEquationValidee(true);

        log.info("Imputation réussie : DA={} SF={} montant={}", da.getOidDa(), sf.getOidSub(), montant);
        return response;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. GET /api/budget/famille/{id}
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Retourne le détail d'une famille avec la liste complète de ses sous-familles.
     */
    public BudgetFamilleDto getFamilleDetail(@jakarta.annotation.Nonnull Integer idFamille) {
        Family famille = familyRepository.findById(idFamille)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Famille introuvable : id=" + idFamille));
        return toFamilleDto(famille);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. GET /api/budget/alertes
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Retourne toutes les entités (familles et sous-familles) dont le taux de
     * consommation dépasse {@link #SEUIL_ALERTE}.
     */
    public List<AlerteBudgetaireDto> getAlertes() {
        List<AlerteBudgetaireDto> alertes = new ArrayList<>();

        for (Family f : familyRepository.findAll()) {
            BigDecimal taux = calculerTaux(orZero(f.getBudgetEngage()), orZero(f.getBudgetInitial()));
            if (taux.compareTo(SEUIL_ALERTE) > 0) {
                alertes.add(new AlerteBudgetaireDto(
                        "FAMILLE",
                        f.getIdFamily(),
                        f.getLibelle(),
                        null,
                        orZero(f.getBudgetInitial()),
                        orZero(f.getBudgetEngage()),
                        orZero(f.getBudgetRestant()),
                        taux,
                        SEUIL_ALERTE));
            }

            // Sous-familles de cette famille
            for (SubFamily sf : subFamilyRepository.findByFamilyId(f.getIdFamily())) {
                BigDecimal tauxSf = calculerTaux(orZero(sf.getBudgetEngage()), orZero(sf.getBudgetInitial()));
                if (tauxSf.compareTo(SEUIL_ALERTE) > 0) {
                    alertes.add(new AlerteBudgetaireDto(
                            "SOUS_FAMILLE",
                            sf.getOidSub(),
                            sf.getLibelle(),
                            f.getIdFamily(),
                            orZero(sf.getBudgetInitial()),
                            orZero(sf.getBudgetEngage()),
                            orZero(sf.getBudgetRestant()),
                            tauxSf,
                            SEUIL_ALERTE));
                }
            }
        }

        return alertes;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. GET /api/budget/allocation/verification  &  /{id}
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Vérifie l'invariant Σ SubFamily.budgetInitial = Family.budgetInitial
     * pour toutes les familles.
     *
     * NON BLOQUANT — retourne la liste des écarts pour affichage rouge dans l'UI.
     * Un écart = 0 → niveau "OK". Écart != 0 → niveau "SOUS_ALLOUE" ou "SUR_ALLOUE".
     */
    @Transactional(readOnly = true)
    public List<AlerteAllocationDto> verifierAllocationGlobale() {
        List<AlerteAllocationDto> alertes = new ArrayList<>();
        for (Family f : familyRepository.findAll()) {
            alertes.add(verifierAllocationFamille(f));
        }
        return alertes;
    }

    /**
     * Vérifie l'invariant Σ SF.budgetInitial = Family.budgetInitial pour une famille donnée.
     */
    @Transactional(readOnly = true)
    public AlerteAllocationDto verifierAllocationFamilleParId(Integer familleId) {
        Family famille = familyRepository.findById(familleId)
                .orElseThrow(() -> new IllegalArgumentException("Famille introuvable : id=" + familleId));
        return verifierAllocationFamille(famille);
    }

    /**
     * Construit une AlerteAllocationDto pour une famille donnée.
     * Aussi appelée par DemandeAjustementService après chaque mutation de budgetInitial de SF.
     */
    @Transactional(readOnly = true)
    public AlerteAllocationDto verifierAllocationFamille(Family famille) {
        List<SubFamily> sfs = subFamilyRepository.findByFamilyId(famille.getIdFamily());
        BigDecimal sommeSf = sfs.stream()
                .map(sf -> orZero(sf.getBudgetInitial()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AlerteAllocationDto alerte = new AlerteAllocationDto(
                famille.getIdFamily(),
                famille.getLibelle(),
                orZero(famille.getBudgetInitial()),
                sommeSf);

        if (!"OK".equals(alerte.getNiveau())) {
            log.warn("ALERTE ALLOCATION [Famille id={} '{}'] : budgetInitial={} ≠ Σ SF={} (écart={})",
                    famille.getIdFamily(), famille.getLibelle(),
                    famille.getBudgetInitial(), sommeSf, alerte.getEcart());
        }

        return alerte;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. GET /api/budget/audit
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Audit complet et non bloquant de l'équation budgétaire sur toutes les entités.
     * Vérifie que initial = engage + restant pour toutes les familles et sous-familles.
     * Retourne la liste des anomalies trouvées.
     */
    @Transactional(readOnly = true)
    public List<AuditLog> auditGlobalEquations() {
        List<AuditLog> anomalies = new ArrayList<>();

        // Audit des familles
        for (Family f : familyRepository.findAll()) {
            AuditLog anomalie = verifierEtGenererAnomalie(
                    "Family", f.getIdFamily().longValue(),
                    f.getBudgetInitial(), f.getBudgetEngage(), f.getBudgetRestant());
            if (anomalie != null) anomalies.add(anomalie);
        }

        // Audit des sous-familles
        for (SubFamily sf : subFamilyRepository.findAll()) {
            AuditLog anomalie = verifierEtGenererAnomalie(
                    "SubFamily", sf.getOidSub().longValue(),
                    sf.getBudgetInitial(), sf.getBudgetEngage(), sf.getBudgetRestant());
            if (anomalie != null) anomalies.add(anomalie);
        }

        return anomalies;
    }

    /**
     * Vérifie l'équation pour une entité et retourne une anomalie si l'écart dépasse la tolérance.
     * Ne sauvegarde pas en base, sert juste pour l'endpoint d'audit en direct.
     */
    private AuditLog verifierEtGenererAnomalie(String entite, Long entiteId, BigDecimal budgetInitial, BigDecimal budgetEngage, BigDecimal budgetRestant) {
        if (budgetInitial == null) return null;

        BigDecimal somme = orZero(budgetEngage).add(orZero(budgetRestant));
        BigDecimal ecart = budgetInitial.subtract(somme).abs();

        if (ecart.compareTo(TOLERANCE) > 0) {
            String message = String.format(
                    "ANOMALIE AUDIT [%s id=%s]: initial=%.2f ≠ engage=%.2f + restant=%.2f (Δ=%.2f)",
                    entite, entiteId, budgetInitial, budgetEngage, budgetRestant, ecart);
            return new AuditLog(
                    "ANOMALIE_EQUATION",
                    entite,
                    entiteId,
                    null,
                    String.format("initial=%.2f, engage=%.2f, restant=%.2f", budgetInitial, budgetEngage, budgetRestant),
                    message);
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers internes
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Recalcule les valeurs agrégées d'une famille à partir de ses sous-familles.
     * budget_engage_famille  = Σ budget_engage_sf
     * budget_restant_famille = Σ budget_restant_sf
     * budget_initial_famille = INCHANGÉ (figé à la création / ajustement DG)
     *
     * Méthode publique — appelée par DemandeAjustementService après toute mutation SF.
     */
    @Transactional
    public void recalculerFamille(Family famille) {
        if (famille == null) return;

        // Rechargement avec lock pour éviter les stale reads
        Family familleChargee = familyRepository.findByIdWithLock(famille.getIdFamily())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Famille introuvable pour recalcul : id=" + famille.getIdFamily()));

        List<SubFamily> sousF = subFamilyRepository.findByFamilyId(familleChargee.getIdFamily());

        BigDecimal totalEngage  = sousF.stream()
                .map(sf -> orZero(sf.getBudgetEngage()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Au lieu de sommer le restant des SF (ce qui détruit le budget non alloué de la famille)
        // on calcule le restant de la famille = initial - engage.
        BigDecimal totalRestant = orZero(familleChargee.getBudgetInitial()).subtract(totalEngage);

        familleChargee.setBudgetEngage(totalEngage);
        familleChargee.setBudgetRestant(totalRestant);
        familyRepository.save(familleChargee);

        // Validation de l'équation sur la famille
        validerEquation("Family", familleChargee.getIdFamily(),
                familleChargee.getBudgetInitial(), totalEngage, totalRestant);

        log.debug("Famille {} recalculée : engage={} restant={}", familleChargee.getIdFamily(), totalEngage, totalRestant);
    }

    /**
     * Valide l'équation budgétaire fondamentale.
     * En cas de rupture, logue dans audit_log puis lève une exception.
     */
    private void validerEquation(String entite, Object entiteId,
                                  BigDecimal budgetInitial,
                                  BigDecimal budgetEngage,
                                  BigDecimal budgetRestant) {
        if (budgetInitial == null) return; // entité sans budget défini

        BigDecimal somme = orZero(budgetEngage).add(orZero(budgetRestant));
        BigDecimal ecart = budgetInitial.subtract(somme).abs();

        if (ecart.compareTo(TOLERANCE) > 0) {
            // Loggage dans audit_log
            String message = String.format(
                    "RUPTURE ÉQUATION [%s id=%s]: initial=%.2f ≠ engage=%.2f + restant=%.2f (Δ=%.2f)",
                    entite, entiteId, budgetInitial, budgetEngage, budgetRestant, ecart);

            log.error(message);

            Long entiteIdLong = (entiteId instanceof Integer i) ? i.longValue() : (Long) entiteId;
            AuditLog audit = new AuditLog(
                    "EQUATION_BUDGETAIRE_ROMPUE",
                    entite,
                    entiteIdLong,
                    null,  // système, pas d'utilisateur
                    String.format("initial=%.2f, engage=%.2f, restant=%.2f", budgetInitial, budgetEngage, budgetRestant),
                    message);
            auditLogRepository.save(audit);
            // Note : entiteId est Integer ou Long selon le contexte

            throw new EquationBudgetaireException(
                    entite, entiteId, budgetInitial,
                    orZero(budgetEngage), orZero(budgetRestant));
        }
    }

    // ── Conversion Family → DTO ────────────────────────────────────────────────

    private BudgetFamilleDto toFamilleDto(Family f) {
        List<SubFamily> sfs = subFamilyRepository.findByFamilyId(f.getIdFamily());
        List<BudgetSousFamilleDto> sfDtos = sfs.stream()
                .map(sf -> toSousFamilleDto(sf, f))
                .collect(Collectors.toList());

        return new BudgetFamilleDto(
                f.getIdFamily(),
                f.getLibelle(),
                orZero(f.getBudgetInitial()),   // opening_val
                orZero(f.getBudgetEngage()),     // consumed_val
                orZero(f.getBudgetRestant()),    // current_val
                sfDtos);
    }

    private BudgetSousFamilleDto toSousFamilleDto(SubFamily sf, Family f) {
        return new BudgetSousFamilleDto(
                sf.getOidSub(),
                sf.getLibelle(),
                f.getIdFamily(),
                f.getLibelle(),
                orZero(sf.getBudgetInitial()),  // opening_val
                orZero(sf.getBudgetEngage()),   // consumed_val
                orZero(sf.getBudgetRestant())); // current_val
    }

    // ── Utilitaires ─────────────────────────────────────────────────────────────

    private static BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal calculerTaux(BigDecimal consomme, BigDecimal initial) {
        if (initial == null || initial.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return consomme.multiply(BigDecimal.valueOf(100))
                       .divide(initial, 2, RoundingMode.HALF_UP);
    }
}

package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.dto.*;
import com.pfe.gestionsachat.exception.BudgetInsuffisantException;
import com.pfe.gestionsachat.exception.EquationBudgetaireException;
import com.pfe.gestionsachat.service.BudgetSuiviService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller REST – Suivi et imputation budgétaire
 *
 * Base URL : /api/budget
 *
 * Équation fondamentale garantie :
 *   opening_val (budget_initial) = consumed_val (budget_engage) + current_val (budget_restant)
 */
@RestController
@RequestMapping("/api/budget")

public class BudgetController {

    private static final Logger log = LoggerFactory.getLogger(BudgetController.class);

    @Autowired
    private BudgetSuiviService budgetSuiviService;

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/budget/suivi
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Tableau de bord budgétaire global.
     *
     * Retourne pour chaque famille :
     *   - opening_val  : budget initial (figé)
     *   - consumed_val : budget engagé (Σ imputations)
     *   - current_val  : budget restant
     *   - taux_consommation : consumed_val / opening_val × 100
     *   - sous_familles : la même décomposition par sous-famille
     *
     * Rôles autorisés : FINANCIER, ADMIN, DAF, DG
     */
    @GetMapping("/suivi")
    @PreAuthorize("hasAnyRole('FINANCIER','ADMIN','DAF','DG')")
    public ResponseEntity<List<BudgetFamilleDto>> getSuiviBudgetaire() {
        log.info("GET /api/budget/suivi");
        List<BudgetFamilleDto> suivi = budgetSuiviService.getSuiviBudgetaire();
        return ResponseEntity.ok(suivi);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /api/budget/consommer
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Imputation d'une DA validée sur une sous-famille.
     *
     * Corps de la requête :
     * {
     *   "da_id"          : 42,
     *   "sous_famille_id": 7,
     *   "montant"        : 5000.00
     * }
     *
     * Règles métier :
     *  1. La DA doit être dans un statut permettant l'imputation (VALIDEE_TECH / EN_TRAITEMENT / VALIDEE_FINALE)
     *  2. budget_restant de la SF doit être ≥ montant (sinon BudgetInsuffisantException)
     *  3. Après imputation : budget_engage += montant, budget_restant -= montant
     *  4. La famille parente est recalculée par agrégation
     *  5. L'équation est vérifiée ; toute rupture est loggée dans audit_log
     *
     * Rôles autorisés : FINANCIER, ADMIN
     */
    @PostMapping("/consommer")
    @PreAuthorize("hasAnyRole('FINANCIER','ADMIN')")
    public ResponseEntity<?> consommerBudget(@Valid @RequestBody ConsommerBudgetRequest request) {
        log.info("POST /api/budget/consommer – DA={} SF={} montant={}",
                request.getDaId(), request.getSousFamilleId(), request.getMontant());
        try {
            ConsommerBudgetResponse response = budgetSuiviService.consommerBudget(request);
            return ResponseEntity.ok(response);

        } catch (BudgetInsuffisantException e) {
            log.warn("Budget insuffisant : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(erreur("BUDGET_INSUFFISANT", e.getMessage(),
                            Map.of(
                                    "sous_famille_id", e.getSousFamilleId(),
                                    "budget_restant",  e.getBudgetRestant(),
                                    "montant_demande", e.getMontantDemande())));

        } catch (EquationBudgetaireException e) {
            log.error("Rupture équation budgétaire : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(erreur("EQUATION_BUDGETAIRE_ROMPUE", e.getMessage(),
                            Map.of(
                                    "entite",         e.getEntite(),
                                    "entite_id",      e.getEntiteId(),
                                    "opening_val",    e.getBudgetInitial(),
                                    "consumed_val",   e.getBudgetEngage(),
                                    "current_val",    e.getBudgetRestant())));

        } catch (IllegalArgumentException e) {
            log.warn("Argument invalide : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(erreur("RESSOURCE_INTROUVABLE", e.getMessage(), null));

        } catch (IllegalStateException e) {
            log.warn("Statut DA incompatible : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(erreur("STATUT_DA_INCOMPATIBLE", e.getMessage(), null));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/budget/famille/{id}
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Détail d'une famille budgétaire avec toutes ses sous-familles.
     *
     * Rôles autorisés : FINANCIER, ADMIN, DAF, DG, ACHETEUR
     */
    @GetMapping("/famille/{id}")
    @PreAuthorize("hasAnyRole('FINANCIER','ADMIN','DAF','DG','ACHETEUR')")
    public ResponseEntity<?> getFamilleDetail(@PathVariable Integer id) {
        log.info("GET /api/budget/famille/{}", id);
        try {
            BudgetFamilleDto detail = budgetSuiviService.getFamilleDetail(id);
            return ResponseEntity.ok(detail);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(erreur("RESSOURCE_INTROUVABLE", e.getMessage(), null));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/budget/alertes
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Liste toutes les familles et sous-familles dont le taux de consommation
     * dépasse 80 % (seuil configurable dans BudgetSuiviService.SEUIL_ALERTE).
     *
     * Chaque alerte expose :
     *   - type_entite       : "FAMILLE" ou "SOUS_FAMILLE"
     *   - opening_val / consumed_val / current_val
     *   - taux_consommation : en pourcentage
     *   - seuil_alerte      : valeur du seuil déclencheur
     *
     * Rôles autorisés : FINANCIER, ADMIN, DAF, DG
     */
    @GetMapping("/alertes")
    @PreAuthorize("hasAnyRole('FINANCIER','ADMIN','DAF','DG')")
    public ResponseEntity<List<AlerteBudgetaireDto>> getAlertes() {
        log.info("GET /api/budget/alertes");
        List<AlerteBudgetaireDto> alertes = budgetSuiviService.getAlertes();
        return ResponseEntity.ok(alertes);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helper – structure d'erreur uniforme
    // ══════════════════════════════════════════════════════════════════════════

    private Map<String, Object> erreur(String code, String message, Map<String, Object> details) {
        if (details != null) {
            return Map.of(
                    "code",      code,
                    "message",   message,
                    "details",   details,
                    "timestamp", LocalDateTime.now().toString());
        }
        return Map.of(
                "code",      code,
                "message",   message,
                "timestamp", LocalDateTime.now().toString());
    }
}


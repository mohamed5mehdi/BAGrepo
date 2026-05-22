package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DemandeAjustementService {

    @Autowired private DemandeAjustementRepository demandeAjustementRepository;
    @Autowired private DaHeaderRepository daHeaderRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private SubFamilyRepository subFamilyRepository;
    @Autowired private BudgetTransferRepository budgetTransferRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ActionRepository actionRepository;
    @Autowired private StatusHistoryRepository historyRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private BudgetSuiviService budgetSuiviService;

    private void saveHistory(DaHeader da, String avant, String apres, User utilisateur, String commentaire) {
        StatusHistory history = new StatusHistory("DaHeader", Long.valueOf(da.getOidDa()), avant, apres, utilisateur, commentaire);
        historyRepository.save(history);
        AuditLog log = new AuditLog("AJUSTEMENT_BUDGET", "DaHeader", Long.valueOf(da.getOidDa()), utilisateur, avant, apres);
        auditLogRepository.save(log);
    }

    private void checkActiveAjustement(Integer daId) {
        if (demandeAjustementRepository.existsByDaIdAndStatutIn(daId, List.of(
                StatutAjustement.EN_ATTENTE_DG,
                StatutAjustement.EN_ATTENTE_DAF,
                StatutAjustement.EN_ATTENTE_ACHETEUR))) {
            throw new RuntimeException("Ajustement déjà en cours pour cette DA");
        }
    }

    /**
     * Soumission d'un ajustement Famille (injection de budget par le DG).
     *
     * BUG #1 CORRIGÉ : La soumission n'effectue AUCUNE mutation budgétaire.
     * L'invariant initial = engage + restant ne peut être touché qu'à la décision finale.
     * Toute mutation prématurée créait de la monnaie fictive.
     */
    public DemandeAjustement soumettreAjustementFamille(@NonNull Integer daId, @NonNull Integer familleCibleId,
                                                         @NonNull BigDecimal montantDemande, String justification,
                                                         @NonNull Long acheteurId) {
        checkActiveAjustement(daId);
        DaHeader da = daHeaderRepository.findById(daId)
                .orElseThrow(() -> new RuntimeException("DA introuvable"));
        Family famille = familyRepository.findById(familleCibleId)
                .orElseThrow(() -> new RuntimeException("Famille introuvable"));

        // BUG #5 CORRIGÉ : null-safe sur budgetRestant
        BigDecimal restantActuel = famille.getBudgetRestant() != null
                ? famille.getBudgetRestant() : BigDecimal.ZERO;

        DemandeAjustement daAjust = new DemandeAjustement();
        daAjust.setDa(da);
        daAjust.setType(TypeAjustement.FAMILLE);
        daAjust.setStatut(StatutAjustement.EN_ATTENTE_DG);
        daAjust.setFamilleCibleId(familleCibleId);
        daAjust.setMontantDemande(montantDemande);
        daAjust.setJustificationAcheteur(justification);
        daAjust.setAcheteurId(acheteurId);
        daAjust.setBudgetAvantDemande(restantActuel.toPlainString());
        daAjust.setBudgetApresDemande(restantActuel.add(montantDemande).toPlainString());
        daAjust.setStatutDaAvantAjustement(da.getStatut());

        // BUG #1 CORRIGÉ : aucune mutation budgétaire ici — famille et SF intouchées jusqu'à décision DG.

        DemandeAjustement saved = demandeAjustementRepository.save(daAjust);
        saveHistory(da, da.getStatut().name(), da.getStatut().name(),
                userRepository.findById(acheteurId.intValue()).orElse(null),
                "Soumission ajustement famille: " + justification);

        return saved;
    }

    /**
     * Décision DG sur un ajustement Famille.
     *
     * BUG #2 CORRIGÉ : La famille est rechargée avec lock. Mutation directe budgetInitial et
     * budgetRestant de la famille (injection externe DG). recalculerFamille() non appelé ici
     * car le budgetInitial famille change (abondement d'exercice), pas les SF enfants.
     *
     * BUG #3 CORRIGÉ : BudgetTransfer créé avec subSource=null (documenté : injection DG),
     * subCible=null (injection sur la famille globale, pas une SF spécifique).
     */
    public DemandeAjustement deciderDg(@NonNull Long id, @NonNull Long dgId,
                                        @NonNull String decision, String justification) {
        DemandeAjustement daAjust = demandeAjustementRepository.findById(id).orElseThrow();
        if (daAjust.getStatut() != StatutAjustement.EN_ATTENTE_DG) {
            throw new RuntimeException("Statut invalide pour décision DG");
        }

        Family famille = familyRepository.findByIdWithLock(daAjust.getFamilleCibleId()).orElseThrow();
        User dg = userRepository.findById(dgId.intValue()).orElseThrow();

        if ("VALIDE".equals(decision)) {
            BigDecimal montant = daAjust.getMontantDemande();

            // BUG #2 CORRIGÉ : injection budget sur la famille avec lock. budgetInitial mis à jour
            // pour refléter l'abondement DG (exercice étendu). L'équation famille est maintenue :
            // nouveau_initial = ancien_initial + montant = engage + nouveau_restant.
            famille.setBudgetInitial(orZero(famille.getBudgetInitial()).add(montant));
            famille.setBudgetRestant(orZero(famille.getBudgetRestant()).add(montant));
            familyRepository.save(famille);

            if (daAjust.getDa() != null) {
                DaHeader da = daAjust.getDa();
                da.setStatut(daAjust.getStatutDaAvantAjustement());
                daHeaderRepository.save(da);

                BudgetTransfer transfer = new BudgetTransfer(da, null, null, montant, dg);
                budgetTransferRepository.save(transfer);
                
                saveHistory(da, daAjust.getStatutDaAvantAjustement().name(), da.getStatut().name(), dg,
                        "Décision DG VALIDE — injection budget famille " + montant + " DZD");
            } else if (daAjust.getDemandeInterne() != null) {
                DemandeAchatInterne di = daAjust.getDemandeInterne();
                di.setStatut(daAjust.getStatutDemandeInterneAvantAjustement());
            }

            daAjust.setStatut(StatutAjustement.EN_ATTENTE_ACHETEUR);

        } else if ("REJETE".equals(decision)) {
            daAjust.setStatut(StatutAjustement.REJETE);
            if (daAjust.getDa() != null) {
                saveHistory(daAjust.getDa(), daAjust.getStatutDaAvantAjustement().name(),
                        daAjust.getDa().getStatut().name(), dg,
                        "Décision DG REJETE — ajustement famille refusé");
            } else if (daAjust.getDemandeInterne() != null) {
                daAjust.getDemandeInterne().setStatut(StatutDemande.REJETEE);
            }
        } else {
            throw new RuntimeException("Décision invalide : attendu VALIDE ou REJETE");
        }

        daAjust.setValideurId(dgId);
        daAjust.setJustificationValideur(justification);
        daAjust.setDateDecision(LocalDateTime.now());

        return demandeAjustementRepository.save(daAjust);
    }

    /**
     * Soumission d'un ajustement Sous-Famille (transfert inter-SF via le DAF).
     * La source est pré-réservée (budget_engage++) pour bloquer une consommation concurrente.
     */
    public DemandeAjustement soumettreAjustementSousFamille(@NonNull Integer daId,
                                                              @NonNull Integer sourceSousFamilleId,
                                                              @NonNull Integer cibleSousFamilleId,
                                                              @NonNull BigDecimal montantDemande,
                                                              String justification,
                                                              @NonNull Long acheteurId) {
        checkActiveAjustement(daId);
        DaHeader da = daHeaderRepository.findById(daId)
                .orElseThrow(() -> new RuntimeException("DA introuvable"));

        // Anti-deadlock : lock dans l'ordre croissant des IDs
        SubFamily source, cible;
        if (sourceSousFamilleId < cibleSousFamilleId) {
            source = subFamilyRepository.findByIdWithLock(sourceSousFamilleId).orElseThrow();
            cible  = subFamilyRepository.findByIdWithLock(cibleSousFamilleId).orElseThrow();
        } else {
            cible  = subFamilyRepository.findByIdWithLock(cibleSousFamilleId).orElseThrow();
            source = subFamilyRepository.findByIdWithLock(sourceSousFamilleId).orElseThrow();
        }

        if (!source.hasEnoughBudget(montantDemande)) {
            throw new RuntimeException("Budget insuffisant dans la sous-famille source pour cet ajustement");
        }

        DemandeAjustement daAjust = new DemandeAjustement();
        daAjust.setDa(da);
        daAjust.setType(TypeAjustement.SOUS_FAMILLE);
        daAjust.setStatut(StatutAjustement.EN_ATTENTE_DAF);
        daAjust.setSourceSousFamilleId(sourceSousFamilleId);
        daAjust.setCibleSousFamilleId(cibleSousFamilleId);
        daAjust.setMontantDemande(montantDemande);
        daAjust.setJustificationAcheteur(justification);
        daAjust.setAcheteurId(acheteurId);
        daAjust.setBudgetAvantDemande("Source: " + source.getBudgetRestant() + ", Cible: " + cible.getBudgetRestant());
        daAjust.setBudgetApresDemande("Source: " + source.getBudgetRestant().subtract(montantDemande)
                + ", Cible: " + cible.getBudgetRestant().add(montantDemande));
        daAjust.setStatutDaAvantAjustement(da.getStatut());

        // Pré-réservation : budget_engage++ sur la source pour bloquer consommations concurrentes
        source.setBudgetEngage(orZero(source.getBudgetEngage()).add(montantDemande));
        subFamilyRepository.save(source);

        DemandeAjustement saved = demandeAjustementRepository.save(daAjust);
        saveHistory(da, da.getStatut().name(), da.getStatut().name(),
                userRepository.findById(acheteurId.intValue()).orElse(null),
                "Soumission ajustement sous-famille: " + justification);

        return saved;
    }

    /**
     * Décision DAF sur un ajustement Sous-Famille.
     *
     * BUG #2 CORRIGÉ : recalculerFamille() appelé après mutation des SF.
     */
    public DemandeAjustement deciderDaf(@NonNull Long id, @NonNull Long dafId,
                                         @NonNull String decision, BigDecimal montantFinal,
                                         String justification) {
        DemandeAjustement daAjust = demandeAjustementRepository.findById(id).orElseThrow();
        if (daAjust.getStatut() != StatutAjustement.EN_ATTENTE_DAF) {
            throw new RuntimeException("Statut invalide pour décision DAF");
        }

        // Anti-deadlock : lock dans l'ordre croissant des IDs
        Integer sId = daAjust.getSourceSousFamilleId();
        Integer cId = daAjust.getCibleSousFamilleId();
        SubFamily source, cible;
        if (sId < cId) {
            source = subFamilyRepository.findByIdWithLock(sId).orElseThrow();
            cible  = subFamilyRepository.findByIdWithLock(cId).orElseThrow();
        } else {
            cible  = subFamilyRepository.findByIdWithLock(cId).orElseThrow();
            source = subFamilyRepository.findByIdWithLock(sId).orElseThrow();
        }

        User daf = userRepository.findById(dafId.intValue()).orElseThrow();
        BigDecimal montantF = montantFinal != null ? montantFinal : daAjust.getMontantDemande();

        if ("VALIDE".equals(decision)) {
            daAjust.setMontantFinal(montantF);

            // Transfert réel : réallocation budgetInitial + budgetRestant entre SF.
            source.setBudgetRestant(orZero(source.getBudgetRestant()).subtract(montantF));
            source.setBudgetInitial(orZero(source.getBudgetInitial()).subtract(montantF));
            // Annulation de la pré-réservation (engage--) exactement à hauteur du montant initial demandé
            source.setBudgetEngage(orZero(source.getBudgetEngage()).subtract(daAjust.getMontantDemande()));

            cible.setBudgetRestant(orZero(cible.getBudgetRestant()).add(montantF));
            cible.setBudgetInitial(orZero(cible.getBudgetInitial()).add(montantF));

            subFamilyRepository.save(source);
            subFamilyRepository.save(cible);

            // Gestion des enveloppes Familles si les SF appartiennent à des familles différentes ou même famille
            Family famSource = source.getFamily();
            Family famCible = cible.getFamily();

            if (famSource != null && famCible != null && !famSource.getIdFamily().equals(famCible.getIdFamily())) {
                // Transfert inter-familles : on ajuste les enveloppes initiales des familles
                Family famS = familyRepository.findByIdWithLock(famSource.getIdFamily()).orElseThrow();
                Family famC = familyRepository.findByIdWithLock(famCible.getIdFamily()).orElseThrow();
                
                famS.setBudgetInitial(orZero(famS.getBudgetInitial()).subtract(montantF));
                famC.setBudgetInitial(orZero(famC.getBudgetInitial()).add(montantF));
                
                familyRepository.save(famS);
                familyRepository.save(famC);
                
                budgetSuiviService.recalculerFamille(famS);
                budgetSuiviService.recalculerFamille(famC);
            } else if (famSource != null) {
                // Même famille (ou cible sans famille)
                budgetSuiviService.recalculerFamille(famSource);
            }

            BudgetTransfer transfer = new BudgetTransfer(daAjust.getDa(), source, cible, montantF, daf);
            budgetTransferRepository.save(transfer);

            if (daAjust.getDemandeInterne() != null) {
                DemandeAchatInterne di = daAjust.getDemandeInterne();
                di.setStatut(daAjust.getStatutDemandeInterneAvantAjustement());
            }

            daAjust.setStatut(StatutAjustement.EN_ATTENTE_ACHETEUR);

        } else if ("REJETE".equals(decision)) {
            // Annulation de la pré-réservation sur la source
            source.setBudgetEngage(orZero(source.getBudgetEngage()).subtract(daAjust.getMontantDemande()));
            subFamilyRepository.save(source);
            daAjust.setStatut(StatutAjustement.REJETE);
            if (daAjust.getDemandeInterne() != null) {
                daAjust.getDemandeInterne().setStatut(StatutDemande.REJETEE);
            }
        } else {
            throw new RuntimeException("Décision invalide : attendu VALIDE ou REJETE");
        }

        daAjust.setValideurId(dafId);
        daAjust.setJustificationValideur(justification);
        daAjust.setDateDecision(LocalDateTime.now());

        DemandeAjustement saved = demandeAjustementRepository.save(daAjust);
        if (daAjust.getDa() != null) {
            saveHistory(daAjust.getDa(), daAjust.getStatutDaAvantAjustement().name(),
                    daAjust.getDa().getStatut().name(), daf,
                    "Décision DAF sur ajustement: " + decision + " (" + justification + ")");
        }

        return saved;
    }

    public DemandeAjustement confirmerAcheteur(@NonNull Long id, @NonNull Long acheteurId) {
        DemandeAjustement daAjust = demandeAjustementRepository.findById(id).orElseThrow();
        if (daAjust.getStatut() != StatutAjustement.EN_ATTENTE_ACHETEUR) {
            throw new RuntimeException("Statut invalide pour confirmation acheteur");
        }
        daAjust.setStatut(StatutAjustement.EN_ATTENTE_AMG);

        if (daAjust.getDa() != null) {
            DaHeader da = daAjust.getDa();
            da.setStatut(StatutDA.EN_ATTENTE_AMG);
            daHeaderRepository.save(da);
        } else if (daAjust.getDemandeInterne() != null) {
            DemandeAchatInterne di = daAjust.getDemandeInterne();
            di.setStatut(StatutDemande.EN_TRAITEMENT);
        }

        return demandeAjustementRepository.save(daAjust);
    }

    public DemandeAjustement finaliserAmg(@NonNull Long id) {
        DemandeAjustement daAjust = demandeAjustementRepository.findById(id).orElseThrow();
        if (daAjust.getStatut() != StatutAjustement.EN_ATTENTE_AMG) {
            throw new RuntimeException("Statut invalide pour finalisation AMG");
        }
        daAjust.setStatut(StatutAjustement.VALIDE);
        return demandeAjustementRepository.save(daAjust);
    }

    /**
     * Ajustement direct inter-SF (legacy orchestrator).
     *
     * BUG #6 CORRIGÉ : familyRepository.save() explicite via recalculerFamille().
     * L'ancien code mutait le proxy Lazy sans jamais persister les changements.
     */
    public DaHeader legacyAjusterBudgetSousFamille(Integer daId, Integer dafId,
                                                    Integer subSourceId, Integer subCibleId,
                                                    BigDecimal montant) {
        DaHeader da = daHeaderRepository.findById(daId).orElseThrow();
        User daf = userRepository.findById(dafId).orElseThrow();

        SubFamily source, cible;
        if (subSourceId < subCibleId) {
            source = subFamilyRepository.findByIdWithLock(subSourceId).orElseThrow();
            cible  = subFamilyRepository.findByIdWithLock(subCibleId).orElseThrow();
        } else {
            cible  = subFamilyRepository.findByIdWithLock(subCibleId).orElseThrow();
            source = subFamilyRepository.findByIdWithLock(subSourceId).orElseThrow();
        }

        if (!source.hasEnoughBudget(montant)) throw new RuntimeException("Budget insuffisant");

        source.deductBudget(montant);
        source.setBudgetInitial(orZero(source.getBudgetInitial()).subtract(montant));
        cible.setBudgetRestant(orZero(cible.getBudgetRestant()).add(montant));
        cible.setBudgetInitial(orZero(cible.getBudgetInitial()).add(montant));

        subFamilyRepository.save(source);
        subFamilyRepository.save(cible);

        Family famSource = source.getFamily();
        Family famCible = cible.getFamily();

        if (famSource != null && famCible != null && !famSource.getIdFamily().equals(famCible.getIdFamily())) {
            Family famS = familyRepository.findByIdWithLock(famSource.getIdFamily()).orElseThrow();
            Family famC = familyRepository.findByIdWithLock(famCible.getIdFamily()).orElseThrow();
            
            famS.setBudgetInitial(orZero(famS.getBudgetInitial()).subtract(montant));
            famC.setBudgetInitial(orZero(famC.getBudgetInitial()).add(montant));
            
            familyRepository.save(famS);
            familyRepository.save(famC);
            
            budgetSuiviService.recalculerFamille(famS);
            budgetSuiviService.recalculerFamille(famC);
        } else if (famSource != null) {
            budgetSuiviService.recalculerFamille(famSource);
        }

        BudgetTransfer transfer = new BudgetTransfer(da, source, cible, montant, daf);
        budgetTransferRepository.save(transfer);

        Action action = new Action(daf, da, TypeAction.AJUST_BUDGET_SF);
        actionRepository.save(action);

        da.setStatut(StatutDA.EN_ATTENTE_AMG);
        return daHeaderRepository.save(da);
    }

    /**
     * Injection de budget famille par le DG (legacy orchestrator).
     *
     * BUG #6 CORRIGÉ : rechargement avec lock explicite + budgetInitial famille mis à jour.
     */
    public DaHeader legacyAjusterBudgetFamille(Integer daId, Integer dgId,
                                                Integer subCibleId, BigDecimal montant) {
        DaHeader da = daHeaderRepository.findById(daId).orElseThrow();
        User dg = userRepository.findById(dgId).orElseThrow();

        SubFamily cible = subFamilyRepository.findByIdWithLock(subCibleId).orElseThrow();
        cible.setBudgetRestant(orZero(cible.getBudgetRestant()).add(montant));
        cible.setBudgetInitial(orZero(cible.getBudgetInitial()).add(montant));
        subFamilyRepository.save(cible);

        // BUG #6 CORRIGÉ : rechargement famille avec lock + persistance explicite
        if (cible.getFamily() != null) {
            Family famille = familyRepository.findByIdWithLock(cible.getFamily().getIdFamily()).orElseThrow();
            famille.setBudgetInitial(orZero(famille.getBudgetInitial()).add(montant));
            familyRepository.save(famille);
            budgetSuiviService.recalculerFamille(famille);
        }

        BudgetTransfer transfer = new BudgetTransfer(da, null, cible, montant, dg);
        budgetTransferRepository.save(transfer);

        Action action = new Action(dg, da, TypeAction.VALID_BUDGET_FAMILLE);
        actionRepository.save(action);

        da.setStatut(StatutDA.VALIDEE);
        return daHeaderRepository.save(da);
    }

    private static BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}

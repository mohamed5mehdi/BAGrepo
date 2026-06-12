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
    @Autowired private DemandeAchatInterneRepository demandeAchatInterneRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private SubFamilyRepository subFamilyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StatusHistoryRepository historyRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private BudgetSuiviService budgetSuiviService;

    private void saveHistory(DemandeAchatInterne di, String avant, String apres, User utilisateur, String commentaire) {
        StatusHistory history = new StatusHistory("DemandeAchatInterne", Long.valueOf(di.getId()), avant, apres, utilisateur, commentaire);
        historyRepository.save(history);
        AuditLog log = new AuditLog("AJUSTEMENT_BUDGET", "DemandeAchatInterne", Long.valueOf(di.getId()), utilisateur, avant, apres);
        auditLogRepository.save(log);
    }

    private void checkActiveAjustement(Long diId) {
        if (demandeAjustementRepository.existsByDemandeInterneIdAndStatutIn(diId, List.of(
                StatutAjustement.EN_ATTENTE_DG,
                StatutAjustement.EN_ATTENTE_DAF,
                StatutAjustement.EN_ATTENTE_ACHETEUR))) {
            throw new RuntimeException("Ajustement déjà en cours pour cette demande interne");
        }
    }

    public DemandeAjustement soumettreAjustementFamille(@NonNull Long diId, @NonNull Integer familleCibleId,
                                                         @NonNull BigDecimal montantDemande, String justification,
                                                         @NonNull Long acheteurId) {
        checkActiveAjustement(diId);
        DemandeAchatInterne di = demandeAchatInterneRepository.findById(diId)
                .orElseThrow(() -> new RuntimeException("Demande interne introuvable"));
        Family famille = familyRepository.findById(familleCibleId)
                .orElseThrow(() -> new RuntimeException("Famille introuvable"));

        BigDecimal restantActuel = famille.getBudgetRestant() != null
                ? famille.getBudgetRestant() : BigDecimal.ZERO;

        DemandeAjustement daAjust = new DemandeAjustement();
        daAjust.setDemandeInterne(di);
        daAjust.setType(TypeAjustement.FAMILLE);
        daAjust.setStatut(StatutAjustement.EN_ATTENTE_DG);
        daAjust.setFamilleCibleId(familleCibleId);
        daAjust.setMontantDemande(montantDemande);
        daAjust.setJustificationAcheteur(justification);
        daAjust.setAcheteurId(acheteurId);
        daAjust.setBudgetAvantDemande(restantActuel.toPlainString());
        daAjust.setBudgetApresDemande(restantActuel.add(montantDemande).toPlainString());
        daAjust.setStatutDemandeInterneAvantAjustement(di.getStatut());

        DemandeAjustement saved = demandeAjustementRepository.save(daAjust);
        saveHistory(di, di.getStatut().name(), di.getStatut().name(),
                userRepository.findById(acheteurId.intValue()).orElse(null),
                "Soumission ajustement famille: " + justification);

        return saved;
    }

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
            famille.setBudgetInitial(orZero(famille.getBudgetInitial()).add(montant));
            famille.setBudgetRestant(orZero(famille.getBudgetRestant()).add(montant));
            familyRepository.save(famille);

            if (daAjust.getDemandeInterne() != null) {
                DemandeAchatInterne di = daAjust.getDemandeInterne();
                di.setStatut(daAjust.getStatutDemandeInterneAvantAjustement());
                demandeAchatInterneRepository.save(di);
                saveHistory(di, daAjust.getStatutDemandeInterneAvantAjustement().name(), di.getStatut().name(), dg,
                        "Décision DG VALIDE — injection budget famille " + montant + " DZD");
            }
            daAjust.setStatut(StatutAjustement.EN_ATTENTE_ACHETEUR);

        } else if ("REJETE".equals(decision)) {
            daAjust.setStatut(StatutAjustement.REJETE);
            if (daAjust.getDemandeInterne() != null) {
                DemandeAchatInterne di = daAjust.getDemandeInterne();
                di.setStatut(StatutDemande.BROUILLON);
                demandeAchatInterneRepository.save(di);
                saveHistory(di, daAjust.getStatutDemandeInterneAvantAjustement().name(), di.getStatut().name(), dg,
                        "Décision DG REJETE — ajustement famille refusé");
            }
        } else {
            throw new RuntimeException("Décision invalide : attendu VALIDE ou REJETE");
        }

        daAjust.setValideurId(dgId);
        daAjust.setJustificationValideur(justification);
        daAjust.setDateDecision(LocalDateTime.now());

        return demandeAjustementRepository.save(daAjust);
    }

    public DemandeAjustement soumettreAjustementSousFamille(@NonNull Long diId,
                                                              @NonNull Integer sourceSousFamilleId,
                                                              @NonNull Integer cibleSousFamilleId,
                                                              @NonNull BigDecimal montantDemande,
                                                              String justification,
                                                              @NonNull Long acheteurId) {
        checkActiveAjustement(diId);
        DemandeAchatInterne di = demandeAchatInterneRepository.findById(diId)
                .orElseThrow(() -> new RuntimeException("Demande interne introuvable"));

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
        daAjust.setDemandeInterne(di);
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
        daAjust.setStatutDemandeInterneAvantAjustement(di.getStatut());

        source.deductBudget(montantDemande);
        subFamilyRepository.save(source);
        if (source.getFamily() != null) {
            Family famSource = familyRepository.findByIdWithLock(source.getFamily().getIdFamily()).orElseThrow();
            famSource.deductBudget(montantDemande);
            familyRepository.save(famSource);
        }

        DemandeAjustement saved = demandeAjustementRepository.save(daAjust);
        saveHistory(di, di.getStatut().name(), di.getStatut().name(),
                userRepository.findById(acheteurId.intValue()).orElse(null),
                "Soumission ajustement sous-famille: " + justification);

        return saved;
    }

    public DemandeAjustement deciderDaf(@NonNull Long id, @NonNull Long dafId,
                                         @NonNull String decision, BigDecimal montantFinal,
                                         String justification) {
        DemandeAjustement daAjust = demandeAjustementRepository.findById(id).orElseThrow();
        if (daAjust.getStatut() != StatutAjustement.EN_ATTENTE_DAF) {
            throw new RuntimeException("Statut invalide pour décision DAF");
        }

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

            source.addBudget(daAjust.getMontantDemande());
            source.setBudgetRestant(orZero(source.getBudgetRestant()).subtract(montantF));
            source.setBudgetInitial(orZero(source.getBudgetInitial()).subtract(montantF));

            cible.setBudgetRestant(orZero(cible.getBudgetRestant()).add(montantF));
            cible.setBudgetInitial(orZero(cible.getBudgetInitial()).add(montantF));

            subFamilyRepository.save(source);
            subFamilyRepository.save(cible);

            Family famSource = source.getFamily();
            Family famCible = cible.getFamily();

            if (famSource != null && famCible != null && !famSource.getIdFamily().equals(famCible.getIdFamily())) {
                Family famS = familyRepository.findByIdWithLock(famSource.getIdFamily()).orElseThrow();
                Family famC = familyRepository.findByIdWithLock(famCible.getIdFamily()).orElseThrow();
                
                famS.setBudgetInitial(orZero(famS.getBudgetInitial()).subtract(montantF));
                famC.setBudgetInitial(orZero(famC.getBudgetInitial()).add(montantF));
                
                familyRepository.save(famS);
                familyRepository.save(famC);
                
                budgetSuiviService.recalculerFamille(famS);
                budgetSuiviService.recalculerFamille(famC);
            } else if (famSource != null) {
                budgetSuiviService.recalculerFamille(famSource);
            }

            if (daAjust.getDemandeInterne() != null) {
                DemandeAchatInterne di = daAjust.getDemandeInterne();
                di.setStatut(daAjust.getStatutDemandeInterneAvantAjustement());
                demandeAchatInterneRepository.save(di);
            }

            daAjust.setStatut(StatutAjustement.EN_ATTENTE_ACHETEUR);

        } else if ("REJETE".equals(decision)) {
            source.addBudget(daAjust.getMontantDemande());
            subFamilyRepository.save(source);
            if (source.getFamily() != null) {
                Family famSource = familyRepository.findByIdWithLock(source.getFamily().getIdFamily()).orElseThrow();
                famSource.addBudget(daAjust.getMontantDemande());
                familyRepository.save(famSource);
            }
            daAjust.setStatut(StatutAjustement.REJETE);
            if (daAjust.getDemandeInterne() != null) {
                DemandeAchatInterne di = daAjust.getDemandeInterne();
                di.setStatut(StatutDemande.BROUILLON);
                demandeAchatInterneRepository.save(di);
            }
        } else {
            throw new RuntimeException("Décision invalide : attendu VALIDE ou REJETE");
        }

        daAjust.setValideurId(dafId);
        daAjust.setJustificationValideur(justification);
        daAjust.setDateDecision(LocalDateTime.now());

        DemandeAjustement saved = demandeAjustementRepository.save(daAjust);
        if (daAjust.getDemandeInterne() != null) {
            saveHistory(daAjust.getDemandeInterne(), daAjust.getStatutDemandeInterneAvantAjustement().name(),
                    daAjust.getDemandeInterne().getStatut().name(), daf,
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

        if (daAjust.getDemandeInterne() != null) {
            DemandeAchatInterne di = daAjust.getDemandeInterne();
            di.setStatut(StatutDemande.EN_TRAITEMENT);
            demandeAchatInterneRepository.save(di);
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

    private static BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}

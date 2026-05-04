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

    private void saveHistory(DaHeader da, String avant, String apres, User utilisateur, String commentaire) {
        StatusHistory history = new StatusHistory("DaHeader", Long.valueOf(da.getOidDa()), avant, apres, utilisateur, commentaire);
        historyRepository.save(history);
        
        AuditLog log = new AuditLog("AJUSTEMENT_BUDGET", "DaHeader", Long.valueOf(da.getOidDa()), utilisateur, avant, apres);
        auditLogRepository.save(log);
    }

    private void checkActiveAjustement(Integer daId) {
        if (demandeAjustementRepository.existsByDaIdAndStatutIn(daId, List.of(StatutAjustement.EN_ATTENTE_DG, StatutAjustement.EN_ATTENTE_DAF, StatutAjustement.EN_ATTENTE_ACHETEUR))) {
            throw new RuntimeException("Ajustement déjà en cours pour cette DA");
        }
    }

    public DemandeAjustement soumettreAjustementFamille(@NonNull Integer daId, @NonNull Integer familleCibleId, @NonNull BigDecimal montantDemande, String justification, @NonNull Long acheteurId) {
        checkActiveAjustement(daId);
        DaHeader da = daHeaderRepository.findById(daId).orElseThrow(() -> new RuntimeException("DA introuvable"));
        Family famille = familyRepository.findById(familleCibleId).orElseThrow(() -> new RuntimeException("Famille introuvable"));

        DemandeAjustement daAjust = new DemandeAjustement();
        daAjust.setDa(da);
        daAjust.setType(TypeAjustement.FAMILLE);
        daAjust.setStatut(StatutAjustement.EN_ATTENTE_DG);
        daAjust.setFamilleCibleId(familleCibleId);
        daAjust.setMontantDemande(montantDemande);
        daAjust.setJustificationAcheteur(justification);
        daAjust.setAcheteurId(acheteurId);

        daAjust.setBudgetAvantDemande(famille.getBudgetRestant().toString());
        daAjust.setBudgetApresDemande(famille.getBudgetRestant().add(montantDemande).toString());
        
        famille.setBudgetEngage(famille.getBudgetEngage() != null ? famille.getBudgetEngage().add(montantDemande) : montantDemande);
        familyRepository.save(famille);

        daAjust.setStatutDaAvantAjustement(da.getStatut());
        DemandeAjustement saved = demandeAjustementRepository.save(daAjust);
        
        saveHistory(da, da.getStatut().name(), da.getStatut().name(), userRepository.findById(acheteurId.intValue()).orElse(null), "Soumission ajustement famille: " + justification);
        
        return saved;
    }

    public DemandeAjustement deciderDg(@NonNull Long id, @NonNull Long dgId, @NonNull String decision, String justification) {
        DemandeAjustement daAjust = demandeAjustementRepository.findById(id).orElseThrow();
        if (daAjust.getStatut() != StatutAjustement.EN_ATTENTE_DG) {
            throw new RuntimeException("Statut invalide pour décision DG");
        }
        Family famille = familyRepository.findById(daAjust.getFamilleCibleId()).orElseThrow();
        User dg = userRepository.findById(dgId.intValue()).orElseThrow();

        if ("VALIDE".equals(decision)) {
            famille.setBudgetRestant(famille.getBudgetRestant().add(daAjust.getMontantDemande()));
            famille.setBudgetEngage(famille.getBudgetEngage().subtract(daAjust.getMontantDemande()));
            daAjust.setStatut(StatutAjustement.EN_ATTENTE_ACHETEUR);
            
            DaHeader da = daAjust.getDa();
            da.setStatut(daAjust.getStatutDaAvantAjustement());
            daHeaderRepository.save(da);

            BudgetTransfer transfer = new BudgetTransfer();
            transfer.setDaHeader(da);
            transfer.setMontant(daAjust.getMontantDemande());
            transfer.setDaf(dg); // Using dg as the user performing the transfer
            budgetTransferRepository.save(transfer);
        } else if ("REJETE".equals(decision)) {
            famille.setBudgetEngage(famille.getBudgetEngage().subtract(daAjust.getMontantDemande()));
            daAjust.setStatut(StatutAjustement.REJETE);
        } else {
            throw new RuntimeException("Décision invalide");
        }
        
        daAjust.setValideurId(dgId);
        daAjust.setJustificationValideur(justification);
        daAjust.setDateDecision(LocalDateTime.now());
        
        familyRepository.save(famille);
        DemandeAjustement saved = demandeAjustementRepository.save(daAjust);
        
        saveHistory(daAjust.getDa(), daAjust.getStatutDaAvantAjustement().name(), daAjust.getDa().getStatut().name(), dg, "Décision DG sur ajustement: " + decision + " (" + justification + ")");
        
        return saved;
    }

    public DemandeAjustement soumettreAjustementSousFamille(@NonNull Integer daId, @NonNull Integer sourceSousFamilleId, @NonNull Integer cibleSousFamilleId, @NonNull BigDecimal montantDemande, String justification, @NonNull Long acheteurId) {
        checkActiveAjustement(daId);
        DaHeader da = daHeaderRepository.findById(daId).orElseThrow(() -> new RuntimeException("DA introuvable"));
        
        // Lock both source and cible to prevent race conditions during submission
        SubFamily source = subFamilyRepository.findByIdWithLock(sourceSousFamilleId).orElseThrow();
        SubFamily cible = subFamilyRepository.findByIdWithLock(cibleSousFamilleId).orElseThrow();

        if (!source.hasEnoughBudget(montantDemande)) {
            throw new RuntimeException("Budget insuffisant pour ajustement");
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
        daAjust.setBudgetApresDemande("Source: " + source.getBudgetRestant().subtract(montantDemande) + ", Cible: " + cible.getBudgetRestant().add(montantDemande));
        
        source.setBudgetEngage(source.getBudgetEngage() != null ? source.getBudgetEngage().add(montantDemande) : montantDemande);
        subFamilyRepository.save(source);

        daAjust.setStatutDaAvantAjustement(da.getStatut());
        DemandeAjustement saved = demandeAjustementRepository.save(daAjust);

        saveHistory(da, da.getStatut().name(), da.getStatut().name(), userRepository.findById(acheteurId.intValue()).orElse(null), "Soumission ajustement sous-famille: " + justification);

        return saved;
    }

    public DemandeAjustement deciderDaf(@NonNull Long id, @NonNull Long dafId, @NonNull String decision, BigDecimal montantFinal, String justification) {
        DemandeAjustement daAjust = demandeAjustementRepository.findById(id).orElseThrow();
        if (daAjust.getStatut() != StatutAjustement.EN_ATTENTE_DAF) {
            throw new RuntimeException("Statut invalide pour décision DAF");
        }
        
        // Anti-deadlock: Lock IDs in order
        Integer sId = daAjust.getSourceSousFamilleId();
        Integer cId = daAjust.getCibleSousFamilleId();
        SubFamily source, cible;
        if (sId < cId) {
            source = subFamilyRepository.findByIdWithLock(sId).orElseThrow();
            cible = subFamilyRepository.findByIdWithLock(cId).orElseThrow();
        } else {
            cible = subFamilyRepository.findByIdWithLock(cId).orElseThrow();
            source = subFamilyRepository.findByIdWithLock(sId).orElseThrow();
        }

        User daf = userRepository.findById(dafId.intValue()).orElseThrow();

        BigDecimal montantF = montantFinal != null ? montantFinal : daAjust.getMontantDemande();

        if ("VALIDE".equals(decision)) {
            daAjust.setMontantFinal(montantF);
            source.setBudgetRestant(source.getBudgetRestant().subtract(montantF));
            source.setBudgetEngage(source.getBudgetEngage().subtract(daAjust.getMontantDemande()));
            cible.setBudgetRestant(cible.getBudgetRestant().add(montantF));
            daAjust.setStatut(StatutAjustement.EN_ATTENTE_ACHETEUR);
            
            BudgetTransfer transfer = new BudgetTransfer(daAjust.getDa(), source, cible, montantF, daf);
            budgetTransferRepository.save(transfer);
        } else if ("REJETE".equals(decision)) {
            source.setBudgetEngage(source.getBudgetEngage().subtract(daAjust.getMontantDemande()));
            daAjust.setStatut(StatutAjustement.REJETE);
        } else {
            throw new RuntimeException("Décision invalide");
        }
        
        daAjust.setValideurId(dafId);
        daAjust.setJustificationValideur(justification);
        daAjust.setDateDecision(LocalDateTime.now());
        
        subFamilyRepository.save(source);
        subFamilyRepository.save(cible);
        DemandeAjustement saved = demandeAjustementRepository.save(daAjust);

        saveHistory(daAjust.getDa(), daAjust.getStatutDaAvantAjustement().name(), daAjust.getDa().getStatut().name(), daf, "Décision DAF sur ajustement: " + decision + " (" + justification + ")");

        return saved;
    }

    public DemandeAjustement confirmerAcheteur(@NonNull Long id, @NonNull Long acheteurId) {
        DemandeAjustement daAjust = demandeAjustementRepository.findById(id).orElseThrow();
        if (daAjust.getStatut() != StatutAjustement.EN_ATTENTE_ACHETEUR) {
            throw new RuntimeException("Statut invalide pour confirmation acheteur");
        }
        daAjust.setStatut(StatutAjustement.EN_ATTENTE_AMG);
        
        DaHeader da = daAjust.getDa();
        da.setStatut(StatutDA.EN_ATTENTE_AMG);
        daHeaderRepository.save(da);
        
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

    // Direct methods for legacy orchestrator
    public DaHeader legacyAjusterBudgetSousFamille(Integer daId, Integer dafId, Integer subSourceId, Integer subCibleId, BigDecimal montant) {
        DaHeader da = daHeaderRepository.findById(daId).orElseThrow();
        User daf = userRepository.findById(dafId).orElseThrow();
        
        // Anti-deadlock locking
        SubFamily source, cible;
        if (subSourceId < subCibleId) {
            source = subFamilyRepository.findByIdWithLock(subSourceId).orElseThrow();
            cible = subFamilyRepository.findByIdWithLock(subCibleId).orElseThrow();
        } else {
            cible = subFamilyRepository.findByIdWithLock(subCibleId).orElseThrow();
            source = subFamilyRepository.findByIdWithLock(subSourceId).orElseThrow();
        }

        if (!source.hasEnoughBudget(montant)) throw new RuntimeException("Budget insuffisant");

        source.deductBudget(montant);
        if (source.getFamily() != null) source.getFamily().deductBudget(montant);
        cible.setBudgetRestant(cible.getBudgetRestant().add(montant));
        if (cible.getFamily() != null) cible.getFamily().addBudget(montant);

        subFamilyRepository.save(source);
        subFamilyRepository.save(cible);

        BudgetTransfer transfer = new BudgetTransfer(da, source, cible, montant, daf);
        budgetTransferRepository.save(transfer);

        Action action = new Action(daf, da, TypeAction.AJUST_BUDGET_SF);
        actionRepository.save(action);

        da.setStatut(StatutDA.EN_ATTENTE_AMG);
        return daHeaderRepository.save(da);
    }

    public DaHeader legacyAjusterBudgetFamille(Integer daId, Integer dgId, Integer subCibleId, BigDecimal montant) {
        DaHeader da = daHeaderRepository.findById(daId).orElseThrow();
        User dg = userRepository.findById(dgId).orElseThrow();
        SubFamily cible = subFamilyRepository.findByIdWithLock(subCibleId).orElseThrow();

        cible.setBudgetRestant(cible.getBudgetRestant().add(montant));
        if (cible.getFamily() != null) cible.getFamily().addBudget(montant);
        subFamilyRepository.save(cible);

        BudgetTransfer transfer = new BudgetTransfer(da, null, cible, montant, dg);
        budgetTransferRepository.save(transfer);

        Action action = new Action(dg, da, TypeAction.VALID_BUDGET_FAMILLE);
        actionRepository.save(action);

        da.setStatut(StatutDA.VALIDEE);
        return daHeaderRepository.save(da);
    }
}

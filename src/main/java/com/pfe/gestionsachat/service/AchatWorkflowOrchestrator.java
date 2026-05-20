package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AchatWorkflowOrchestrator {

    @Autowired private DaHeaderRepository daHeaderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SubFamilyRepository subFamilyRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private PurchaseOrderService purchaseOrderService;

    public static class BudgetCheckResult {
        public static final boolean SUFFISANT = true;
        public static final boolean INSUFFISANT = false;
        public boolean suffisant;
        public BigDecimal montantRequis;
        public BigDecimal budgetActuel;
        public String message;
        public BudgetCheckResult(boolean s, BigDecimal mr, BigDecimal ba, String m) {
            this.suffisant = s; this.montantRequis = mr; this.budgetActuel = ba; this.message = m;
        }
    }

    @Transactional
    public BudgetCheckResult verifierBudget(Integer daId, Integer userId) {
        DaHeader da = daHeaderRepository.findById(daId).orElseThrow();

        if (da.getDetails() == null || da.getDetails().isEmpty()) {
            return new BudgetCheckResult(false, BigDecimal.ZERO, BigDecimal.ZERO, "Aucun détail dans la DA");
        }

        BigDecimal total = da.getDetails().stream()
                .filter(d -> d.getPrixUnitaire() != null && d.getQuantite() != null)
                .map(d -> d.getPrixUnitaire().multiply(BigDecimal.valueOf(d.getQuantite())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Vérification globale : toutes les sous-familles impliquées
        boolean ok = da.getDetails().stream().allMatch(d -> {
            SubFamily sf = d.getSubFamily();
            if (sf == null || sf.getBudgetRestant() == null) return false;
            BigDecimal sfTotal = da.getDetails().stream()
                    .filter(x -> x.getSubFamily() != null && x.getSubFamily().getOidSub().equals(sf.getOidSub())
                            && x.getPrixUnitaire() != null && x.getQuantite() != null)
                    .map(x -> x.getPrixUnitaire().multiply(BigDecimal.valueOf(x.getQuantite())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return sf.getBudgetRestant().compareTo(sfTotal) >= 0;
        });

        SubFamily firstSf = da.getDetails().stream()
                .map(DaDetails::getSubFamily).filter(java.util.Objects::nonNull).findFirst().orElse(null);

        if (ok) {
            da.setStatut(StatutDA.EN_ATTENTE_AMG);
            daHeaderRepository.save(da);
        }

        return new BudgetCheckResult(ok, total,
                firstSf != null && firstSf.getBudgetRestant() != null ? firstSf.getBudgetRestant() : BigDecimal.ZERO,
                ok ? "Budget OK" : "Budget insuffisant");
    }

    @Transactional
    public DaHeader processValidation(Integer daId, Integer userId, ValidationDecision decision, String motif) {
        DaHeader da = daHeaderRepository.findById(daId).orElseThrow();
        if (decision == ValidationDecision.ACCEPTE) {
            switch (da.getStatut()) {
                case EN_ATTENTE_N1:
                    da.setStatut(StatutDA.EN_ATTENTE_TECH);
                    break;
                case EN_ATTENTE_TECH:
                    da.setStatut(StatutDA.EN_ATTENTE_ACHAT);
                    break;
                case EN_ATTENTE_AMG:
                    da.setStatut(StatutDA.EN_ATTENTE_DAF);
                    break;
                case EN_ATTENTE_DAF:
                    da.setStatut(StatutDA.EN_ATTENTE_DG);
                    break;
                case EN_ATTENTE_DG:
                    da.setStatut(StatutDA.VALIDEE);
                    break;
                case EN_ATTENTE_PO:
                    da.setStatut(StatutDA.VALIDEE);
                    break;
                default:
                    throw new IllegalStateException("Transition non autorisée depuis le statut : " + da.getStatut());
            }
        } else {
            da.setStatut(StatutDA.REJETEE);
        }
        return daHeaderRepository.save(da);
    }

    @Transactional
    public DaHeader solliciterAjustement(Integer daId, Integer userId, String type, String motif) {
        DaHeader da = daHeaderRepository.findById(daId).orElseThrow();
        da.setStatut(type.equals("FAMILY") ? StatutDA.EN_ATTENTE_AJUSTEMENT_DG : StatutDA.EN_ATTENTE_AJUSTEMENT_DAF);
        return daHeaderRepository.save(da);
    }

    @Transactional
    public DaHeader ajusterBudgetSousFamille(Integer daId, Integer dafId, Integer sourceId, Integer cibleId, BigDecimal montant) {
        DaHeader da = daHeaderRepository.findById(daId).orElseThrow();
        if (sourceId != null && cibleId != null && montant != null && montant.compareTo(BigDecimal.ZERO) > 0) {
            SubFamily source = subFamilyRepository.findByIdWithLock(sourceId).orElseThrow();
            SubFamily cible  = subFamilyRepository.findByIdWithLock(cibleId).orElseThrow();
            if (!source.hasEnoughBudget(montant)) {
                throw new IllegalStateException("Budget source insuffisant pour le transfert");
            }
            source.deductBudget(montant);
            cible.addBudget(montant);
            subFamilyRepository.save(source);
            subFamilyRepository.save(cible);
        }
        da.setStatut(StatutDA.EN_ATTENTE_AMG);
        return daHeaderRepository.save(da);
    }

    @Transactional
    public DaHeader ajusterBudgetFamille(Integer daId, Integer dgId, Integer cibleId, BigDecimal montant) {
        DaHeader da = daHeaderRepository.findById(daId).orElseThrow();
        if (cibleId != null && montant != null && montant.compareTo(BigDecimal.ZERO) > 0) {
            Family cible = familyRepository.findByIdWithLock(cibleId).orElseThrow();
            cible.addBudget(montant);
            familyRepository.save(cible);
        }
        da.setStatut(StatutDA.EN_ATTENTE_AMG);
        return daHeaderRepository.save(da);
    }

    @Transactional
    public PurchaseOrder manualCreatePO(Integer daId, Integer acheteurId) {
        DaHeader da = daHeaderRepository.findById(daId).orElseThrow();
        List<Integer> sortedSfIds = da.getDetails().stream()
                .map(d -> d.getSubFamily() != null ? d.getSubFamily().getOidSub() : null)
                .filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());

        Map<Integer, SubFamily> lockedSfs = new HashMap<>();
        for (Integer sfId : sortedSfIds) {
            subFamilyRepository.findByIdWithLock(sfId).ifPresent(sf -> lockedSfs.put(sfId, sf));
        }

        for (DaDetails detail : da.getDetails()) {
            if (detail.getSubFamily() != null && detail.getPrixUnitaire() != null && detail.getQuantite() != null) {
                SubFamily sf = lockedSfs.get(detail.getSubFamily().getOidSub());
                if (sf != null) {
                    BigDecimal amountHt = detail.getPrixUnitaire().multiply(BigDecimal.valueOf(detail.getQuantite()));
                    sf.deductBudget(amountHt);
                    subFamilyRepository.save(sf);
                    if (sf.getFamily() != null) {
                        Family fam = familyRepository.findByIdWithLock(sf.getFamily().getIdFamily()).orElseThrow();
                        fam.deductBudget(amountHt);
                        familyRepository.save(fam);
                    }
                }
            }
        }
        PurchaseOrder po = purchaseOrderService.generateFromClassic(da);
        da.setStatut(StatutDA.PO_CREE);
        daHeaderRepository.save(da);
        return po;
    }
}

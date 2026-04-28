package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
@Transactional
public class AchatWorkflowOrchestrator {

    @Autowired private DaHeaderRepository daHeaderRepository;
    @Autowired private ActionRepository actionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SubFamilyRepository subFamilyRepository;
    @Autowired private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired private BudgetTransferRepository budgetTransferRepository;

    /**
     * Traite la validation d'une DA selon le rôle de l'utilisateur.
     * Ordre du processus : N1 → TECHNIQUE → ACHAT (vérif budget) → AMG → DAF → DG → PO
     */
    public DaHeader processValidation(Integer daId, Integer userId, ValidationDecision decision, String motif) {
        if (daId == null || userId == null) throw new IllegalArgumentException("IDs cannot be null");
        DaHeader da = daHeaderRepository.findById(daId)
                .orElseThrow(() -> new RuntimeException("DA introuvable : " + daId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + userId));

        Role role = user.getRole();

        // Enregistrer l'Action
        Action action = new Action(user, da, TypeAction.VALIDATION);
        action.setMetadata(motif);
        actionRepository.save(action);

        // Appliquer la transition de statut
        if (ValidationDecision.REJETE.equals(decision)) {
            da.setStatut(StatutDA.REJETEE);
            return daHeaderRepository.save(da);
        }

        switch (role) {
            case ROLE_N1:
                if (StatutDA.EN_ATTENTE_N1.equals(da.getStatut())) {
                    da.setStatut(StatutDA.EN_ATTENTE_TECH);
                }
                break;

            case ROLE_TECHNICIEN:
                if (StatutDA.EN_ATTENTE_TECH.equals(da.getStatut())) {
                    da.setStatut(StatutDA.EN_ATTENTE_ACHAT);
                }
                break;

            case ROLE_AMG:
                if (StatutDA.EN_ATTENTE_AMG.equals(da.getStatut())) {
                    List<BudgetTransfer> transfers = budgetTransferRepository.findByDaHeader_OidDa(da.getOidDa());
                    
                    // Si on trouve un transfert avec une source -> Circuit DAF
                    boolean toDAF = transfers.stream().anyMatch(t -> t.getSubSource() != null && t.getMontant() != null && t.getMontant().compareTo(java.math.BigDecimal.ZERO) >= 0);
                    // Si on trouve un transfert SANS source (null) -> Circuit DG
                    boolean toDG  = transfers.stream().anyMatch(t -> t.getSubSource() == null);

                    if (toDG) {
                        da.setStatut(StatutDA.EN_ATTENTE_DG); 
                    } else if (toDAF) {
                        da.setStatut(StatutDA.EN_ATTENTE_DAF); 
                    } else {
                        da.setStatut(StatutDA.EN_ATTENTE_DG); // Arbitrage Final
                    }
                }
                break;

            case ROLE_DAF:
                if (StatutDA.EN_ATTENTE_DAF.equals(da.getStatut())) {
                    // After DAF budget approval, it MUST go to DG for final arbitrage
                    da.setStatut(StatutDA.EN_ATTENTE_DG);
                }
                break;

            case ROLE_DG:
                if (StatutDA.EN_ATTENTE_DG.equals(da.getStatut())) {
                    da.setStatut(StatutDA.VALIDEE);
                }
                break;

            default:
                throw new RuntimeException("Rôle non autorisé dans ce workflow : " + role);
        }

        return daHeaderRepository.save(da);
    }

    public BudgetCheckResult verifierBudget(Integer daId, Integer acheteurId) {
        if (daId == null || acheteurId == null) throw new IllegalArgumentException("IDs cannot be null");
        DaHeader da = daHeaderRepository.findById(daId)
                .orElseThrow(() -> new RuntimeException("DA introuvable : " + daId));
        User acheteur = userRepository.findById(acheteurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + acheteurId));

        Action action = new Action(acheteur, da, TypeAction.VALID_BUDGET_FAMILLE);
        action.setMetadata("Vérification budget");

        java.util.Map<SubFamily, java.math.BigDecimal> totalsBySubFamily = da.getDetails().stream()
            .filter(d -> d.getSubFamily() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                DaDetails::getSubFamily,
                java.util.stream.Collectors.reducing(java.math.BigDecimal.ZERO, DaDetails::getTotalPrice, java.math.BigDecimal::add)
            ));

        for (java.util.Map.Entry<SubFamily, java.math.BigDecimal> entry : totalsBySubFamily.entrySet()) {
            if (!entry.getKey().hasEnoughBudget(entry.getValue())) {
                actionRepository.save(action);
                return BudgetCheckResult.INSUFFISANT;
            }
        }

        da.setStatut(StatutDA.EN_ATTENTE_AMG);
        daHeaderRepository.save(da);
        actionRepository.save(action);
        return BudgetCheckResult.SUFFISANT;
    }

    public DaHeader ajusterBudgetSousFamille(Integer daId, Integer dafId,
                                               Integer subSourceId, Integer subCibleId,
                                               java.math.BigDecimal montant) {
        if (daId == null || dafId == null || subSourceId == null || subCibleId == null) {
            throw new IllegalArgumentException("IDs cannot be null");
        }
        DaHeader da = daHeaderRepository.findById(daId).orElseThrow();
        User daf = userRepository.findById(dafId).orElseThrow();
        SubFamily source = subFamilyRepository.findById(subSourceId).orElseThrow();
        SubFamily cible = subFamilyRepository.findById(subCibleId).orElseThrow();

        if (!source.hasEnoughBudget(montant)) {
            throw new RuntimeException("Budget insuffisant");
        }

        source.deductBudget(montant);
        if (source.getFamily() != null) {
            source.getFamily().deductBudget(montant);
        }
        
        cible.setBudgetRestant(cible.getBudgetRestant().add(montant));
        if (cible.getFamily() != null) {
            cible.getFamily().addBudget(montant);
        }

        subFamilyRepository.save(source);
        subFamilyRepository.save(cible);

        BudgetTransfer transfer = new BudgetTransfer(da, source, cible, montant, daf);
        budgetTransferRepository.save(transfer);

        Action action = new Action(daf, da, TypeAction.AJUST_BUDGET_SF);
        actionRepository.save(action);

        da.setStatut(StatutDA.EN_ATTENTE_AMG); // Return to AMG for final review after DAF adjustment
        return daHeaderRepository.save(da);
    }

    public DaHeader ajusterBudgetFamille(Integer daId, Integer dgId, Integer subCibleId, java.math.BigDecimal montant) {
        if (daId == null || dgId == null || subCibleId == null) throw new IllegalArgumentException("IDs cannot be null");
        DaHeader da = daHeaderRepository.findById(daId).orElseThrow();
        User dg = userRepository.findById(dgId).orElseThrow();
        SubFamily cible = subFamilyRepository.findById(subCibleId).orElseThrow();

        cible.setBudgetRestant(cible.getBudgetRestant().add(montant));
        if (cible.getFamily() != null) {
            cible.getFamily().addBudget(montant);
        }
        subFamilyRepository.save(cible);

        BudgetTransfer transfer = new BudgetTransfer(da, null, cible, montant, dg);
        budgetTransferRepository.save(transfer);

        Action action = new Action(dg, da, TypeAction.VALID_BUDGET_FAMILLE);
        actionRepository.save(action);

        da.setStatut(StatutDA.VALIDEE); // DG validation is final
        return daHeaderRepository.save(da);
    }

    public PurchaseOrder manualCreatePO(Integer daId, Integer acheteurId) {
        if (daId == null || acheteurId == null) throw new IllegalArgumentException("IDs cannot be null");
        DaHeader da = daHeaderRepository.findById(daId).orElseThrow();
        User acheteur = userRepository.findById(acheteurId).orElseThrow();

        if (!StatutDA.VALIDEE.equals(da.getStatut())) {
            throw new RuntimeException("La DA doit être VALIDEE avant de créer un PO");
        }

        java.math.BigDecimal total = da.getDetails().stream()
                .map(DaDetails::getTotalPrice)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        PurchaseOrder po = new PurchaseOrder(da, total);
        po.setStatut("VALIDE");
        purchaseOrderRepository.save(po);

        da.setStatut(StatutDA.PO_CREE);
        daHeaderRepository.save(da);

        // Déduire le budget au moment de la commande ferme
        da.getDetails().forEach(detail -> {
            SubFamily sf = detail.getSubFamily();
            if (sf != null) {
                sf.deductBudget(detail.getTotalPrice());
                if (sf.getFamily() != null) {
                    sf.getFamily().deductBudget(detail.getTotalPrice());
                }
                subFamilyRepository.save(sf);
            }
        });

        Action action = new Action(acheteur, da, TypeAction.VALIDATION);
        action.setMetadata("Génération du Bon de Commande (PO)");
        actionRepository.save(action);

        return po;
    }

    public DaHeader solliciterAjustement(Integer daId, Integer acheteurId, String type, String motif) {
        if (daId == null || acheteurId == null) throw new IllegalArgumentException("IDs cannot be null");
        DaHeader da = daHeaderRepository.findById(daId).orElseThrow();
        User acheteur = userRepository.findById(acheteurId).orElseThrow();

        // Enregistrer l'intention dans un transfert
        BudgetTransfer transfer = new BudgetTransfer();
        transfer.setDaHeader(da);
        transfer.setDaf(acheteur);
        transfer.setMontant(java.math.BigDecimal.ZERO);
        
        // On utilise la sous-famille de la première ligne de la DA comme cible par défaut
        if (!da.getDetails().isEmpty()) {
            transfer.setSubCible(da.getDetails().get(0).getSubFamily());
        }

        if ("SUBFAMILY".equals(type)) {
            // Pour indiquer le circuit DAF, on s'assure que subSource n'est PAS null
            // On peut mettre la cible comme source temporairement pour marquer le coup
            transfer.setSubSource(transfer.getSubCible()); 
        } else {
            // subSource = null -> Circuit DG (Famille)
            transfer.setSubSource(null);
        }
        budgetTransferRepository.saveAndFlush(transfer);

        Action action = new Action(acheteur, da, TypeAction.AJUST_BUDGET_SF);
        action.setMetadata("Demande d'ajustement " + type + " : " + motif);
        actionRepository.save(action);

        da.setStatut(StatutDA.EN_ATTENTE_AMG);
        return daHeaderRepository.save(da);
    }

    public enum BudgetCheckResult {
        SUFFISANT,
        INSUFFISANT
    }
}

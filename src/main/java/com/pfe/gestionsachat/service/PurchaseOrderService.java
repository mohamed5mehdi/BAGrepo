package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.FamilyRepository;
import com.pfe.gestionsachat.repository.PurchaseOrderRepository;
import com.pfe.gestionsachat.repository.StatusHistoryRepository;
import com.pfe.gestionsachat.repository.SubFamilyRepository;
import com.pfe.gestionsachat.repository.GrnHeaderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class PurchaseOrderService {

    @Autowired private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired private StatusHistoryRepository historyRepository;
    @Autowired private GrnHeaderRepository grnRepository;
    @Autowired private SubFamilyRepository subFamilyRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired @org.springframework.context.annotation.Lazy private DemandeAchatInterneService demandeAchatInterneService;

    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    /**
     * Calcule le solde de réception pour un PO.
     */
    public Map<String, Integer> getPoBalance(Integer poId) {
        PurchaseOrder po = getPurchaseOrderById(poId);
        Map<String, Integer> balance = new HashMap<>();

        if (po.getDemandeInterne() != null) {
            balance.put("GLOBAL", grnRepository.sumAllReceivedByPoId(poId));
        } else if (po.getDaHeader() != null && po.getDaHeader().getDetails() != null) {
            for (DaDetails d : po.getDaHeader().getDetails()) {
                String code = d.getItemCode();
                if (code != null) {
                    balance.put(code, grnRepository.sumReceivedQuantityByPoIdAndItemCode(poId, code));
                }
            }
        }
        return balance;
    }

    public PurchaseOrder getPurchaseOrderById(Integer id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PO non trouvé : " + id));
    }

    public PurchaseOrder getPurchaseOrderByDa(Integer oidDa) {
        return purchaseOrderRepository.findByDaHeader_OidDa(oidDa);
    }

    public List<PurchaseOrder> getPurchaseOrdersByStatus(POStatus statut) {
        return purchaseOrderRepository.findByStatut(statut);
    }

    /**
     * Génère un PO depuis une DemandeAchatInterne approuvée.
     * Le PO démarre directement à APPROVED car le circuit DA (N1→...→DG) a déjà validé.
     */
    @Transactional
    public PurchaseOrder generateFromInternal(DemandeAchatInterne demande) {
        BigDecimal montantHt = demande.getMontantEstime();
        if (montantHt == null || montantHt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                "Impossible de créer un PO : DA interne [" + demande.getId() + "] n'est pas valorisée."
            );
        }
        BigDecimal tva = montantHt.multiply(new BigDecimal("0.20"));
        BigDecimal montantTtc = montantHt.add(tva).setScale(2, RoundingMode.HALF_UP);

        PurchaseOrder po = new PurchaseOrder();
        po.setDemandeInterne(demande);
        po.setFournisseur(demande.getFournisseur());
        
        // Règle Schéma 2 : Si c'est une pièce SAV (Bypass DG), le PO doit être approuvé par le Resp. Achat
        // Si c'est un flux standard, il est déjà validé par la DG.
        if (Boolean.TRUE.equals(demande.getIsPieceRechange())) {
            po.setStatut(POStatus.PENDING_APPROVAL);
        } else {
            po.setStatut(POStatus.APPROVED);
        }
        
        po.setMontantTotal(montantTtc);
        po.setDateCreation(LocalDate.now());

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        saved.setPoNumber(generatePoNumber(saved.getIdPo()));
        purchaseOrderRepository.save(saved);

        String msg = Boolean.TRUE.equals(demande.getIsPieceRechange()) 
            ? "PO généré en attente d'approbation (Flux SAV Bypass DG)" 
            : "PO généré et approuvé automatiquement depuis DA interne (Flux Standard post-DG)";
            
        logTransition(saved, null, po.getStatut(), null, msg + " #" + demande.getId());

        // Le budget est déjà pré-engagé dans DemandeAchatInterneService.valoriserDemande
        // On ne le déduit plus ici pour éviter un double-dip.

        return saved;
    }

    /**
     * Génère un PO depuis une DA classique (DaHeader).
     */
    @Transactional
    public PurchaseOrder generateFromClassic(DaHeader da) {
        if (da.getDetails() == null || da.getDetails().isEmpty()) {
            throw new IllegalArgumentException("Impossible de créer un PO : DA [" + da.getOidDa() + "] sans détails.");
        }

        BigDecimal totalHt = da.getDetails().stream()
                .filter(d -> d.getPrixUnitaire() != null && d.getQuantite() != null)
                .map(d -> d.getPrixUnitaire().multiply(BigDecimal.valueOf(d.getQuantite())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montantTtc = totalHt.multiply(new BigDecimal("1.20")).setScale(2, RoundingMode.HALF_UP);

        Supplier fournisseur = da.getDetails().stream()
                .map(DaDetails::getFournisseur).filter(java.util.Objects::nonNull).findFirst().orElse(null);

        PurchaseOrder po = new PurchaseOrder();
        po.setDaHeader(da);
        po.setFournisseur(fournisseur);
        po.setStatut(POStatus.APPROVED);
        po.setMontantTotal(montantTtc);
        po.setDateCreation(LocalDate.now());

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        saved.setPoNumber(generatePoNumber(saved.getIdPo()));
        purchaseOrderRepository.save(saved);

        logTransition(saved, null, POStatus.APPROVED, null,
            "PO généré et approuvé automatiquement depuis DA classique #" + da.getOidDa());

        return saved;
    }

    @Transactional
    public PurchaseOrder submitForApproval(Integer poId, User acheteur) {
        PurchaseOrder po = purchaseOrderRepository.findByIdWithLock(poId).orElseThrow();
        assertTransition(po, POStatus.DRAFT, POStatus.PENDING_APPROVAL);
        po.setStatut(POStatus.PENDING_APPROVAL);
        return purchaseOrderRepository.save(po);
    }

    @Transactional
    public PurchaseOrder approvePO(Integer poId, User responsable, String commentaire) {
        PurchaseOrder po = purchaseOrderRepository.findByIdWithLock(poId).orElseThrow();
        assertTransition(po, POStatus.PENDING_APPROVAL, POStatus.APPROVED);
        po.setStatut(POStatus.APPROVED);
        logTransition(po, POStatus.PENDING_APPROVAL, POStatus.APPROVED, responsable, commentaire);
        
        // Déduction budgétaire déjà pré-engagée dans le Flux SAV
        // Pas de déduction supplémentaire ici.
        
        return purchaseOrderRepository.save(po);
    }

    @Transactional
    public PurchaseOrder rejectPO(Integer poId, User responsable, String motif) {
        PurchaseOrder po = purchaseOrderRepository.findByIdWithLock(poId).orElseThrow();
        assertTransition(po, POStatus.PENDING_APPROVAL, POStatus.REJECTED);
        po.setStatut(POStatus.REJECTED);
        if (po.getDemandeInterne() != null) {
            demandeAchatInterneService.rejeterDaSuiteAuPO(po.getDemandeInterne().getId(), responsable, motif);
        }
        logTransition(po, POStatus.PENDING_APPROVAL, POStatus.REJECTED, responsable, motif);
        return purchaseOrderRepository.save(po);
    }

    @Transactional
    public PurchaseOrder shortClose(Integer poId, User responsable, String motif) {
        PurchaseOrder po = purchaseOrderRepository.findByIdWithLock(poId).orElseThrow();
        if (po.getStatut() != POStatus.APPROVED) throw new IllegalStateException("Non-APPROVED");
        po.setStatut(POStatus.SHORT_CLOSED);
        logTransition(po, POStatus.APPROVED, POStatus.SHORT_CLOSED, responsable, motif);
        return purchaseOrderRepository.save(po);
    }

    private void deduireBudgetInterne(DemandeAchatInterne da) {
        if (da.getBudgetSousFamille() != null && da.getMontantEstime() != null) {
            SubFamily sf = subFamilyRepository.findByIdWithLock(
                da.getBudgetSousFamille().getOidSub()).orElseThrow();
            sf.deductBudget(da.getMontantEstime());
            subFamilyRepository.save(sf);
            if (sf.getFamily() != null) {
                Family fam = familyRepository.findByIdWithLock(
                    sf.getFamily().getIdFamily()).orElseThrow();
                fam.deductBudget(da.getMontantEstime());
                familyRepository.save(fam);
            }
        }
    }

    private void assertTransition(PurchaseOrder po, POStatus from, POStatus to) {
        if (po.getStatut() != from) throw new IllegalStateException("Transition invalide");
    }

    private void logTransition(PurchaseOrder po, POStatus avant, POStatus apres, User acteur, String commentaire) {
        historyRepository.save(new StatusHistory("PurchaseOrder", po.getIdPo().longValue(), 
            avant != null ? avant.name() : null, apres.name(), acteur, commentaire));
    }

    private String generatePoNumber(Integer id) {
        return "PO-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-" + String.format("%05d", id);
    }
}
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

    /**
     * Taux de TVA par défaut appliqué lors de la génération du PO depuis une DA interne.
     * Règle BAG ERP : 20% si aucun taux spécifique n'est défini au niveau de la ligne GRC.
     * À centraliser dans une table de configuration si le taux devient paramétrable.
     */
    private static final BigDecimal TVA_DEFAUT = new BigDecimal("0.20");
    private static final BigDecimal TVA_FACTEUR_DEFAUT = BigDecimal.ONE.add(TVA_DEFAUT); // 1.20

    @Autowired private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired private StatusHistoryRepository historyRepository;
    @Autowired private GrnHeaderRepository grnRepository;
    @Autowired private SubFamilyRepository subFamilyRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired @org.springframework.context.annotation.Lazy private DemandeAchatInterneService demandeAchatInterneService;
    @Autowired @org.springframework.context.annotation.Lazy private GrnService grnService;
    @Autowired @org.springframework.context.annotation.Lazy private GrcService grcService;

    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    public List<PurchaseOrder> getPendingInternalPOsForAutomation() {
        return purchaseOrderRepository.findPendingInternalPOsForAutomation(POStatus.APPROVED);
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
        List<PurchaseOrder> pos = purchaseOrderRepository.findByStatut(statut);
        if (statut == POStatus.APPROVED) {
            // Filtrer les POs déjà totalement réceptionnés
            return pos.stream().filter(po -> !isFullyReceived(po)).toList();
        }
        return pos;
    }

    private boolean isFullyReceived(PurchaseOrder po) {
        Map<String, Integer> balance = getPoBalance(po.getIdPo());
        if (po.getDemandeInterne() != null) {
            int ordered = po.getDemandeInterne().getQuantite() != null ? po.getDemandeInterne().getQuantite() : 0;
            int received = balance.getOrDefault("GLOBAL", 0);
            return ordered > 0 && received >= ordered;
        } else if (po.getDaHeader() != null && po.getDaHeader().getDetails() != null) {
            boolean allReceived = true;
            for (DaDetails d : po.getDaHeader().getDetails()) {
                String code = d.getItemCode();
                if (code != null) {
                    int ordered = d.getQuantite() != null ? d.getQuantite() : 0;
                    int received = balance.getOrDefault(code, 0);
                    if (ordered > 0 && received < ordered) {
                        allReceived = false;
                        break;
                    }
                }
            }
            return allReceived;
        }
        return false;
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
        // TVA calculée via constante — toute modification du taux se fait en un seul endroit.
        BigDecimal tva = montantHt.multiply(TVA_DEFAUT).setScale(2, RoundingMode.HALF_UP);
        BigDecimal montantTtc = montantHt.add(tva).setScale(2, RoundingMode.HALF_UP);

        PurchaseOrder po = new PurchaseOrder();
        po.setDemandeInterne(demande);
        po.setFournisseur(demande.getFournisseur());
        
        // Règle Schéma 2 : Si c'est une pièce SAV (Bypass DG), le PO doit être approuvé par le Resp. Achat
        // Si c'est un flux standard, il est déjà validé par la DG.
        // Toutes les commandes générées à partir d'une DI (qu'elles soient SAV ou Standard)
        // nécessitent l'approbation du Responsable Achat (Omar Kettani) pour acquérir le statut APPROVED.
        po.setStatut(POStatus.PENDING_APPROVAL);
        
        po.setMontantTotal(montantTtc);
        po.setDateCreation(LocalDate.now());

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        saved.setPoNumber(generatePoNumber(saved.getIdPo()));
        purchaseOrderRepository.save(saved);

        String msg = Boolean.TRUE.equals(demande.getIsPieceRechange()) 
            ? "PO généré en attente d'approbation (Flux SAV Bypass DG)" 
            : "PO généré en attente d'approbation (Flux Standard post-DG)";
            
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

        // Même constante TVA_DEFAUT — cohérence garantie avec generateFromInternal()
        BigDecimal montantTtc = totalHt.multiply(TVA_FACTEUR_DEFAUT).setScale(2, RoundingMode.HALF_UP);

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

    @Transactional
    public void autoGenerateGrnGrc(Integer poId, User acheteur) {
        PurchaseOrder po = purchaseOrderRepository.findByIdWithLock(poId).orElseThrow();
        if (po.getStatut() != POStatus.APPROVED) {
            throw new IllegalStateException("Le PO doit être approuvé avant de générer le GRN/GRC.");
        }
        if (po.getDemandeInterne() == null) {
            throw new IllegalStateException("Seul un PO de demande interne peut générer le GRN/GRC automatiquement via ce processus.");
        }
        
        // 1. Génération du GRN
        GrnHeader grn = new GrnHeader();
        grn.setPurchaseOrder(po);
        grn.setSupplier(po.getFournisseur());
        grn.setDeliveryNoteNumber("AUTO-BL-" + po.getPoNumber());
        grn.setReceiptDate(LocalDate.now());
        grn.setReceivedBy(acheteur);
        
        List<GrnDetails> grnDetailsList = new java.util.ArrayList<>();
        int qtyOrdered = po.getDemandeInterne().getQuantite() != null ? po.getDemandeInterne().getQuantite() : 1;
        String itemCode = po.getDemandeInterne().getItemCode();
        if (itemCode == null && po.getDemandeInterne().getDetails() != null && !po.getDemandeInterne().getDetails().isEmpty()) {
            itemCode = po.getDemandeInterne().getDetails().get(0).getItemCode();
        }
        
        GrnDetails gd = new GrnDetails();
        gd.setItemCode(itemCode != null ? itemCode : "INTERNE-ITEM");
        gd.setItemName(po.getDemandeInterne().getDesignation());
        gd.setOrderedQuantity(qtyOrdered);
        gd.setReceivedQuantity(qtyOrdered);
        gd.setAcceptedQuantity(qtyOrdered);
        gd.setQualityStatus(QualityStatus.APPROVED);
        grnDetailsList.add(gd);
        grn.setDetails(grnDetailsList);
        
        GrnHeader savedGrn = grnService.createGrn(grn);
        grnService.completeGrnEntry(savedGrn.getId(), acheteur); // Passe le GRN à ENTRY_COMPLETED + màj Stock
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
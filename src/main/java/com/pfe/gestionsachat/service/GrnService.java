package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class GrnService {
    @Autowired
    private GrnHeaderRepository grnRepository;
    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired
    private WarehouseService warehouseService;

    @Transactional
    public GrnHeader createGrn(GrnHeader grn) {
        if (grn.getPurchaseOrder() == null || grn.getPurchaseOrder().getIdPo() == null) {
            throw new IllegalArgumentException("Le Bon de Commande (PO) est requis pour la réception.");
        }

        Integer poId = grn.getPurchaseOrder().getIdPo();
        if (poId == null) throw new IllegalArgumentException("ID PO manquant");
        
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("PO introuvable : " + poId));

        // R5: 4-Way Matching Strict (Réception Partielle)
        if (grn.getDetails() != null) {
            for (GrnDetails newDetail : grn.getDetails()) {
                String itemCode = newDetail.getItemCode();

                // Quantité ordonnée selon la source du PO (DA classique ou Demande Interne)
                Integer qtyOrdered = 0;
                if (po.getDaHeader() != null && po.getDaHeader().getDetails() != null) {
                    qtyOrdered = po.getDaHeader().getDetails().stream()
                            .filter(d -> itemCode != null && itemCode.equals(d.getItemCode()))
                            .mapToInt(d -> d.getQuantite() != null ? d.getQuantite() : 0)
                            .sum();
                } else if (po.getDemandeInterne() != null) {
                    // Circuit interne : pas de détails multi-items, on utilise la quantité globale
                    qtyOrdered = po.getDemandeInterne().getQuantite() != null
                            ? po.getDemandeInterne().getQuantite() : 0;
                }

                if (qtyOrdered == 0) {
                    throw new IllegalArgumentException("Article introuvable dans le PO : " + itemCode);
                }

                // Somme des réceptions existantes pour ce PO (Optimisé)
                Integer qtyAlreadyReceived = grnRepository.sumReceivedQuantityByPoIdAndItemCode(po.getIdPo(), itemCode);

                if (qtyAlreadyReceived + newDetail.getReceivedQuantity() > qtyOrdered) {
                    throw new com.pfe.gestionsachat.exception.ReceptionDoublonException(
                        "Sur-réception détectée pour l'article " + itemCode +
                        " (Total reçu: " + (qtyAlreadyReceived + newDetail.getReceivedQuantity()) +
                        ", Commandé: " + qtyOrdered + ")"
                    );
                }
            }
        }


        grn.setStatus(GrnStatus.DRAFT);
        if (grn.getDetails() != null) {
            grn.getDetails().forEach(d -> d.setGrnHeader(grn));
        }
        return grnRepository.save(grn);
    }

    @Transactional
    public GrnHeader validateGrn(@org.springframework.lang.NonNull Long grnId) {
        GrnHeader grn = grnRepository.findById(grnId).orElseThrow();
        grn.setStatus(GrnStatus.VALIDATED);

        for (GrnDetails detail : grn.getDetails()) {
            if (detail.getQualityStatus() == QualityStatus.APPROVED) {
                // Flux 1 : IN (Physique) - On ajoute uniquement ce qui est accepté
                warehouseService.addStock(detail.getItemCode(), detail.getAcceptedQuantity(), "GRN-" + grn.getId());
            } 
            // Les pièces rejetées ne sont pas ajoutées au stock 'Available', donc pas besoin de removeStock.
            // On pourrait ajouter un mouvement 'REJECTED' sans impact sur la quantité available si besoin.
        }
        return grnRepository.save(grn);
    }
}

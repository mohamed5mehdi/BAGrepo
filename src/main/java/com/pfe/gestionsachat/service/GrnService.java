package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class GrnService {

    @Autowired private GrnHeaderRepository grnRepository;
    @Autowired private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired private WarehouseService warehouseService;
    @Autowired private StatusHistoryRepository historyRepository;

    /**
     * Crée un GRN avec vérification stricte des règles BAG ERP.
     * FIXES appliqués :
     *  - Guard PO.APPROVED (un seul check)
     *  - resolveOrderedQuantity corrigé : DemandeInterne → DA.quantite sans match designation
     *  - DemandeInterne PO : validation sur total global reçu (pas par itemCode)
     *  - Null guard sur grn.getDetails()
     */
    @Transactional(readOnly = true)
    public List<GrnHeader> getAllGrns() {
        return grnRepository.findAll();
    }

    @Transactional
    public GrnHeader createGrn(GrnHeader grn) {
        if (grn.getPurchaseOrder() == null || grn.getPurchaseOrder().getIdPo() == null) {
            throw new IllegalArgumentException("Le Bon de Commande (PO) est requis.");
        }

        Integer poId = grn.getPurchaseOrder().getIdPo();

        // ── LOCK PESSIMISTE : élimine la race condition de sur-réception concurrente ──
        PurchaseOrder po = purchaseOrderRepository.findByIdWithLock(poId)
                .orElseThrow(() -> new RuntimeException("PO introuvable : " + poId));

        // ── GUARD unique : PO doit être APPROVED (SHORT_CLOSED et REJECTED sont != APPROVED) ──
        if (po.getStatut() != POStatus.APPROVED) {
            throw new IllegalStateException(
                "Impossible de créer un GRN : PO [" + poId + "] en statut ["
                + po.getStatut() + "]. Requis : APPROVED."
            );
        }

        List<GrnDetails> details = grn.getDetails() != null ? grn.getDetails() : Collections.emptyList();

        boolean isInternalDA = (po.getDemandeInterne() != null);

        // ── Validation quantités ──────────────────────────────────────────────
        if (isInternalDA) {
            /*
             * BUG FIX #1 — DemandeInterne (mono-article) :
             * Le Magasinier assigne lui-même le code catalogue → l'itemCode GRN
             * ne correspond JAMAIS à DA.designation (human-readable).
             * La validation se fait sur le TOTAL GLOBAL reçu pour ce PO,
             * toutes lignes GRN confondues, vs demandeInterne.quantite.
             */
            int qtyOrdered = po.getDemandeInterne().getQuantite() != null
                    ? po.getDemandeInterne().getQuantite() : 0;

            if (qtyOrdered == 0) {
                throw new IllegalArgumentException("Quantité commandée introuvable sur la DA interne.");
            }

            // Total reçu sur TOUS les GRNs de ce PO, tous itemCodes confondus
            Integer alreadyReceivedAll = grnRepository.sumAllReceivedByPoId(poId);
            int newQty = details.stream()
                    .mapToInt(d -> d.getReceivedQuantity() != null ? d.getReceivedQuantity() : 0)
                    .sum();

            if ((alreadyReceivedAll + newQty) > qtyOrdered) {
                    throw new com.pfe.gestionsachat.exception.OverReceptionException(
                    "Sur-réception bloquée (DA interne) : total demandé après GRN = "
                    + (alreadyReceivedAll + newQty) + " / Commandé = " + qtyOrdered
                );
            }

            // Calcul shippedQty (solde restant)
            int shippedQty = qtyOrdered - alreadyReceivedAll - newQty;
            for (GrnDetails d : details) {
                d.setOrderedQuantity(qtyOrdered);
                d.setShippedQuantity(Math.max(0, shippedQty));
            }

        } else {
            /*
             * Circuit DA classique (multi-items) : validation par itemCode.
             */
            for (GrnDetails newDetail : details) {
                String itemCode = newDetail.getItemCode();
                int qtyOrdered = resolveOrderedQuantityFromDaHeader(po, itemCode);

                if (qtyOrdered == 0) {
                    throw new IllegalArgumentException(
                        "Article [" + itemCode + "] introuvable dans le PO [" + poId + "]."
                    );
                }

                Integer qtyAlreadyReceived = grnRepository.sumReceivedQuantityByPoIdAndItemCode(poId, itemCode);
                int thisReceived = newDetail.getReceivedQuantity() != null ? newDetail.getReceivedQuantity() : 0;
                int totalAfter = qtyAlreadyReceived + thisReceived;

                if (totalAfter > qtyOrdered) {
                        throw new com.pfe.gestionsachat.exception.OverReceptionException(
                        "Sur-réception bloquée pour [" + itemCode + "] : reçu=" + totalAfter
                        + " > commandé=" + qtyOrdered
                    );
                }

                newDetail.setShippedQuantity(qtyOrdered - totalAfter);
                newDetail.setOrderedQuantity(qtyOrdered);
            }
        }

        grn.setStatus(GrnStatus.PENDING);
        grn.setPurchaseOrder(po);
        details.forEach(d -> d.setGrnHeader(grn));

        return grnRepository.save(grn);
    }

    /**
     * Entry Completed — action Magasinier (pas d'approbation hiérarchique, règle BAG ERP).
     * FIX : null guard sur getDetails(), user nullable dans StatusHistory.
     */
    @Transactional
    public GrnHeader completeGrnEntry(Long grnId, User magasinier) {
        GrnHeader grn = grnRepository.findById(grnId)
                .orElseThrow(() -> new RuntimeException("GRN introuvable : " + grnId));

        if (grn.getStatus() != GrnStatus.PENDING) {
            throw new IllegalStateException(
                "GRN [" + grnId + "] est déjà " + grn.getStatus() + ". Entry Completed ne peut être appliqué qu'une fois."
            );
        }

        // ── Mise à jour stock au GRN (règle BAG ERP — PAS au GRC) ─────────────
        // FIX #3 : null guard sur getDetails()
        List<GrnDetails> details = grn.getDetails() != null ? grn.getDetails() : Collections.emptyList();
        for (GrnDetails detail : details) {
            if (detail.getQualityStatus() == QualityStatus.APPROVED
                    && detail.getAcceptedQuantity() != null
                    && detail.getAcceptedQuantity() > 0) {
                warehouseService.addStock(
                    detail.getItemCode(),
                    detail.getItemName(),
                    detail.getAcceptedQuantity(),
                    "GRN-" + grn.getId()
                );
            }
        }

        grn.setStatus(GrnStatus.ENTRY_COMPLETED);

        // Génération grnNumber si non encore assigné
        if (grn.getGrnNumber() == null) {
            String yyyyMM = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
            grn.setGrnNumber("GRN-" + yyyyMM + "-" + String.format("%05d", grn.getId()));
        }

        GrnHeader saved = grnRepository.save(grn);

        // StatusHistory — user nullable (no FK NOT NULL constraint on modifie_par_id)
        historyRepository.save(new StatusHistory(
            "GrnHeader", grn.getId(),
            GrnStatus.PENDING.name(), GrnStatus.ENTRY_COMPLETED.name(),
            magasinier, "Entry Completed par le Magasinier"
        ));

        return saved;
    }

    /** Alias endpoint /grn/{id}/valider — délègue à completeGrnEntry sans user. */
    @Transactional
    public GrnHeader validateGrn(Long grnId) {
        return completeGrnEntry(grnId, null);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    /**
     * Résout la quantité commandée pour un article dans une DA classique (multi-items).
     * N'est PAS appelé pour les POs DemandeInterne (circuit séparé dans createGrn).
     */
    private int resolveOrderedQuantityFromDaHeader(PurchaseOrder po, String itemCode) {
        if (po.getDaHeader() == null || po.getDaHeader().getDetails() == null) return 0;
        return po.getDaHeader().getDetails().stream()
                .filter(d -> itemCode != null && itemCode.equals(d.getItemCode()))
                .mapToInt(d -> d.getQuantite() != null ? d.getQuantite() : 0)
                .sum();
    }
}

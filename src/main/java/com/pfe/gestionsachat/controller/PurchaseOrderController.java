package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.POStatus;
import com.pfe.gestionsachat.model.PurchaseOrder;
import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.repository.UserRepository;
import com.pfe.gestionsachat.service.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")

public class PurchaseOrderController {

    @Autowired private PurchaseOrderService purchaseOrderService;
    @Autowired private com.pfe.gestionsachat.service.PdfExportService pdfExportService;
    @Autowired private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> getAllPurchaseOrders() {
        return ResponseEntity.ok(purchaseOrderService.getAllPurchaseOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> getPurchaseOrderById(@PathVariable @org.springframework.lang.NonNull Integer id) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderById(id));
    }

    @GetMapping("/da/{oidDa}")
    public ResponseEntity<PurchaseOrder> getPurchaseOrderByDa(@PathVariable @org.springframework.lang.NonNull Long oidDa) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderByDa(oidDa));
    }

    @GetMapping("/status/{statut}")
    public ResponseEntity<List<PurchaseOrder>> getPurchaseOrdersByStatus(
            @PathVariable @org.springframework.lang.NonNull POStatus statut) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrdersByStatus(statut));
    }

    /**
     * Retourne le solde de réception pour chaque article du PO.
     * Requis pour l'interface Magasinier (Calcul du Shipped Quantity).
     */
    @GetMapping("/{id}/balance")
    public ResponseEntity<java.util.Map<String, Integer>> getPoBalance(@PathVariable Integer id) {
        return ResponseEntity.ok(purchaseOrderService.getPoBalance(id));
    }

    /** Responsable Achat approuve un PO PENDING_APPROVAL → APPROVED */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('RESP_ACHAT', 'ADMINISTRATEUR')")
    public ResponseEntity<PurchaseOrder> approvePO(
            @PathVariable Integer id,
            @RequestParam Integer userId,
            @RequestParam(required = false) String commentaire) {
        User responsable = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(purchaseOrderService.approvePO(id, responsable, commentaire));
    }

    /** Responsable Achat rejette un PO PENDING_APPROVAL → REJECTED */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('RESP_ACHAT', 'ADMINISTRATEUR')")
    public ResponseEntity<PurchaseOrder> rejectPO(
            @PathVariable Integer id,
            @RequestParam Integer userId,
            @RequestParam String motif) {
        User responsable = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(purchaseOrderService.rejectPO(id, responsable, motif));
    }

    /** Clôture manuelle forcée SHORT_CLOSED (APPROVED → SHORT_CLOSED) */
    @PutMapping("/{id}/short-close")
    @PreAuthorize("hasAnyRole('RESP_ACHAT', 'ACHETEUR', 'ACHETEUR_INFORMATIQUE', 'ACHETEUR_BUREAUTIQUE', 'ACHETEUR_MOBILIER', 'ACHETEUR_CONSOMMABLE', 'ACHETEUR_AUTRE', 'ADMINISTRATEUR')")
    public ResponseEntity<PurchaseOrder> shortClose(
            @PathVariable Integer id,
            @RequestParam Integer userId,
            @RequestParam String motif) {
        User responsable = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(purchaseOrderService.shortClose(id, responsable, motif));
    }

    @GetMapping("/{id}/download")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadPo(@PathVariable Integer id) {
        PurchaseOrder po = purchaseOrderService.getPurchaseOrderById(id);
        return generatePdfResponse(po);
    }

    @GetMapping("/da/{oidDa}/download")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadPoByDa(@PathVariable Long oidDa) {
        PurchaseOrder po = purchaseOrderService.getPurchaseOrderByDa(oidDa);
        if (po == null) {
            return ResponseEntity.notFound().build();
        }
        return generatePdfResponse(po);
    }

    private ResponseEntity<byte[]> generatePdfResponse(PurchaseOrder po) {
        byte[] pdfBytes = pdfExportService.generatePurchaseOrderPdf(po);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=BC_" + po.getIdPo() + ".pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}

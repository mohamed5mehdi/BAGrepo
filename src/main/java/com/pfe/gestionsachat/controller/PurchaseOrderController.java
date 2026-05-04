package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.PurchaseOrder;
import com.pfe.gestionsachat.service.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@CrossOrigin(origins = "*")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private com.pfe.gestionsachat.service.PdfExportService pdfExportService;

    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> getAllPurchaseOrders() {
        return ResponseEntity.ok(purchaseOrderService.getAllPurchaseOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> getPurchaseOrderById(@PathVariable @org.springframework.lang.NonNull Integer id) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderById(id));
    }

    @GetMapping("/da/{oidDa}")
    public ResponseEntity<PurchaseOrder> getPurchaseOrderByDa(@PathVariable @org.springframework.lang.NonNull Integer oidDa) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderByDa(oidDa));
    }

    @GetMapping("/status/{statut}")
    public ResponseEntity<List<PurchaseOrder>> getPurchaseOrdersByStatus(@PathVariable @org.springframework.lang.NonNull String statut) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrdersByStatus(statut));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadPo(@PathVariable Integer id) {
        PurchaseOrder po = purchaseOrderService.getPurchaseOrderById(id);
        return generatePdfResponse(po);
    }

    @GetMapping("/da/{oidDa}/download")
    public ResponseEntity<byte[]> downloadPoByDa(@PathVariable Integer oidDa) {
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
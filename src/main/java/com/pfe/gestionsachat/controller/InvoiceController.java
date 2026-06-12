package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.Invoice;
import com.pfe.gestionsachat.model.InvoiceStatus;
import com.pfe.gestionsachat.repository.InvoiceRepository;
import com.pfe.gestionsachat.service.MatchingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@RestController
@RequestMapping("/api/invoice")

public class InvoiceController {

    @Autowired
    private MatchingService matchingService;
    @Autowired
    private InvoiceRepository invoiceRepository;

    @GetMapping
    public ResponseEntity<List<Invoice>> getAll() {
        return ResponseEntity.ok(matchingService.getAllInvoices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceRepository.findById(id).orElseThrow());
    }

    @PostMapping
    @org.springframework.transaction.annotation.Transactional
    @PreAuthorize("hasAnyRole('COMPTABLE', 'ADMINISTRATEUR')")
    public ResponseEntity<Invoice> createInvoice(@RequestBody Invoice invoice) {
        invoice.setStatus(InvoiceStatus.RECEIVED);
        return ResponseEntity.ok(invoiceRepository.save(invoice));
    }

    @PostMapping("/{id}/match")
    @PreAuthorize("hasAnyRole('COMPTABLE', 'ADMINISTRATEUR')")
    public ResponseEntity<Invoice> matchInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(matchingService.matchInvoice(java.util.Objects.requireNonNull(id)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('DAF', 'DG', 'ADMINISTRATEUR')")
    public ResponseEntity<Invoice> approveInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(matchingService.approveInvoice(java.util.Objects.requireNonNull(id)));
    }

    @Autowired
    private com.pfe.gestionsachat.service.PdfExportService pdfExportService;

    @GetMapping("/{id}/download")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {
        // Recherche par ID natif de l'invoice, ou par PO ID si non trouvé
        Invoice invoice = invoiceRepository.findById(id)
                .orElseGet(() -> invoiceRepository.findByPurchaseOrder_IdPo(id.intValue()).stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("Facture introuvable pour l'ID ou le PO spécifié")));
        
        byte[] pdfBytes = pdfExportService.generateInvoicePdf(invoice);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=INV_" + invoice.getInvoiceNumber() + ".pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}


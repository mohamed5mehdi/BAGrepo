package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.Invoice;
import com.pfe.gestionsachat.model.InvoiceStatus;
import com.pfe.gestionsachat.repository.InvoiceRepository;
import com.pfe.gestionsachat.service.MatchingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoice")
@CrossOrigin(origins = "*")
public class InvoiceController {

    @Autowired
    private MatchingService matchingService;
    @Autowired
    private InvoiceRepository invoiceRepository;

    @PostMapping
    public ResponseEntity<Invoice> createInvoice(@RequestBody Invoice invoice) {
        invoice.setStatus(InvoiceStatus.RECEIVED);
        return ResponseEntity.ok(invoiceRepository.save(invoice));
    }

    @PostMapping("/{id}/match")
    public ResponseEntity<Invoice> matchInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(matchingService.matchInvoice(java.util.Objects.requireNonNull(id)));
    }
}

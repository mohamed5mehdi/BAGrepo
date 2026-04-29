package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MatchingService {
    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private GrcHeaderRepository grcHeaderRepository;

    public Invoice matchInvoice(@org.springframework.lang.NonNull Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
        PurchaseOrder po = invoice.getPurchaseOrder();
        GrnHeader grn = invoice.getGrnHeader();
        
        GrcHeader grc = grn.getGrcHeader();
        if (grc == null) {
            // Tentative de récupération directe par GRN ID
            grc = grcHeaderRepository.findByGrnHeader(grn).orElse(null);
        }

        if (po == null || grc == null) {
            throw new RuntimeException("PO or GRC missing for 3-way matching (GRC found: " + (grc != null) + ")");
        }

        boolean match = po.getTotalAmount().compareTo(grc.getTotalAmount()) == 0 
                     && grc.getTotalAmount().compareTo(invoice.getMontantTTC()) == 0;

        if (match) {
            invoice.setStatus(InvoiceStatus.MATCHED);
            // Auto-approve if matched?
            invoice.setStatus(InvoiceStatus.APPROVED);
        } else {
            invoice.setStatus(InvoiceStatus.REJECTED);
        }
        
        return invoiceRepository.save(invoice);
    }
}

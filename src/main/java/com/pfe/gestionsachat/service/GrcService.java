package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrcService {
    @Autowired
    private GrcHeaderRepository grcRepository;
    @Autowired
    private StockItemRepository stockItemRepository;
    @Autowired
    private InvoiceRepository invoiceRepository;

    @Transactional
    public GrcHeader createGrc(GrcHeader grc) {
        grc.setStatus(GrcStatus.DRAFT);
        if (grc.getDetails() != null) {
            grc.getDetails().forEach(d -> d.setGrcHeader(grc));
        }
        if (grc.getGrnHeader() != null) {
            grc.getGrnHeader().setGrcHeader(grc);
        }
        return grcRepository.save(grc);
    }

    @Transactional
    public GrcHeader validateGrc(@org.springframework.lang.NonNull Long grcId) {
        GrcHeader grc = grcRepository.findById(grcId).orElseThrow();
        grc.setStatus(GrcStatus.VALIDATED);
        
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (GrcDetails detail : grc.getDetails()) {
            java.math.BigDecimal unitCost = java.math.BigDecimal.valueOf(detail.getUnitCost() != null ? detail.getUnitCost() : 0.0);
            java.math.BigDecimal lineTotal = unitCost.multiply(java.math.BigDecimal.valueOf(detail.getAcceptedQuantity()));
            detail.setTotalCost(lineTotal.doubleValue());
            
            // Flux 2 : Valorisation financière du stock
            stockItemRepository.findByItemCode(detail.getItemCode()).stream()
                .findFirst()
                .ifPresent(stockItem -> {
                    stockItem.setUnitCost(unitCost.doubleValue());
                    stockItemRepository.save(stockItem);
                });

            java.math.BigDecimal taxFactor = java.math.BigDecimal.ONE;
            if (detail.getTaxRate() != null) {
                taxFactor = java.math.BigDecimal.ONE.add(java.math.BigDecimal.valueOf(detail.getTaxRate()).divide(java.math.BigDecimal.valueOf(100)));
            }
            java.math.BigDecimal montantTTC = lineTotal.multiply(taxFactor);
            detail.setMontantTTC(montantTTC.doubleValue());
            total = total.add(montantTTC);
        }
        grc.setTotalAmount(total);
        
        // Génération de la facture attendue
        Invoice invoice = new Invoice();
        invoice.setGrnHeader(grc.getGrnHeader());
        invoice.setPurchaseOrder(grc.getGrnHeader().getPurchaseOrder());
        invoice.setInvoiceNumber("INV-AUTO-" + grc.getId());
        invoice.setInvoiceDate(java.time.LocalDate.now());
        invoice.setMontantTTC(total);
        invoice.setMontantHT(total.divide(java.math.BigDecimal.valueOf(1.20), 2, java.math.RoundingMode.HALF_UP)); // Estimation HT
        invoice.setStatus(InvoiceStatus.RECEIVED);
        invoiceRepository.save(invoice);
        
        return grcRepository.save(grc);
    }
}

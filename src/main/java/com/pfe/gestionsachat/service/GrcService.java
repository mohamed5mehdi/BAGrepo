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
    @Autowired
    private GrnHeaderRepository grnHeaderRepository;

    @Transactional
    public GrcHeader createGrc(GrcHeader grc) {
        grc.setStatus(GrcStatus.DRAFT);
        if (grc.getDetails() != null) {
            grc.getDetails().forEach(d -> d.setGrcHeader(grc));
        }
        if (grc.getGrnHeader() != null && grc.getGrnHeader().getId() != null) {
            // On cherche le GRN par son ID ou par le PO ID s'il est passé en tant qu'ID (compatibilité frontend)
            Long id = grc.getGrnHeader().getId();
            GrnHeader actualGrn = grnHeaderRepository.findById(id).orElse(null);
            
            if (actualGrn == null) {
                // Tentative par PO ID
                actualGrn = grnHeaderRepository.findByPurchaseOrder_IdPo(id.intValue()).stream().findFirst().orElse(null);
            }

            if (actualGrn != null) {
                grc.setGrnHeader(actualGrn);
            }
        }
        return grcRepository.save(grc);
    }

    @Transactional
    public GrcHeader validateGrc(@org.springframework.lang.NonNull Long grcId) {
        GrcHeader grc = grcRepository.findById(grcId).orElseThrow(() -> new RuntimeException("GRC introuvable: " + grcId));
        grc.setStatus(GrcStatus.VALIDATED);
        
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (GrcDetails detail : grc.getDetails()) {
            java.math.BigDecimal unitCost = java.math.BigDecimal.valueOf(detail.getUnitCost() != null ? detail.getUnitCost() : 0.0);
            java.math.BigDecimal quantity = java.math.BigDecimal.valueOf(detail.getAcceptedQuantity() != null ? detail.getAcceptedQuantity() : 0);
            java.math.BigDecimal lineTotal = unitCost.multiply(quantity);
            detail.setTotalCost(lineTotal.doubleValue());
            
            // Flux 2 : Valorisation financière du stock
            if (detail.getItemCode() != null) {
                stockItemRepository.findByItemCode(detail.getItemCode()).stream()
                    .findFirst()
                    .ifPresent(stockItem -> {
                        stockItem.setUnitCost(unitCost.doubleValue());
                        stockItemRepository.save(stockItem);
                    });
            }

            java.math.BigDecimal taxFactor = java.math.BigDecimal.ONE;
            if (detail.getTaxRate() != null && detail.getTaxRate() > 0) {
                taxFactor = java.math.BigDecimal.ONE.add(java.math.BigDecimal.valueOf(detail.getTaxRate()).divide(java.math.BigDecimal.valueOf(100)));
            } else {
                // Default TVA 20% si non spécifié pour le calcul TTC
                taxFactor = java.math.BigDecimal.valueOf(1.20);
                detail.setTaxRate(20.0);
            }
            java.math.BigDecimal montantTTC = lineTotal.multiply(taxFactor);
            detail.setMontantTTC(montantTTC.doubleValue());
            total = total.add(montantTTC);
        }
        grc.setTotalAmount(total);
        
        // Génération de la facture attendue si lien GRN existe
        if (grc.getGrnHeader() != null) {
            Invoice invoice = new Invoice();
            invoice.setGrnHeader(grc.getGrnHeader());
            invoice.setPurchaseOrder(grc.getGrnHeader().getPurchaseOrder());
            invoice.setInvoiceNumber("INV-AUTO-" + grc.getId());
            invoice.setInvoiceDate(java.time.LocalDate.now());
            invoice.setMontantTTC(total);
            invoice.setMontantHT(total.divide(java.math.BigDecimal.valueOf(1.20), 2, java.math.RoundingMode.HALF_UP));
            invoice.setStatus(InvoiceStatus.RECEIVED);
            invoiceRepository.save(invoice);
        }
        
        return grcRepository.save(grc);
    }
}

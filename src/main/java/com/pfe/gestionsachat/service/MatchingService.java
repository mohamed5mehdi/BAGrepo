package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchingService {
    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private GrcHeaderRepository grcHeaderRepository;
    @Autowired
    private GrnHeaderRepository grnHeaderRepository;

    @Transactional
    public Invoice matchInvoice(@org.springframework.lang.NonNull Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new RuntimeException("Facture introuvable: " + invoiceId));
        PurchaseOrder po = invoice.getPurchaseOrder();
        GrnHeader grn = invoice.getGrnHeader();

        // Si le PO est manquant, on essaie de le retrouver via le GRN
        if (po == null && grn != null) {
            po = grn.getPurchaseOrder();
            invoice.setPurchaseOrder(po);
        }

        // Si le GRN est manquant, on essaie de le retrouver via le PO
        if (grn == null && po != null && po.getIdPo() != null) {
            grn = grnHeaderRepository.findByPurchaseOrder_IdPo(po.getIdPo()).stream().findFirst().orElse(null);
            invoice.setGrnHeader(grn);
        }

        if (po == null) {
            throw new RuntimeException("Bon de Commande (PO) manquant sur la facture #" + invoiceId);
        }
        if (grn == null) {
            throw new RuntimeException("Bon de Réception (GRN) manquant ou non trouvé pour la facture #" + invoiceId);
        }

        GrcHeader grc = grn.getGrcHeader();
        if (grc == null) {
            grc = grcHeaderRepository.findByGrnHeader(grn).orElse(null);
        }

        if (grc == null) {
            throw new RuntimeException("La valorisation (GRC) est manquante pour le GRN #" + grn.getId() + ". Veuillez valoriser avant de facturer.");
        }

        // Comparaison des montants (PO, GRC, Facture)
        java.math.BigDecimal poAmount = po.getTotalAmount() != null ? po.getTotalAmount() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal grcAmount = grc.getTotalAmount() != null ? grc.getTotalAmount() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal invAmount = invoice.getMontantTTC() != null ? invoice.getMontantTTC() : java.math.BigDecimal.ZERO;

        // Tolérance d'arrondi maximale : 0.01 MAD (norme comptable grande entreprise)
        boolean amountsMatch = poAmount.subtract(grcAmount).abs().compareTo(new java.math.BigDecimal("0.01")) < 0
                && grcAmount.subtract(invAmount).abs().compareTo(new java.math.BigDecimal("0.01")) < 0;

        invoice.setStatus(amountsMatch ? InvoiceStatus.MATCHED : InvoiceStatus.REJECTED);
        invoice.setGrcHeader(grc);

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice approveInvoice(@org.springframework.lang.NonNull Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
        if (invoice.getStatus() != InvoiceStatus.MATCHED) {
            throw new RuntimeException("La facture doit être MATCHED avant approbation (statut actuel : " + invoice.getStatus() + ")");
        }
        invoice.setStatus(InvoiceStatus.APPROVED);
        return invoiceRepository.save(invoice);
    }
}

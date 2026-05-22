package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoice")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @ManyToOne
    @JoinColumn(name = "grn_header_id")
    private GrnHeader grnHeader;

    @ManyToOne
    @JoinColumn(name = "grc_header_id")
    private GrcHeader grcHeader;

    private String invoiceNumber;
    private LocalDate invoiceDate = LocalDate.now();
    private BigDecimal montantHT;
    private BigDecimal montantTTC;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    /**
     * Invariant comptable fondamental : TTC >= HT (TVA >= 0).
     * Un TTC inférieur au HT est mathématiquement impossible.
     * Cette garde empêche toute donnée corrompue d'être persistée en base,
     * quelle que soit l'origine de l'appel (service, API, retry).
     */
    @PrePersist
    @PreUpdate
    private void validateMontants() {
        if (montantHT != null && montantTTC != null) {
            if (montantTTC.compareTo(montantHT) < 0) {
                throw new IllegalStateException(
                    "Invariant comptable violé sur la facture [" + invoiceNumber + "] : " +
                    "montantTTC (" + montantTTC + ") < montantHT (" + montantHT + "). " +
                    "La TVA ne peut pas être négative."
                );
            }
        }
        if (montantHT != null && montantHT.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                "Invariant comptable violé : montantHT (" + montantHT + ") ne peut pas être négatif."
            );
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    public GrnHeader getGrnHeader() { return grnHeader; }
    public void setGrnHeader(GrnHeader grnHeader) { this.grnHeader = grnHeader; }
    public GrcHeader getGrcHeader() { return grcHeader; }
    public void setGrcHeader(GrcHeader grcHeader) { this.grcHeader = grcHeader; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public BigDecimal getMontantHT() { return montantHT; }
    public void setMontantHT(BigDecimal montantHT) { this.montantHT = montantHT; }
    public BigDecimal getMontantTTC() { return montantTTC; }
    public void setMontantTTC(BigDecimal montantTTC) { this.montantTTC = montantTTC; }
    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }
}

package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
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

    private String invoiceNumber;
    private LocalDate invoiceDate = LocalDate.now();
    private java.math.BigDecimal montantHT;
    private java.math.BigDecimal montantTTC;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    public GrnHeader getGrnHeader() { return grnHeader; }
    public void setGrnHeader(GrnHeader grnHeader) { this.grnHeader = grnHeader; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public java.math.BigDecimal getMontantHT() { return montantHT; }
    public void setMontantHT(java.math.BigDecimal montantHT) { this.montantHT = montantHT; }
    public java.math.BigDecimal getMontantTTC() { return montantTTC; }
    public void setMontantTTC(java.math.BigDecimal montantTTC) { this.montantTTC = montantTTC; }
    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }
}

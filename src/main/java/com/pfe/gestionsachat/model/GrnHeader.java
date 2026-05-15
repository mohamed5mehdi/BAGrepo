package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "grn_header")
public class GrnHeader {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    private String deliveryNoteNumber;
    private LocalDate receiptDate = LocalDate.now();

    /**
     * Numéro de référence BAG ERP du GRN — partagé avec le GRC associé.
     * Formats : GRN-YYYYMM-XXXXX
     * Règle BAG ERP : GRC = MÊME numéro que son GRN.
     */
    @Column(name = "grn_number", unique = true)
    private String grnNumber;

    @ManyToOne
    @JoinColumn(name = "received_by")
    private User receivedBy;

    @ManyToOne
    @JoinColumn(name = "parent_grn_id")
    private GrnHeader parentGrn;

    @Enumerated(EnumType.STRING)
    private GrnStatus status;

    @OneToMany(mappedBy = "grnHeader", cascade = CascadeType.ALL)
    private List<GrnDetails> details;

    @OneToOne(mappedBy = "grnHeader")
    private GrcHeader grcHeader;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public String getDeliveryNoteNumber() { return deliveryNoteNumber; }
    public void setDeliveryNoteNumber(String deliveryNoteNumber) { this.deliveryNoteNumber = deliveryNoteNumber; }
    public String getGrnNumber() { return grnNumber; }
    public void setGrnNumber(String grnNumber) { this.grnNumber = grnNumber; }
    public LocalDate getReceiptDate() { return receiptDate; }
    public void setReceiptDate(LocalDate receiptDate) { this.receiptDate = receiptDate; }
    public User getReceivedBy() { return receivedBy; }
    public void setReceivedBy(User receivedBy) { this.receivedBy = receivedBy; }
    public GrnHeader getParentGrn() { return parentGrn; }
    public void setParentGrn(GrnHeader parentGrn) { this.parentGrn = parentGrn; }
    public GrnStatus getStatus() { return status; }
    public void setStatus(GrnStatus status) { this.status = status; }
    public List<GrnDetails> getDetails() { return details; }
    public void setDetails(List<GrnDetails> details) { this.details = details; }
    public GrcHeader getGrcHeader() { return grcHeader; }
    public void setGrcHeader(GrcHeader grcHeader) { this.grcHeader = grcHeader; }
}

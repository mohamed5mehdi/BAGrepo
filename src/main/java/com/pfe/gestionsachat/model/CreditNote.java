package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "credit_note")
public class CreditNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "grn_header_id")
    private GrnHeader grnHeader;

    private String creditNoteNumber;
    private java.util.Date creditNoteDate;
    private java.math.BigDecimal montant;

    @Enumerated(EnumType.STRING)
    private CreditNoteStatus status;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public GrnHeader getGrnHeader() { return grnHeader; }
    public void setGrnHeader(GrnHeader grnHeader) { this.grnHeader = grnHeader; }
    public String getCreditNoteNumber() { return creditNoteNumber; }
    public void setCreditNoteNumber(String creditNoteNumber) { this.creditNoteNumber = creditNoteNumber; }
    public Date getCreditNoteDate() { return creditNoteDate; }
    public void setCreditNoteDate(Date creditNoteDate) { this.creditNoteDate = creditNoteDate; }
    public java.math.BigDecimal getMontant() { return montant; }
    public void setMontant(java.math.BigDecimal montant) { this.montant = montant; }
    public CreditNoteStatus getStatus() { return status; }
    public void setStatus(CreditNoteStatus status) { this.status = status; }
}

package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "credit_note")
public class CreditNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_header_id")
    private GrnHeader grnHeader;

    /**
     * BUG-09 FIX : lien direct vers la facture d'origine.
     * Invariant comptable : une note de crédit est TOUJOURS émise en réponse à une facture.
     * Sans ce lien, le montant net payable (Invoice.montantTTC - sum(CreditNote.montant))
     * est incalculable dans le modèle sans jointure externe supplémentaire.
     * nullable = false : une note de crédit sans facture associée est métier impossible.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    /**
     * BUG-09 FIX : lien direct vers le fournisseur.
     * Nécessaire pour les rapports de rapprochement fournisseur sans double jointure.
     * nullable = false : une note de crédit sans fournisseur est métier impossible.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "credit_note_number", unique = true)
    private String creditNoteNumber;

    private LocalDate creditNoteDate = LocalDate.now();

    @Column(precision = 19, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    private CreditNoteStatus status;

    /**
     * BUG-09 FIX : validation de positivité du montant de la note de crédit.
     * Un montant nul ou négatif est métier impossible pour une note de crédit.
     */
    @PrePersist
    @PreUpdate
    private void validateMontant() {
        if (montant != null && montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                "CreditNote [" + creditNoteNumber + "] : montant (" + montant +
                ") doit être strictement positif.");
        }
        if (invoice != null && montant != null) {
            BigDecimal plafond = invoice.getMontantTTC();
            if (plafond != null && montant.compareTo(plafond) > 0) {
                throw new IllegalStateException(
                    "CreditNote [" + creditNoteNumber + "] : montant (" + montant +
                    ") dépasse le montantTTC de la facture associée (" + plafond + ").");
            }
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public GrnHeader getGrnHeader() { return grnHeader; }
    public void setGrnHeader(GrnHeader grnHeader) { this.grnHeader = grnHeader; }
    /** BUG-09 FIX : accès direct à la facture source. */
    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
    /** BUG-09 FIX : accès direct au fournisseur. */
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public String getCreditNoteNumber() { return creditNoteNumber; }
    public void setCreditNoteNumber(String creditNoteNumber) { this.creditNoteNumber = creditNoteNumber; }
    public LocalDate getCreditNoteDate() { return creditNoteDate; }
    public void setCreditNoteDate(LocalDate creditNoteDate) { this.creditNoteDate = creditNoteDate; }
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public CreditNoteStatus getStatus() { return status; }
    public void setStatus(CreditNoteStatus status) { this.status = status; }
}

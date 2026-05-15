package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_order")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_po")
    @com.fasterxml.jackson.annotation.JsonProperty("id_po")
    private Integer idPo;

    @OneToOne
    @JoinColumn(name = "id_da")
    private DaHeader daHeader;

    @OneToOne
    @JoinColumn(name = "id_demande_interne")
    private DemandeAchatInterne demandeInterne;

    @ManyToOne
    @JoinColumn(name = "id_supplier")
    private Supplier fournisseur;

    @Column(name = "date_creation")
    @com.fasterxml.jackson.annotation.JsonProperty("date_creation")
    private LocalDate dateCreation;

    /**
     * Statut typé — remplace l'ancien String libre.
     * Machine à états : DRAFT → PENDING_APPROVAL → APPROVED | REJECTED | SHORT_CLOSED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private POStatus statut;

    @Column(name = "montant_total")
    @com.fasterxml.jackson.annotation.JsonProperty("montant_total")
    private BigDecimal montantTotal;

    /**
     * Numéro de référence BAG ERP (format : PO-YYYYMM-XXXXX).
     * Partagé dans les rapports de rapprochement PO/GRN/GRC.
     */
    @Column(name = "po_number", unique = true)
    private String poNumber;

    /**
     * GRNs associés à ce PO — traçabilité complète pour le reporting.
     */
    @OneToMany(mappedBy = "purchaseOrder", fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<GrnHeader> grns = new ArrayList<>();

    public PurchaseOrder() {
        this.dateCreation = LocalDate.now();
        this.statut = POStatus.DRAFT;
    }

    public PurchaseOrder(DaHeader daHeader, BigDecimal montantTotal) {
        this();
        this.daHeader = daHeader;
        this.montantTotal = montantTotal;
    }

    // ── Getters ────────────────────────────────────────────────
    public Integer getIdPo() { return idPo; }
    public DaHeader getDaHeader() { return daHeader; }
    public LocalDate getDateCreation() { return dateCreation; }
    public POStatus getStatut() { return statut; }
    /** Alias for AI/reporting layers that use getStatut() as String */
    @com.fasterxml.jackson.annotation.JsonGetter("statut")
    public String getStatutAsString() { return statut != null ? statut.name() : null; }
    public BigDecimal getMontantTotal() { return montantTotal; }
    public BigDecimal getTotalAmount() { return montantTotal; }
    public DemandeAchatInterne getDemandeInterne() { return demandeInterne; }
    public Supplier getFournisseur() { return fournisseur; }
    public String getPoNumber() { return poNumber; }
    public List<GrnHeader> getGrns() { return grns; }

    // ── Setters ────────────────────────────────────────────────
    public void setIdPo(Integer idPo) { this.idPo = idPo; }
    public void setDaHeader(DaHeader daHeader) { this.daHeader = daHeader; }
    public void setDemandeInterne(DemandeAchatInterne demandeInterne) { this.demandeInterne = demandeInterne; }
    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }
    public void setStatut(POStatus statut) { this.statut = statut; }
    public void setFournisseur(Supplier fournisseur) { this.fournisseur = fournisseur; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }
    public void setGrns(List<GrnHeader> grns) { this.grns = grns; }
}
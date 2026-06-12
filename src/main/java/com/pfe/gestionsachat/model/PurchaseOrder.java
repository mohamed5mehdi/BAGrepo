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


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_demande_interne")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private DemandeAchatInterne demandeInterne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_supplier")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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


    // ── Getters ────────────────────────────────────────────────
    public Integer getIdPo() { return idPo; }

    public LocalDate getDateCreation() { return dateCreation; }
    /** Retourne le statut typé POStatus — usage interne / logique métier. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public POStatus getStatut() { return statut; }
    /**
     * Sérialisation JSON du statut en String — seule clé exposée : "statut".
     * BUG-04 FIX (symétrique) : un seul getter JSON par champ — évite toute ambiguïté.
     */
    @com.fasterxml.jackson.annotation.JsonGetter("statut")
    public String getStatutAsString() { return statut != null ? statut.name() : null; }
    /**
     * BUG-04 FIX : getTotalAmount() supprimé — il doublonnait getMontantTotal() et produisait
     * deux clés JSON ("montant_total" et "totalAmount") pour la même valeur.
     * Risque éliminé : confusion HT/TTC dans les couches AI ou reporting.
     * Utiliser getMontantTotal() exclusivement.
     */
    public BigDecimal getMontantTotal() { return montantTotal; }
    
    @com.fasterxml.jackson.annotation.JsonIgnore
    public BigDecimal getTotalAmount() { return montantTotal; }
    public DemandeAchatInterne getDemandeInterne() { return demandeInterne; }
    public Supplier getFournisseur() { return fournisseur; }
    public String getPoNumber() { return poNumber; }
    public List<GrnHeader> getGrns() { return grns; }

    // ── Setters ────────────────────────────────────────────────
    public void setIdPo(Integer idPo) { this.idPo = idPo; }

    public void setDemandeInterne(DemandeAchatInterne demandeInterne) { this.demandeInterne = demandeInterne; }
    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }
    public void setStatut(POStatus statut) { this.statut = statut; }
    public void setFournisseur(Supplier fournisseur) { this.fournisseur = fournisseur; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }
    public void setGrns(List<GrnHeader> grns) { this.grns = grns; }
}
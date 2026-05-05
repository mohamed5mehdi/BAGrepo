package com.pfe.gestionsachat.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.math.BigDecimal;

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

    private String statut;

    @Column(name = "montant_total")
    @com.fasterxml.jackson.annotation.JsonProperty("montant_total")
    private BigDecimal montantTotal;

    public PurchaseOrder() {
        this.dateCreation = LocalDate.now();
    }

    public PurchaseOrder(DaHeader daHeader, BigDecimal montantTotal) {
        this();
        this.daHeader = daHeader;
        this.montantTotal = montantTotal;
    }

    // Getters
    public Integer getIdPo() { return idPo; }
    public DaHeader getDaHeader() { return daHeader; }
    public LocalDate getDateCreation() { return dateCreation; }
    public String getStatut() { return statut; }
    public BigDecimal getMontantTotal() { return montantTotal; }
    public BigDecimal getTotalAmount() { return montantTotal; }
    public DemandeAchatInterne getDemandeInterne() { return demandeInterne; }
    public Supplier getFournisseur() { return fournisseur; }

    // Setters
    public void setIdPo(Integer idPo) { this.idPo = idPo; }
    public void setDaHeader(DaHeader daHeader) { this.daHeader = daHeader; }
    public void setDemandeInterne(DemandeAchatInterne demandeInterne) { this.demandeInterne = demandeInterne; }
    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }
    public void setStatut(String statut) { this.statut = statut; }
    public void setFournisseur(Supplier fournisseur) { this.fournisseur = fournisseur; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }
}
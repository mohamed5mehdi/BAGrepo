package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "DaDetails")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class DaDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oid_detail")
    @com.fasterxml.jackson.annotation.JsonProperty("oid_detail")
    private Integer oidDetail;

    @ManyToOne
    @JoinColumn(name = "id_da")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private DaHeader daHeader;

    @ManyToOne
    @JoinColumn(name = "id_sous_famille")
    private SubFamily subFamily;

    private Integer quantite;
    private String itemCode;
    private String itemName;
    private String description;
    private String justification;

    @Column(name = "prix_unitaire")
    @com.fasterxml.jackson.annotation.JsonProperty("prix_unitaire")
    private BigDecimal prixUnitaire;

    @ManyToOne
    @JoinColumn(name = "id_fournisseur", nullable = true)
    private Supplier fournisseur;

    public DaDetails() {}

    public DaDetails(DaHeader daHeader, SubFamily subFamily, Integer quantite,
                     String description, BigDecimal prixUnitaire) {
        this.daHeader = daHeader;
        this.subFamily = subFamily;
        this.quantite = quantite;
        this.description = description;
        this.prixUnitaire = prixUnitaire;
    }

    public BigDecimal getTotalPrice() {
        if (prixUnitaire == null || quantite == null) return BigDecimal.ZERO;
        return prixUnitaire.multiply(BigDecimal.valueOf(quantite));
    }

    // Getters
    public Integer getOidDetail() { return oidDetail; }
    public DaHeader getDaHeader() { return daHeader; }
    public SubFamily getSubFamily() { return subFamily; }
    public Integer getQuantite() { return quantite; }
    public String getDescription() { return description; }
    public String getItemCode() { return itemCode; }
    public String getItemName() { return itemName; }
    public String getJustification() { return justification; }
    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public Supplier getFournisseur() { return fournisseur; }

    // Setters
    public void setOidDetail(Integer oidDetail) { this.oidDetail = oidDetail; }
    public void setDaHeader(DaHeader daHeader) { this.daHeader = daHeader; }
    public void setSubFamily(SubFamily subFamily) { this.subFamily = subFamily; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setDescription(String description) { this.description = description; }
    public void setJustification(String justification) { this.justification = justification; }
    public void setPrixUnitaire(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }
    public void setFournisseur(Supplier fournisseur) { this.fournisseur = fournisseur; }
}
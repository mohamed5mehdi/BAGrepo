package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "offre_fournisseur")
public class OffreFournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "da_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private DemandeAchatInterne da;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier fournisseur;

    private BigDecimal prixPropose;
    
    @Column(length = 500)
    private String conditions;

    private Integer delaiLivraisonOffert; // Optional: specific to this offer

    public OffreFournisseur() {}

    public OffreFournisseur(DemandeAchatInterne da, Supplier fournisseur, BigDecimal prixPropose, String conditions, Integer delai) {
        this.da = da;
        this.fournisseur = fournisseur;
        this.prixPropose = prixPropose;
        this.conditions = conditions;
        this.delaiLivraisonOffert = delai;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DemandeAchatInterne getDa() { return da; }
    public void setDa(DemandeAchatInterne da) { this.da = da; }
    public Supplier getFournisseur() { return fournisseur; }
    public void setFournisseur(Supplier fournisseur) { this.fournisseur = fournisseur; }
    public BigDecimal getPrixPropose() { return prixPropose; }
    public void setPrixPropose(BigDecimal prixPropose) { this.prixPropose = prixPropose; }
    public String getConditions() { return conditions; }
    public void setConditions(String conditions) { this.conditions = conditions; }
    public Integer getDelaiLivraisonOffert() { return delaiLivraisonOffert; }
    public void setDelaiLivraisonOffert(Integer delaiLivraisonOffert) { this.delaiLivraisonOffert = delaiLivraisonOffert; }
}

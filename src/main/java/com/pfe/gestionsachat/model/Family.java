package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

@Entity
@Table(name = "family")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "ignoreUnknown"})
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @com.fasterxml.jackson.annotation.JsonProperty("id")
    @com.fasterxml.jackson.annotation.JsonAlias({"id_family", "familyId", "oid_family"})
    private Integer idFamily;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "categorie")
    private CategorieDemande categorie;

    @Version
    private Long version;

    @com.fasterxml.jackson.annotation.JsonProperty("name")
    @com.fasterxml.jackson.annotation.JsonAlias({"libelle", "label"})
    private String libelle;

    @Column(name = "budget_initial")
    @com.fasterxml.jackson.annotation.JsonProperty("budget_initial")
    private BigDecimal budgetInitial;

    @Column(name = "budget_restant")
    @com.fasterxml.jackson.annotation.JsonProperty("budget_restant")
    private BigDecimal budgetRestant;

    @Column(name = "budget_engage")
    @com.fasterxml.jackson.annotation.JsonProperty("budget_engage")
    private BigDecimal budgetEngage = BigDecimal.ZERO;

    @OneToMany(mappedBy = "family", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<SubFamily> subFamilies = new ArrayList<>();

    public Family() {}

    public Family(String libelle, BigDecimal budgetInitial) {
        this.libelle = libelle;
        this.budgetInitial = budgetInitial;
        this.budgetRestant = budgetInitial;
    }

    public void deductBudget(BigDecimal amount) {
        if (this.budgetRestant != null) {
            this.budgetRestant = this.budgetRestant.subtract(amount);
        }
        this.budgetEngage = (this.budgetEngage != null ? this.budgetEngage : BigDecimal.ZERO).add(amount);
    }

    public void addBudget(BigDecimal amount) {
        if (this.budgetRestant != null) {
            this.budgetRestant = this.budgetRestant.add(amount);
        }
    }

    // Getters
    public Integer getIdFamily() { return idFamily; }
    public String getLibelle() { return libelle; }
    public BigDecimal getBudgetInitial() { return budgetInitial; }
    public BigDecimal getBudgetRestant() { return budgetRestant; }
    public BigDecimal getBudgetEngage() { return budgetEngage; }

    @com.fasterxml.jackson.annotation.JsonProperty("budget_disponible")
    @Transient
    public BigDecimal getBudgetDisponible() {
        return budgetRestant != null ? budgetRestant : BigDecimal.ZERO;
    }
    public List<SubFamily> getSubFamilies() { return subFamilies; }

    // Setters
    public void setIdFamily(Integer idFamily) { this.idFamily = idFamily; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
    public void setBudgetInitial(BigDecimal budgetInitial) { this.budgetInitial = budgetInitial; }
    public void setBudgetRestant(BigDecimal budgetRestant) { this.budgetRestant = budgetRestant; }
    public void setBudgetEngage(BigDecimal budgetEngage) { this.budgetEngage = budgetEngage; }

    public CategorieDemande getCategorie() { return categorie; }
    public void setCategorie(CategorieDemande categorie) { this.categorie = categorie; }

    public void setSubFamilies(List<SubFamily> subFamilies) { this.subFamilies = subFamilies; }
}
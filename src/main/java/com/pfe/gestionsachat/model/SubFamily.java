package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

@Entity
@Table(name = "sub_family")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class SubFamily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @com.fasterxml.jackson.annotation.JsonProperty("id")
    @com.fasterxml.jackson.annotation.JsonAlias({"oid_sub", "id_sous_famille", "subFamilyId", "oidSub"})
    private Integer oidSub;

    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "id_family")
    @JsonIgnore
    private Family family;

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

    @OneToMany(mappedBy = "subFamily")
    @JsonIgnore
    private List<DaDetails> details = new ArrayList<>();

    @OneToMany(mappedBy = "subSource")
    @JsonIgnore
    private List<BudgetTransfer> transfersSource = new ArrayList<>();

    @OneToMany(mappedBy = "subCible")
    @JsonIgnore
    private List<BudgetTransfer> transfersCible = new ArrayList<>();

    public SubFamily() {}

    public SubFamily(String libelle, BigDecimal budgetInitial, Family family) {
        this.libelle = libelle;
        this.budgetInitial = budgetInitial;
        this.budgetRestant = budgetInitial;
        this.family = family;
    }

    public boolean hasEnoughBudget(BigDecimal amount) {
        return budgetRestant != null && budgetRestant.compareTo(amount) >= 0;
    }

    public void deductBudget(BigDecimal amount) {
        if (this.budgetRestant != null) {
            this.budgetRestant = this.budgetRestant.subtract(amount);
        }
    }

    // Getters
    public Integer getOidSub() { return oidSub; }
    public Family getFamily() { return family; }
    public String getLibelle() { return libelle; }
    public BigDecimal getBudgetInitial() { return budgetInitial; }
    public BigDecimal getBudgetRestant() { return budgetRestant; }
    public BigDecimal getBudgetEngage() { return budgetEngage; }

    @com.fasterxml.jackson.annotation.JsonProperty("budget_disponible")
    @Transient
    public BigDecimal getBudgetDisponible() {
        if (budgetRestant == null) return BigDecimal.ZERO;
        return budgetEngage == null ? budgetRestant : budgetRestant.subtract(budgetEngage);
    }
    public List<DaDetails> getDetails() { return details; }
    public List<BudgetTransfer> getTransfersSource() { return transfersSource; }
    public List<BudgetTransfer> getTransfersCible() { return transfersCible; }

    // Setters
    public void setOidSub(Integer oidSub) { this.oidSub = oidSub; }
    public void setFamily(Family family) { this.family = family; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
    public void setBudgetInitial(BigDecimal budgetInitial) { this.budgetInitial = budgetInitial; }
    public void setBudgetRestant(BigDecimal budgetRestant) { this.budgetRestant = budgetRestant; }
    public void setBudgetEngage(BigDecimal budgetEngage) { this.budgetEngage = budgetEngage; }
    public void setDetails(List<DaDetails> details) { this.details = details; }
    public void setTransfersSource(List<BudgetTransfer> transfersSource) { this.transfersSource = transfersSource; }
    public void setTransfersCible(List<BudgetTransfer> transfersCible) { this.transfersCible = transfersCible; }
}
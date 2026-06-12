package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

@Entity
@Table(name = "sub_family")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "ignoreUnknown"})
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



    public SubFamily() {}

    public SubFamily(String libelle, BigDecimal budgetInitial, Family family) {
        this.libelle = libelle;
        this.budgetInitial = budgetInitial;
        this.budgetRestant = budgetInitial;
        this.family = family;
    }

    /**
     * BUG-03 FIX : déduction stricte — élimine le silence quand budgetRestant est null.
     * Avant : si budgetRestant == null, la déduction était ignorée MAIS budgetEngage était incrémenté.
     * Invariant gardé : budget_initial = budget_engage + budget_restant.
     * Garde pré-écriture : exception métier avant soustraction si budget insuffisant.
     */
    public void deductBudget(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                "Montant à déduire invalide pour la sous-famille '" + libelle + "' : " + amount);
        }
        if (this.budgetRestant == null) {
            throw new IllegalStateException(
                "Impossible de déduire sur la sous-famille '" + libelle +
                "' : budgetRestant non initialisé. Vérifier la configuration du budget.");
        }
        if (this.budgetRestant.compareTo(amount) < 0) {
            throw new IllegalStateException(
                "Budget insuffisant sur la sous-famille '" + libelle +
                "' : montant demandé (" + amount +
                ") > budget_restant (" + this.budgetRestant + ").");
        }
        this.budgetRestant = this.budgetRestant.subtract(amount);
        this.budgetEngage  = (this.budgetEngage != null ? this.budgetEngage : BigDecimal.ZERO).add(amount);
    }

    /**
     * BUG-03 FIX : restitution stricte — même logique que BUG-02 sur BudgetPieces.
     * Garde sur-restitution : budget_restant + amount <= budget_initial.
     */
    public void addBudget(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                "Montant à restituer invalide pour la sous-famille '" + libelle + "' : " + amount);
        }
        if (this.budgetRestant == null) {
            throw new IllegalStateException(
                "Impossible de restituer sur la sous-famille '" + libelle +
                "' : budgetRestant non initialisé.");
        }
        BigDecimal restantApres = this.budgetRestant.add(amount);
        if (this.budgetInitial != null && restantApres.compareTo(this.budgetInitial) > 0) {
            throw new IllegalStateException(
                "Sur-restitution détectée sur la sous-famille '" + libelle +
                "' : budget_restant après restitution (" + restantApres +
                ") dépasserait budget_initial (" + budgetInitial + ").");
        }
        this.budgetRestant = restantApres;
        if (this.budgetEngage != null && this.budgetEngage.compareTo(amount) >= 0) {
            this.budgetEngage = this.budgetEngage.subtract(amount);
        } else {
            this.budgetEngage = BigDecimal.ZERO;
        }
    }

    public boolean hasEnoughBudget(BigDecimal amount) {
        return amount != null && this.budgetRestant != null && this.budgetRestant.compareTo(amount) >= 0;
    }

    /**
     * BUG-03 FIX : borne inférieure ET supérieure vérifiées.
     */
    @PrePersist
    @PreUpdate
    private void checkBudgetIntegrity() {
        if (budgetRestant != null && budgetRestant.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                "Intégrité compromise: le budget_restant de la sous-famille '" + libelle + "' ne peut pas être négatif.");
        }
        if (budgetInitial != null && budgetRestant != null
                && budgetRestant.compareTo(budgetInitial) > 0) {
            throw new IllegalStateException(
                "Intégrité compromise: budget_restant (" + budgetRestant +
                ") > budget_initial (" + budgetInitial +
                ") pour la sous-famille '" + libelle + "'. Sur-restitution non bloquée.");
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
        return budgetRestant != null ? budgetRestant : BigDecimal.ZERO;
    }


    // Setters
    public void setOidSub(Integer oidSub) { this.oidSub = oidSub; }
    public void setFamily(Family family) { this.family = family; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
    public void setBudgetInitial(BigDecimal budgetInitial) { this.budgetInitial = budgetInitial; }
    public void setBudgetRestant(BigDecimal budgetRestant) { this.budgetRestant = budgetRestant; }
    public void setBudgetEngage(BigDecimal budgetEngage) { this.budgetEngage = budgetEngage; }

}
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Pool budgétaire global dédié exclusivement aux pièces de rechange (isPieceRechange = true).
 * Totalement étanche du circuit Famille/Sous-Famille.
 *
 * Invariant fondamental : budget_initial = budget_engage + budget_restant
 * Alimenté une seule fois en début d'exercice par le Financier/DAF.
 */
@Entity
@Table(name = "budget_pieces")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BudgetPieces {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Identifiant de l'exercice comptable. Format : "YYYY".
     * Contrainte d'unicité : un seul pool par exercice.
     */
    @Column(name = "exercice", nullable = false, unique = true, length = 4)
    private String exercice;

    @Column(name = "budget_initial", nullable = false, precision = 19, scale = 2)
    private BigDecimal budgetInitial;

    @Column(name = "budget_engage", nullable = false, precision = 19, scale = 2)
    private BigDecimal budgetEngage = BigDecimal.ZERO;

    @Column(name = "budget_restant", nullable = false, precision = 19, scale = 2)
    private BigDecimal budgetRestant;

    /**
     * Verrou optimiste pour la gestion de la concurrence.
     * Si deux transactions tentent de modifier le pool simultanément,
     * l'une sera rejetée avec ObjectOptimisticLockingFailureException.
     */
    @Version
    private Long version;

    public BudgetPieces() {}

    public BudgetPieces(String exercice, BigDecimal budgetInitial) {
        this.exercice      = exercice;
        this.budgetInitial = budgetInitial;
        this.budgetRestant = budgetInitial;
        this.budgetEngage  = BigDecimal.ZERO;
    }

    // ── Méthodes métier ───────────────────────────────────────────────────────

    /**
     * Impute un montant sur le pool pièces.
     * budget_engage  += montant
     * budget_restant -= montant
     */
    public void deductBudget(BigDecimal montant) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montant à déduire invalide : " + montant);
        }
        this.budgetRestant = orZero(this.budgetRestant).subtract(montant);
        this.budgetEngage  = orZero(this.budgetEngage).add(montant);
    }

    /**
     * Restitue un montant au pool pièces (annulation / rejet).
     * budget_engage  -= montant
     * budget_restant += montant
     */
    public void addBudget(BigDecimal montant) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montant à restituer invalide : " + montant);
        }
        this.budgetRestant = orZero(this.budgetRestant).add(montant);
        BigDecimal engage  = orZero(this.budgetEngage);
        this.budgetEngage  = engage.compareTo(montant) >= 0
            ? engage.subtract(montant)
            : BigDecimal.ZERO;
    }

    /**
     * Vérifie que le pool dispose d'un budget suffisant.
     */
    public boolean hasEnoughBudget(BigDecimal montant) {
        return montant != null
            && orZero(this.budgetRestant).compareTo(montant) >= 0;
    }

    @PrePersist
    @PreUpdate
    private void checkIntegrite() {
        if (budgetRestant != null && budgetRestant.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                "Intégrité rompue : le budget_restant du pool Pièces (exercice=" + exercice + ") est négatif.");
        }
    }

    private static BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Integer getId()                          { return id; }
    public void setId(Integer id)                   { this.id = id; }

    public String getExercice()                     { return exercice; }
    public void setExercice(String exercice)         { this.exercice = exercice; }

    public BigDecimal getBudgetInitial()             { return budgetInitial; }
    public void setBudgetInitial(BigDecimal v)       { this.budgetInitial = v; }

    public BigDecimal getBudgetEngage()              { return budgetEngage; }
    public void setBudgetEngage(BigDecimal v)        { this.budgetEngage = v; }

    public BigDecimal getBudgetRestant()             { return budgetRestant; }
    public void setBudgetRestant(BigDecimal v)       { this.budgetRestant = v; }

    public Long getVersion()                         { return version; }
    public void setVersion(Long version)             { this.version = version; }
}

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
     *
     * Garde pré-écriture : lève une exception métier si le budget est insuffisant,
     * avant toute modification des champs — évite le rollback silencieux via @PreUpdate.
     */
    public void deductBudget(BigDecimal montant) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montant à déduire invalide : " + montant);
        }
        if (!hasEnoughBudget(montant)) {
            throw new IllegalStateException(
                "Budget insuffisant sur le pool Pièces (exercice=" + exercice +
                ") : montant demandé (" + montant +
                ") > budget_restant (" + orZero(this.budgetRestant) + ").");
        }
        this.budgetRestant = orZero(this.budgetRestant).subtract(montant);
        this.budgetEngage  = orZero(this.budgetEngage).add(montant);
    }

    /**
     * Restitue un montant au pool pièces (annulation / rejet).
     * budget_engage  -= montant
     * budget_restant += montant
     *
     * BUG-02 FIX : garde sur-restitution — interdit budget_restant > budget_initial.
     * Invariant protégé : budget_initial = budget_engage + budget_restant.
     * Si addBudget() est appelé deux fois pour la même DA (double restitution),
     * la deuxième levée une IllegalStateException avant toute écriture.
     */
    public void addBudget(BigDecimal montant) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montant à restituer invalide : " + montant);
        }
        BigDecimal restantApres = orZero(this.budgetRestant).add(montant);
        if (this.budgetInitial != null && restantApres.compareTo(this.budgetInitial) > 0) {
            throw new IllegalStateException(
                "Sur-restitution détectée sur le pool Pièces (exercice=" + exercice +
                ") : budget_restant après restitution (" + restantApres +
                ") dépasserait budget_initial (" + budgetInitial + "). " +
                "Double restitution probable — vérifier le flag budgetRestitue.");
        }
        this.budgetRestant = restantApres;
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

    /**
     * BUG-02 FIX : vérification renfroce des deux bornes de l'invariant.
     * Borne inférieure : budget_restant >= 0 (déjà présent).
     * Borne supérieure : budget_restant <= budget_initial (ajoutée).
     * Les deux violations sont des corruptions comptables.
     */
    @PrePersist
    @PreUpdate
    private void checkIntegrite() {
        if (budgetRestant != null && budgetRestant.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                "Intégrité rompue : le budget_restant du pool Pièces (exercice=" + exercice + ") est négatif.");
        }
        if (budgetInitial != null && budgetRestant != null
                && budgetRestant.compareTo(budgetInitial) > 0) {
            throw new IllegalStateException(
                "Intégrité rompue : budget_restant (" + budgetRestant +
                ") > budget_initial (" + budgetInitial +
                ") sur le pool Pièces (exercice=" + exercice + "). Sur-restitution non bloquée.");
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

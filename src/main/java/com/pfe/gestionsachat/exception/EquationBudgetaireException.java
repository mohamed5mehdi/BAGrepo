package com.pfe.gestionsachat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception métier levée quand l'équation fondamentale
 * budget_initial = budget_engage + budget_restant est violée.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class EquationBudgetaireException extends RuntimeException {

    private final String entite;
    private final Object entiteId;
    private final java.math.BigDecimal budgetInitial;
    private final java.math.BigDecimal budgetEngage;
    private final java.math.BigDecimal budgetRestant;

    public EquationBudgetaireException(String entite, Object entiteId,
                                       java.math.BigDecimal budgetInitial,
                                       java.math.BigDecimal budgetEngage,
                                       java.math.BigDecimal budgetRestant) {
        super(String.format(
            "RUPTURE EQUATION BUDGETAIRE [%s id=%s] : " +
            "budget_initial=%.2f ≠ budget_engage=%.2f + budget_restant=%.2f (somme=%.2f)",
            entite, entiteId, budgetInitial, budgetEngage, budgetRestant,
            budgetEngage.add(budgetRestant)));
        this.entite       = entite;
        this.entiteId     = entiteId;
        this.budgetInitial = budgetInitial;
        this.budgetEngage  = budgetEngage;
        this.budgetRestant = budgetRestant;
    }

    public String getEntite()                          { return entite; }
    public Object getEntiteId()                        { return entiteId; }
    public java.math.BigDecimal getBudgetInitial()     { return budgetInitial; }
    public java.math.BigDecimal getBudgetEngage()      { return budgetEngage; }
    public java.math.BigDecimal getBudgetRestant()     { return budgetRestant; }
}

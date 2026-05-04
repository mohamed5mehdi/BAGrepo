package com.pfe.gestionsachat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception métier levée quand le budget_restant d'une sous-famille
 * est insuffisant pour couvrir le montant demandé.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class BudgetInsuffisantException extends RuntimeException {

    private final Integer sousFamilleId;
    private final java.math.BigDecimal budgetRestant;
    private final java.math.BigDecimal montantDemande;

    public BudgetInsuffisantException(Integer sousFamilleId,
                                      java.math.BigDecimal budgetRestant,
                                      java.math.BigDecimal montantDemande) {
        super(String.format(
            "Budget insuffisant pour la sous-famille [id=%d] : budget_restant=%.2f < montant_demande=%.2f",
            sousFamilleId, budgetRestant, montantDemande));
        this.sousFamilleId = sousFamilleId;
        this.budgetRestant = budgetRestant;
        this.montantDemande = montantDemande;
    }

    public Integer getSousFamilleId()       { return sousFamilleId; }
    public java.math.BigDecimal getBudgetRestant()  { return budgetRestant; }
    public java.math.BigDecimal getMontantDemande() { return montantDemande; }
}

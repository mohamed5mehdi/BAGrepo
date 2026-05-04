package com.pfe.gestionsachat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Réponse après imputation d'une DA sur le budget.
 */
public class ConsommerBudgetResponse {

    @JsonProperty("da_id")
    private Integer daId;

    @JsonProperty("sous_famille_id")
    private Integer sousFamilleId;

    @JsonProperty("libelle_sous_famille")
    private String libelleSousFamille;

    @JsonProperty("montant_impute")
    private BigDecimal montantImpute;

    @JsonProperty("budget_engage_avant")
    private BigDecimal budgetEngageAvant;

    @JsonProperty("budget_restant_avant")
    private BigDecimal budgetRestantAvant;

    @JsonProperty("budget_engage_apres")
    private BigDecimal budgetEngageApres;

    @JsonProperty("budget_restant_apres")
    private BigDecimal budgetRestantApres;

    @JsonProperty("date_imputation")
    private LocalDateTime dateImputation;

    @JsonProperty("equation_validee")
    private boolean equationValidee;

    public ConsommerBudgetResponse() {
        this.dateImputation = LocalDateTime.now();
        this.equationValidee = true;
    }

    // ─── Getters / Setters ─────────────────────────────────────────────────────

    @JsonProperty("da_id")
    public Integer getDaId()                        { return daId; }
    public void setDaId(Integer v)                  { this.daId = v; }

    @JsonProperty("sous_famille_id")
    public Integer getSousFamilleId()               { return sousFamilleId; }
    public void setSousFamilleId(Integer v)         { this.sousFamilleId = v; }

    @JsonProperty("libelle_sous_famille")
    public String getLibelleSousFamille()           { return libelleSousFamille; }
    public void setLibelleSousFamille(String v)     { this.libelleSousFamille = v; }

    @JsonProperty("montant_impute")
    public BigDecimal getMontantImpute()            { return montantImpute; }
    public void setMontantImpute(BigDecimal v)      { this.montantImpute = v; }

    @JsonProperty("budget_engage_avant")
    public BigDecimal getBudgetEngageAvant()        { return budgetEngageAvant; }
    public void setBudgetEngageAvant(BigDecimal v)  { this.budgetEngageAvant = v; }

    @JsonProperty("budget_restant_avant")
    public BigDecimal getBudgetRestantAvant()       { return budgetRestantAvant; }
    public void setBudgetRestantAvant(BigDecimal v) { this.budgetRestantAvant = v; }

    @JsonProperty("budget_engage_apres")
    public BigDecimal getBudgetEngageApres()        { return budgetEngageApres; }
    public void setBudgetEngageApres(BigDecimal v)  { this.budgetEngageApres = v; }

    @JsonProperty("budget_restant_apres")
    public BigDecimal getBudgetRestantApres()       { return budgetRestantApres; }
    public void setBudgetRestantApres(BigDecimal v) { this.budgetRestantApres = v; }

    @JsonProperty("date_imputation")
    public LocalDateTime getDateImputation()        { return dateImputation; }
    public void setDateImputation(LocalDateTime v)  { this.dateImputation = v; }

    @JsonProperty("equation_validee")
    public boolean isEquationValidee()              { return equationValidee; }
    public void setEquationValidee(boolean v)       { this.equationValidee = v; }
}

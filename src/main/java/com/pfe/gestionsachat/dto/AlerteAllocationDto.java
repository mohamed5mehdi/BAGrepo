package com.pfe.gestionsachat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * DTO d'alerte d'allocation budgétaire.
 * Levé lorsque Σ SubFamily.budgetInitial ≠ Family.budgetInitial.
 *
 * Retourné par GET /api/budget/allocation/verification
 * et GET /api/budget/allocation/verification/{familleId}
 *
 * NON bloquant : la sauvegarde n'est pas rejetée, l'alerte est uniquement
 * affichée en rouge dans l'interface pour que le responsable corrige.
 */
public class AlerteAllocationDto {

    @JsonProperty("famille_id")
    private Integer familleId;

    @JsonProperty("libelle_famille")
    private String libelleFamille;

    @JsonProperty("budget_initial_famille")
    private BigDecimal budgetInitialFamille;

    @JsonProperty("somme_initial_sous_familles")
    private BigDecimal sommeInitialSousFamilles;

    /**
     * ecart = Family.budgetInitial - Σ SF.budgetInitial
     * > 0 : budget famille sous-alloué (il reste X DZD non distribués)
     * < 0 : budget famille sur-alloué  (les SF dépassent l'enveloppe)
     * = 0 : équation respectée
     */
    private BigDecimal ecart;

    /**
     * "OK"       : |ecart| = 0
     * "SOUS_ALLOUE" : ecart > 0
     * "SUR_ALLOUE"  : ecart < 0  (le plus grave — création monétaire)
     */
    private String niveau;

    public AlerteAllocationDto() {}

    public AlerteAllocationDto(Integer familleId, String libelleFamille,
                                BigDecimal budgetInitialFamille,
                                BigDecimal sommeInitialSousFamilles) {
        this.familleId               = familleId;
        this.libelleFamille          = libelleFamille;
        this.budgetInitialFamille    = budgetInitialFamille    != null ? budgetInitialFamille    : BigDecimal.ZERO;
        this.sommeInitialSousFamilles = sommeInitialSousFamilles != null ? sommeInitialSousFamilles : BigDecimal.ZERO;
        this.ecart = this.budgetInitialFamille.subtract(this.sommeInitialSousFamilles);

        if (this.ecart.compareTo(BigDecimal.ZERO) == 0) {
            this.niveau = "OK";
        } else if (this.ecart.compareTo(BigDecimal.ZERO) > 0) {
            this.niveau = "SOUS_ALLOUE";
        } else {
            this.niveau = "SUR_ALLOUE";
        }
    }

    @JsonProperty("famille_id")
    public Integer getFamilleId()                         { return familleId; }
    public void setFamilleId(Integer v)                   { this.familleId = v; }

    @JsonProperty("libelle_famille")
    public String getLibelleFamille()                     { return libelleFamille; }
    public void setLibelleFamille(String v)               { this.libelleFamille = v; }

    @JsonProperty("budget_initial_famille")
    public BigDecimal getBudgetInitialFamille()           { return budgetInitialFamille; }
    public void setBudgetInitialFamille(BigDecimal v)     { this.budgetInitialFamille = v; }

    @JsonProperty("somme_initial_sous_familles")
    public BigDecimal getSommeInitialSousFamilles()       { return sommeInitialSousFamilles; }
    public void setSommeInitialSousFamilles(BigDecimal v) { this.sommeInitialSousFamilles = v; }

    public BigDecimal getEcart()                          { return ecart; }
    public void setEcart(BigDecimal v)                    { this.ecart = v; }

    public String getNiveau()                             { return niveau; }
    public void setNiveau(String v)                       { this.niveau = v; }
}

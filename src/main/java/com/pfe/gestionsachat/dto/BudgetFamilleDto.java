package com.pfe.gestionsachat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * DTO de suivi budgétaire d'une famille avec les alias métier :
 *   opening_val  = budget_initial
 *   consumed_val = budget_engage
 *   current_val  = budget_restant
 */
public class BudgetFamilleDto {

    private Integer id;
    private String libelle;

    @JsonProperty("opening_val")
    private BigDecimal openingVal;

    @JsonProperty("consumed_val")
    private BigDecimal consumedVal;

    @JsonProperty("current_val")
    private BigDecimal currentVal;

    @JsonProperty("taux_consommation")
    private BigDecimal tauxConsommation;

    @JsonProperty("sous_familles")
    private List<BudgetSousFamilleDto> sousFamilles;

    // ─── Constructeur ──────────────────────────────────────────────────────────

    public BudgetFamilleDto() {}

    public BudgetFamilleDto(Integer id, String libelle,
                             BigDecimal openingVal,
                             BigDecimal consumedVal,
                             BigDecimal currentVal,
                             List<BudgetSousFamilleDto> sousFamilles) {
        this.id           = id;
        this.libelle      = libelle;
        this.openingVal   = openingVal  != null ? openingVal  : BigDecimal.ZERO;
        this.consumedVal  = consumedVal != null ? consumedVal : BigDecimal.ZERO;
        this.currentVal   = currentVal  != null ? currentVal  : BigDecimal.ZERO;
        this.sousFamilles = sousFamilles;
        this.tauxConsommation = calculerTaux(this.consumedVal, this.openingVal);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    public static BigDecimal calculerTaux(BigDecimal consomme, BigDecimal initial) {
        if (initial == null || initial.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return consomme.multiply(BigDecimal.valueOf(100))
                       .divide(initial, 2, RoundingMode.HALF_UP);
    }

    // ─── Getters / Setters ─────────────────────────────────────────────────────

    public Integer getId()                            { return id; }
    public void setId(Integer id)                     { this.id = id; }

    public String getLibelle()                        { return libelle; }
    public void setLibelle(String libelle)            { this.libelle = libelle; }

    @JsonProperty("opening_val")
    public BigDecimal getOpeningVal()                 { return openingVal; }
    public void setOpeningVal(BigDecimal v)           { this.openingVal = v; }

    @JsonProperty("consumed_val")
    public BigDecimal getConsumedVal()                { return consumedVal; }
    public void setConsumedVal(BigDecimal v)          { this.consumedVal = v; }

    @JsonProperty("current_val")
    public BigDecimal getCurrentVal()                 { return currentVal; }
    public void setCurrentVal(BigDecimal v)           { this.currentVal = v; }

    @JsonProperty("taux_consommation")
    public BigDecimal getTauxConsommation()           { return tauxConsommation; }
    public void setTauxConsommation(BigDecimal v)     { this.tauxConsommation = v; }

    @JsonProperty("sous_familles")
    public List<BudgetSousFamilleDto> getSousFamilles()             { return sousFamilles; }
    public void setSousFamilles(List<BudgetSousFamilleDto> list)    { this.sousFamilles = list; }
}

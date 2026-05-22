package com.pfe.gestionsachat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DTO représentant l'état du pool budgétaire global dédié aux pièces de rechange.
 * Retourné par GET /api/budget/pieces
 */
public class BudgetPiecesDto {

    private Integer id;
    private String exercice;

    @JsonProperty("opening_val")
    private BigDecimal openingVal;

    @JsonProperty("consumed_val")
    private BigDecimal consumedVal;

    @JsonProperty("current_val")
    private BigDecimal currentVal;

    @JsonProperty("taux_consommation")
    private BigDecimal tauxConsommation;

    public BudgetPiecesDto() {}

    public BudgetPiecesDto(Integer id, String exercice,
                            BigDecimal openingVal,
                            BigDecimal consumedVal,
                            BigDecimal currentVal) {
        this.id          = id;
        this.exercice    = exercice;
        this.openingVal  = openingVal  != null ? openingVal  : BigDecimal.ZERO;
        this.consumedVal = consumedVal != null ? consumedVal : BigDecimal.ZERO;
        this.currentVal  = currentVal  != null ? currentVal  : BigDecimal.ZERO;
        this.tauxConsommation = calculerTaux(this.consumedVal, this.openingVal);
    }

    private static BigDecimal calculerTaux(BigDecimal consomme, BigDecimal initial) {
        if (initial == null || initial.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return consomme.multiply(BigDecimal.valueOf(100))
                       .divide(initial, 2, RoundingMode.HALF_UP);
    }

    public Integer getId()                         { return id; }
    public void setId(Integer id)                  { this.id = id; }

    public String getExercice()                    { return exercice; }
    public void setExercice(String exercice)        { this.exercice = exercice; }

    @JsonProperty("opening_val")
    public BigDecimal getOpeningVal()              { return openingVal; }
    public void setOpeningVal(BigDecimal v)        { this.openingVal = v; }

    @JsonProperty("consumed_val")
    public BigDecimal getConsumedVal()             { return consumedVal; }
    public void setConsumedVal(BigDecimal v)       { this.consumedVal = v; }

    @JsonProperty("current_val")
    public BigDecimal getCurrentVal()              { return currentVal; }
    public void setCurrentVal(BigDecimal v)        { this.currentVal = v; }

    @JsonProperty("taux_consommation")
    public BigDecimal getTauxConsommation()        { return tauxConsommation; }
    public void setTauxConsommation(BigDecimal v)  { this.tauxConsommation = v; }
}

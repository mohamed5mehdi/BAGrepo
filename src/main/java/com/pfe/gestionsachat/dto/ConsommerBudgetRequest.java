package com.pfe.gestionsachat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Corps de la requête POST /api/budget/consommer.
 * Impute une DA validée sur une sous-famille.
 */
public class ConsommerBudgetRequest {

    @NotNull(message = "L'identifiant de la DA est obligatoire")
    @JsonProperty("demande_interne_id")
    private Long demandeInterneId;

    @NotNull(message = "L'identifiant de la sous-famille est obligatoire")
    @JsonProperty("sous_famille_id")
    private Integer sousFamilleId;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être positif")
    private BigDecimal montant;

    public ConsommerBudgetRequest() {}

    public ConsommerBudgetRequest(Long demandeInterneId, Integer sousFamilleId, BigDecimal montant) {
        this.demandeInterneId         = demandeInterneId;
        this.sousFamilleId = sousFamilleId;
        this.montant      = montant;
    }

    @JsonProperty("demande_interne_id")
    public Long getDemandeInterneId()              { return demandeInterneId; }
    public void setDemandeInterneId(Long v)        { this.demandeInterneId = v; }

    @JsonProperty("sous_famille_id")
    public Integer getSousFamilleId()     { return sousFamilleId; }
    public void setSousFamilleId(Integer v){ this.sousFamilleId = v; }

    public BigDecimal getMontant()        { return montant; }
    public void setMontant(BigDecimal v)  { this.montant = v; }
}

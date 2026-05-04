package com.pfe.gestionsachat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * DTO d'alerte budgétaire : entité dont le taux de consommation dépasse le seuil.
 */
public class AlerteBudgetaireDto {

    @JsonProperty("type_entite")
    private String typeEntite;   // "FAMILLE" ou "SOUS_FAMILLE"

    private Integer id;
    private String libelle;

    @JsonProperty("id_famille")
    private Integer idFamille;   // null si typeEntite == "FAMILLE"

    @JsonProperty("opening_val")
    private BigDecimal openingVal;

    @JsonProperty("consumed_val")
    private BigDecimal consumedVal;

    @JsonProperty("current_val")
    private BigDecimal currentVal;

    @JsonProperty("taux_consommation")
    private BigDecimal tauxConsommation;

    @JsonProperty("seuil_alerte")
    private BigDecimal seuilAlerte;

    public AlerteBudgetaireDto() {}

    public AlerteBudgetaireDto(String typeEntite, Integer id, String libelle,
                                Integer idFamille,
                                BigDecimal openingVal, BigDecimal consumedVal,
                                BigDecimal currentVal, BigDecimal tauxConsommation,
                                BigDecimal seuilAlerte) {
        this.typeEntite       = typeEntite;
        this.id               = id;
        this.libelle          = libelle;
        this.idFamille        = idFamille;
        this.openingVal       = openingVal;
        this.consumedVal      = consumedVal;
        this.currentVal       = currentVal;
        this.tauxConsommation = tauxConsommation;
        this.seuilAlerte      = seuilAlerte;
    }

    // ─── Getters / Setters ─────────────────────────────────────────────────────

    @JsonProperty("type_entite")
    public String getTypeEntite()               { return typeEntite; }
    public void setTypeEntite(String v)         { this.typeEntite = v; }

    public Integer getId()                      { return id; }
    public void setId(Integer id)               { this.id = id; }

    public String getLibelle()                  { return libelle; }
    public void setLibelle(String v)            { this.libelle = v; }

    @JsonProperty("id_famille")
    public Integer getIdFamille()               { return idFamille; }
    public void setIdFamille(Integer v)         { this.idFamille = v; }

    @JsonProperty("opening_val")
    public BigDecimal getOpeningVal()           { return openingVal; }
    public void setOpeningVal(BigDecimal v)     { this.openingVal = v; }

    @JsonProperty("consumed_val")
    public BigDecimal getConsumedVal()          { return consumedVal; }
    public void setConsumedVal(BigDecimal v)    { this.consumedVal = v; }

    @JsonProperty("current_val")
    public BigDecimal getCurrentVal()           { return currentVal; }
    public void setCurrentVal(BigDecimal v)     { this.currentVal = v; }

    @JsonProperty("taux_consommation")
    public BigDecimal getTauxConsommation()     { return tauxConsommation; }
    public void setTauxConsommation(BigDecimal v){ this.tauxConsommation = v; }

    @JsonProperty("seuil_alerte")
    public BigDecimal getSeuilAlerte()          { return seuilAlerte; }
    public void setSeuilAlerte(BigDecimal v)    { this.seuilAlerte = v; }
}

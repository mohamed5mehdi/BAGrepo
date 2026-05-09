package com.pfe.gestionsachat.chatbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pfe.gestionsachat.model.UrgenceDemande;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SlotState {

    private String designation;
    private Integer quantite;
    private String justification;
    private UrgenceDemande urgence;
    private Integer familyId;
    private Integer subFamilyId;
    private String familyLibelle;
    private String subFamilyLibelle;

    // ─── Constructeur vide obligatoire pour Jackson ──────────────────────────
    public SlotState() {
    }

    // ─── Méthodes métier ─────────────────────────────────────────────────────

    /**
     * Retourne true si tous les slots obligatoires sont renseignés.
     * Ordre de vérification : designation → quantite → familyId
     *                         → subFamilyId → justification → urgence
     */
    public boolean isComplet() {
        return designation != null && !designation.isBlank()
                && quantite != null
                && justification != null && !justification.isBlank()
                && urgence != null
                && familyId != null
                && subFamilyId != null;
    }

    /**
     * Retourne le prochain slot manquant selon l'ordre de priorité strict.
     * Retourne "COMPLET" si tous les slots sont renseignés.
     */
    public String getProchainSlotManquant() {
        if (designation == null || designation.isBlank()) return "DESIGNATION";
        if (quantite == null)                               return "QUANTITE";
        if (familyId == null)                               return "FAMILLE";
        if (subFamilyId == null)                            return "SOUS_FAMILLE";
        if (justification == null || justification.isBlank()) return "JUSTIFICATION";
        if (urgence == null)                                return "URGENCE";
        return "COMPLET";
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public UrgenceDemande getUrgence() { return urgence; }
    public void setUrgence(UrgenceDemande urgence) { this.urgence = urgence; }

    public Integer getFamilyId() { return familyId; }
    public void setFamilyId(Integer familyId) { this.familyId = familyId; }

    public Integer getSubFamilyId() { return subFamilyId; }
    public void setSubFamilyId(Integer subFamilyId) { this.subFamilyId = subFamilyId; }

    public String getFamilyLibelle() { return familyLibelle; }
    public void setFamilyLibelle(String familyLibelle) { this.familyLibelle = familyLibelle; }

    public String getSubFamilyLibelle() { return subFamilyLibelle; }
    public void setSubFamilyLibelle(String subFamilyLibelle) { this.subFamilyLibelle = subFamilyLibelle; }
}

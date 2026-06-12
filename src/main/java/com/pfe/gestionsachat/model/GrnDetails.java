package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "grn_details")
public class GrnDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "grn_header_id")
    @JsonIgnore
    private GrnHeader grnHeader;

    private String itemCode;
    private String itemName;
    private Integer orderedQuantity;
    /**
     * Shipped Qty — solde restant à recevoir (règle BAG ERP).
     * = orderedQuantity - somme(receivedQuantity de tous les GRNs précédents) - receivedQuantity de CE GRN.
     * Calculé et persisté à la création du GRN. PO clôturé quand shippedQuantity = 0 pour toutes les lignes.
     */
    private Integer shippedQuantity;
    private Integer receivedQuantity;
    private Integer acceptedQuantity;
    private Integer rejectedQuantity;

    @Enumerated(EnumType.STRING)
    private QualityStatus qualityStatus;

    private String notes;

    /**
     * BUG-06 FIX : validation des invariants physiques de réception qualité.
     *
     * Invariants garantis :
     *  1. Toutes les quantités sont >= 0 (pas de valeurs négatives physiquement impossibles).
     *  2. acceptedQuantity + rejectedQuantity == receivedQuantity
     *     → Un GRN où 10 sont reçus, 7 acceptés et 5 rejetés (total=12) est physiquement impossible.
     *  3. receivedQuantity <= orderedQuantity
     *     → On ne peut pas recevoir plus que ce qui a été commandé sur ce GRN.
     *
     * Ces gardes empêchent la corruption silencieuse du stock et les erreurs comptables dans le GRC.
     */
    @PrePersist
    @PreUpdate
    private void validateQuantites() {
        // Invariant 1 : positivité de toutes les quantités
        if (receivedQuantity != null && receivedQuantity < 0) {
            throw new IllegalStateException(
                "GrnDetails [item=" + itemCode + "] : receivedQuantity (" + receivedQuantity + ") ne peut pas être négatif.");
        }
        if (acceptedQuantity != null && acceptedQuantity < 0) {
            throw new IllegalStateException(
                "GrnDetails [item=" + itemCode + "] : acceptedQuantity (" + acceptedQuantity + ") ne peut pas être négatif.");
        }
        if (rejectedQuantity != null && rejectedQuantity < 0) {
            throw new IllegalStateException(
                "GrnDetails [item=" + itemCode + "] : rejectedQuantity (" + rejectedQuantity + ") ne peut pas être négatif.");
        }
        if (shippedQuantity != null && shippedQuantity < 0) {
            throw new IllegalStateException(
                "GrnDetails [item=" + itemCode + "] : shippedQuantity (" + shippedQuantity + ") ne peut pas être négatif.");
        }

        // Invariant 2 : accepted + rejected == received (contrôle qualité exhaustif)
        if (receivedQuantity != null && acceptedQuantity != null && rejectedQuantity != null) {
            int total = acceptedQuantity + rejectedQuantity;
            if (total != receivedQuantity) {
                throw new IllegalStateException(
                    "GrnDetails [item=" + itemCode + "] : invariant qualité violé — " +
                    "acceptedQuantity (" + acceptedQuantity + ") + rejectedQuantity (" + rejectedQuantity +
                    ") = " + total + " ≠ receivedQuantity (" + receivedQuantity + "). " +
                    "Toute la marchandise reçue doit être acceptée ou rejetée.");
            }
        }

        // Invariant 3 : receivedQuantity <= orderedQuantity (sur-réception impossible)
        if (receivedQuantity != null && orderedQuantity != null && receivedQuantity > orderedQuantity) {
            throw new IllegalStateException(
                "GrnDetails [item=" + itemCode + "] : sur-réception détectée — " +
                "receivedQuantity (" + receivedQuantity + ") > orderedQuantity (" + orderedQuantity + "). " +
                "Vérifier les données ou créer un avenant au PO.");
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public GrnHeader getGrnHeader() { return grnHeader; }
    public void setGrnHeader(GrnHeader grnHeader) { this.grnHeader = grnHeader; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public Integer getOrderedQuantity() { return orderedQuantity; }
    public void setOrderedQuantity(Integer orderedQuantity) { this.orderedQuantity = orderedQuantity; }
    public Integer getShippedQuantity() { return shippedQuantity; }
    public void setShippedQuantity(Integer shippedQuantity) { this.shippedQuantity = shippedQuantity; }
    public Integer getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    public Integer getAcceptedQuantity() { return acceptedQuantity; }
    public void setAcceptedQuantity(Integer acceptedQuantity) { this.acceptedQuantity = acceptedQuantity; }
    public Integer getRejectedQuantity() { return rejectedQuantity; }
    public void setRejectedQuantity(Integer rejectedQuantity) { this.rejectedQuantity = rejectedQuantity; }
    public QualityStatus getQualityStatus() { return qualityStatus; }
    public void setQualityStatus(QualityStatus qualityStatus) { this.qualityStatus = qualityStatus; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}


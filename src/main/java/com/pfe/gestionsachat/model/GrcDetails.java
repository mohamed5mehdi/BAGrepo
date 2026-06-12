package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "grc_details")
public class GrcDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "grc_header_id")
    @JsonIgnore
    private GrcHeader grcHeader;

    @OneToOne
    @JoinColumn(name = "grn_detail_id")
    private GrnDetails grnDetail;

    private String itemCode;
    private Integer acceptedQuantity;

    @Column(precision = 19, scale = 4)
    private BigDecimal unitCost;

    /**
     * BUG-07 FIX : totalCost est calculé automatiquement par @PrePersist/@PreUpdate.
     * Formule : totalCost = acceptedQuantity * unitCost
     * Ne jamais setter directement — calculé exclusivement par computeCosts().
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal totalCost;

    @Column(precision = 5, scale = 4)
    private BigDecimal taxRate;

    /**
     * BUG-07 FIX : montantTTC est calculé automatiquement par @PrePersist/@PreUpdate.
     * Formule : montantTTC = totalCost * (1 + taxRate)
     * Ne jamais setter directement — calculé exclusivement par computeCosts().
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal montantTTC;

    private String mainAccount;
    private String subAccount;

    /**
     * BUG-07 FIX : calcul automatique de totalCost et montantTTC avant toute persistance.
     *
     * Invariants garantis :
     *  - totalCost = acceptedQuantity * unitCost (non null si les deux inputs sont non null)
     *  - montantTTC = totalCost * (1 + taxRate) — taxRate = 0 si null (exonéré de TVA)
     *  - totalCost >= 0 et montantTTC >= 0 (positivité physique)
     *
     * Avant ce fix : totalCost et montantTTC restaient null si le service oubliait de les setter,
     * provoquant des montants GRC nuls et une comptabilité erronée.
     */
    @PrePersist
    @PreUpdate
    private void computeCosts() {
        if (acceptedQuantity != null && unitCost != null) {
            if (unitCost.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException(
                    "GrcDetails [item=" + itemCode + "] : unitCost (" + unitCost + ") ne peut pas être négatif.");
            }
            if (acceptedQuantity < 0) {
                throw new IllegalStateException(
                    "GrcDetails [item=" + itemCode + "] : acceptedQuantity (" + acceptedQuantity + ") ne peut pas être négatif.");
            }
            this.totalCost = unitCost
                .multiply(BigDecimal.valueOf(acceptedQuantity))
                .setScale(2, RoundingMode.HALF_UP);

            BigDecimal taux = (taxRate != null) ? taxRate : BigDecimal.ZERO;
            if (taux.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException(
                    "GrcDetails [item=" + itemCode + "] : taxRate (" + taux + ") ne peut pas être négatif.");
            }
            this.montantTTC = this.totalCost
                .multiply(BigDecimal.ONE.add(taux))
                .setScale(2, RoundingMode.HALF_UP);
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public GrcHeader getGrcHeader() { return grcHeader; }
    public void setGrcHeader(GrcHeader grcHeader) { this.grcHeader = grcHeader; }
    public GrnDetails getGrnDetail() { return grnDetail; }
    public void setGrnDetail(GrnDetails grnDetail) { this.grnDetail = grnDetail; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public Integer getAcceptedQuantity() { return acceptedQuantity; }
    public void setAcceptedQuantity(Integer acceptedQuantity) { this.acceptedQuantity = acceptedQuantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    /** Lecture seule — calculé par computeCosts() au moment de la persistance. */
    public BigDecimal getTotalCost() { return totalCost; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    /** Lecture seule — calculé par computeCosts() au moment de la persistance. */
    public BigDecimal getMontantTTC() { return montantTTC; }
    
    @Deprecated
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    
    @Deprecated
    public void setMontantTTC(BigDecimal montantTTC) { this.montantTTC = montantTTC; }

    public String getMainAccount() { return mainAccount; }
    public void setMainAccount(String mainAccount) { this.mainAccount = mainAccount; }
    public String getSubAccount() { return subAccount; }
    public void setSubAccount(String subAccount) { this.subAccount = subAccount; }
}

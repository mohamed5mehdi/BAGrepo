package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;

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
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private BigDecimal taxRate;
    private BigDecimal montantTTC;

    private String mainAccount;
    private String subAccount;

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
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public BigDecimal getMontantTTC() { return montantTTC; }
    public void setMontantTTC(BigDecimal montantTTC) { this.montantTTC = montantTTC; }

    public String getMainAccount() { return mainAccount; }
    public void setMainAccount(String mainAccount) { this.mainAccount = mainAccount; }
    public String getSubAccount() { return subAccount; }
    public void setSubAccount(String subAccount) { this.subAccount = subAccount; }
}

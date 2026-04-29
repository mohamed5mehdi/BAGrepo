package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
    private Double unitCost;
    private Double totalCost;
    private Double taxRate;
    private Double montantTTC;

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
    public Double getUnitCost() { return unitCost; }
    public void setUnitCost(Double unitCost) { this.unitCost = unitCost; }
    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }
    public Double getTaxRate() { return taxRate; }
    public void setTaxRate(Double taxRate) { this.taxRate = taxRate; }
    public Double getMontantTTC() { return montantTTC; }
    public void setMontantTTC(Double montantTTC) { this.montantTTC = montantTTC; }
}

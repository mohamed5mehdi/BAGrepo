package com.pfe.gestionsachat.model;

import jakarta.persistence.*;

@Entity
@Table(name = "stock_item", uniqueConstraints = {
    @UniqueConstraint(name = "uk_stock_item_code_warehouse", columnNames = {"item_code", "warehouse_id"})
})
public class StockItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    private String itemCode;
    private String itemName;

    @Enumerated(EnumType.STRING)
    private ItemCategory category;

    /**
     * Code emplacement — généré automatiquement, logique virtuel (BAG ERP).
     * Format : LOC-{YYYYMM}-{UUID_SHORT}
     * Règle : 1 emplacement = 1 article unique (unicité stricte via @UniqueConstraint).
     */
    @Column(name = "location_code", unique = true)
    private String locationCode;

    private Integer quantityAvailable;
    private Integer quantityReserved;

    private Integer minStock;
    private Integer reorderPoint;
    private Double unitCost;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    public Integer getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(Integer quantityAvailable) { this.quantityAvailable = quantityAvailable; }
    public Integer getQuantityReserved() { return quantityReserved; }
    public void setQuantityReserved(Integer quantityReserved) { this.quantityReserved = quantityReserved; }
    public Integer getMinStock() { return minStock; }
    public void setMinStock(Integer minStock) { this.minStock = minStock; }
    public Integer getReorderPoint() { return reorderPoint; }
    public void setReorderPoint(Integer reorderPoint) { this.reorderPoint = reorderPoint; }
    public Double getUnitCost() { return unitCost; }
    public void setUnitCost(Double unitCost) { this.unitCost = unitCost; }
}

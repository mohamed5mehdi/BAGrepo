package com.pfe.gestionsachat.dto.transfer;

public class AvailableStockDto {
    private Long id;
    private String itemCode;
    private String itemName;
    private String locationCode;
    private Integer quantityAvailable;
    
    // Properties specific for MagasinierStock.tsx
    private String locationName;
    private Integer quantity;
    
    // Properties specific for TransferDashboard.tsx
    private WarehouseDto warehouse;

    public AvailableStockDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    
    public Integer getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(Integer quantityAvailable) { this.quantityAvailable = quantityAvailable; }
    
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public WarehouseDto getWarehouse() { return warehouse; }
    public void setWarehouse(WarehouseDto warehouse) { this.warehouse = warehouse; }

    public static class WarehouseDto {
        private Long id;
        private String name;

        public WarehouseDto() {}
        public WarehouseDto(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}

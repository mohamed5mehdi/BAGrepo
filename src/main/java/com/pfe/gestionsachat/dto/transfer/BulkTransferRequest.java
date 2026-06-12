package com.pfe.gestionsachat.dto.transfer;

import java.util.List;

public class BulkTransferRequest {
    private Long destWarehouseId;
    private List<TransferItemReq> items;

    public Long getDestWarehouseId() {
        return destWarehouseId;
    }

    public void setDestWarehouseId(Long destWarehouseId) {
        this.destWarehouseId = destWarehouseId;
    }

    public List<TransferItemReq> getItems() {
        return items;
    }

    public void setItems(List<TransferItemReq> items) {
        this.items = items;
    }
}

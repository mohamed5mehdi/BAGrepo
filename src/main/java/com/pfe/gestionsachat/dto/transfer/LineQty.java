package com.pfe.gestionsachat.dto.transfer;

public class LineQty {
    private Long lineId;
    private Integer quantity;

    public Long getLineId() {
        return lineId;
    }

    public void setLineId(Long lineId) {
        this.lineId = lineId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}

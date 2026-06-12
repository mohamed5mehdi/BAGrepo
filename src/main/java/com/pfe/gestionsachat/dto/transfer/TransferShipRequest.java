package com.pfe.gestionsachat.dto.transfer;

import java.util.List;

public class TransferShipRequest {
    private List<LineQty> lines;

    public List<LineQty> getLines() {
        return lines;
    }

    public void setLines(List<LineQty> lines) {
        this.lines = lines;
    }
}

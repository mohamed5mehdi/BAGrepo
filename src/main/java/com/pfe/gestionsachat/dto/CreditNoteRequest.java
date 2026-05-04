package com.pfe.gestionsachat.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreditNoteRequest {
    private Long grnHeaderId;
    private String creditNoteNumber;
    private List<ItemQuantityRejection> rejections;
}

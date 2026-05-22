package com.pfe.gestionsachat.exception;

/**
 * Levée lors d'une tentative de shipTransfer ou submitTransfer
 * quand le stock disponible est inférieur à la quantité demandée.
 *
 * HTTP 409 CONFLICT — code métier : INSUFFICIENT_STOCK
 */
public class InsufficientStockTransferException extends RuntimeException {

    private final String itemCode;
    private final int available;
    private final int requested;

    public InsufficientStockTransferException(String itemCode, int available, int requested) {
        super("Stock insuffisant pour [" + itemCode + "] lors du transfert. "
              + "Disponible : " + available + ", Demandé : " + requested);
        this.itemCode = itemCode;
        this.available = available;
        this.requested = requested;
    }

    public String getItemCode() { return itemCode; }
    public int getAvailable() { return available; }
    public int getRequested() { return requested; }
}

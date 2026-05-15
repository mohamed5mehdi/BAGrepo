package com.pfe.gestionsachat.exception;

/**
 * Levée quand receivedQty + déjà reçu > orderedQty sur un PO.
 * Cas : sur-réception quantité BAG ERP — jamais un doublon de document.
 * HTTP 422 (Unprocessable Entity) — règle métier violée, pas un conflit de ressource.
 */
public class OverReceptionException extends RuntimeException {
    public OverReceptionException(String message) {
        super(message);
    }
}

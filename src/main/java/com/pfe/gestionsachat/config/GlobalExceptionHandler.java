package com.pfe.gestionsachat.config;

import com.pfe.gestionsachat.exception.InsufficientStockTransferException;
import com.pfe.gestionsachat.exception.OverReceptionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(RuntimeException ex, WebRequest request) {
        ex.printStackTrace(); // Log the full stack trace for debugging
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("error", "INTERNAL_ERROR");
        body.put("message", ex.getMessage());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Erreur de saisie utilisateur — HTTP 400 avec code métier structuré.
     * Couvre : itemCode inconnu dans le PO, montantEstime manquant, etc.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("error", "INVALID_INPUT");
        body.put("message", ex.getMessage());
        body.put("field", extractField(ex.getMessage()));
        body.put("status", HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Sur-réception GRN — HTTP 409 Conflict (violation règle métier BAG ERP).
     */
    @ExceptionHandler(OverReceptionException.class)
    public ResponseEntity<Object> handleOverReception(OverReceptionException ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("error", "OVER_RECEPTION");
        body.put("message", ex.getMessage());
        body.put("status", HttpStatus.UNPROCESSABLE_ENTITY.value());
        return new ResponseEntity<>(body, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Transition d'état invalide (machine à états PO/GRN/GRC/Transfer) — HTTP 422.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalState(IllegalStateException ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("error", "INVALID_STATE_TRANSITION");
        body.put("message", ex.getMessage());
        body.put("status", HttpStatus.UNPROCESSABLE_ENTITY.value());
        return new ResponseEntity<>(body, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * RISQUE-09 : SecurityException → HTTP 403 FORBIDDEN.
     * Sans ce handler, SecurityException extends RuntimeException serait interceptée
     * par handleRuntimeException et retournerait HTTP 500 au frontend.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Object> handleSecurity(SecurityException ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("error", "FORBIDDEN");
        body.put("message", ex.getMessage());
        body.put("status", HttpStatus.FORBIDDEN.value());
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    /**
     * Stock insuffisant lors d'un transfert — HTTP 409 CONFLICT.
     * Code métier INSUFFICIENT_STOCK avec détail itemCode/available/requested
     * pour un message d'erreur précis côté frontend.
     */
    @ExceptionHandler(InsufficientStockTransferException.class)
    public ResponseEntity<Object> handleInsufficientStock(InsufficientStockTransferException ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("error", "INSUFFICIENT_STOCK");
        body.put("message", ex.getMessage());
        body.put("field", "quantityRequested");
        body.put("itemCode", ex.getItemCode());
        body.put("available", ex.getAvailable());
        body.put("requested", ex.getRequested());
        body.put("status", HttpStatus.CONFLICT.value());
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex, WebRequest request) {
        ex.printStackTrace();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("error", "INTERNAL_ERROR");
        body.put("message", "Une erreur inattendue s'est produite.");
        body.put("details", ex.getMessage());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /** RISQUE-25 : Extrait un hint de champ depuis le message d'erreur pour le frontend. */
    private String extractField(String message) {
        if (message == null) return null;
        if (message.contains("itemCode") || message.contains("Article")) return "itemCode";
        if (message.contains("montantEstime")) return "montantEstime";
        if (message.contains("grnHeader")) return "grnHeader";
        if (message.contains("PO")) return "purchaseOrder";
        // Champs transfert inter-sites
        if (message.contains("warehouseSource") || message.contains("warehouse source") || message.contains("entrepôt")) return "warehouseSource";
        if (message.contains("quantityRequested") || message.contains("Stock insuffisant")) return "quantityRequested";
        if (message.contains("Magasinier non assign")) return "userId";
        if (message.contains("warehouse dest") || message.contains("warehouse_dest")) return "warehouseDest";
        return null;
    }
}

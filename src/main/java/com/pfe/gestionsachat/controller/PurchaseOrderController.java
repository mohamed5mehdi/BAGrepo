package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.PurchaseOrder;
import com.pfe.gestionsachat.service.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@CrossOrigin(origins = "*")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> getAllPurchaseOrders() {
        return ResponseEntity.ok(purchaseOrderService.getAllPurchaseOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> getPurchaseOrderById(@PathVariable @org.springframework.lang.NonNull Integer id) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderById(id));
    }

    @GetMapping("/da/{oidDa}")
    public ResponseEntity<PurchaseOrder> getPurchaseOrderByDa(@PathVariable @org.springframework.lang.NonNull Integer oidDa) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderByDa(oidDa));
    }

    @GetMapping("/status/{statut}")
    public ResponseEntity<List<PurchaseOrder>> getPurchaseOrdersByStatus(@PathVariable @org.springframework.lang.NonNull String statut) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrdersByStatus(statut));
    }
}
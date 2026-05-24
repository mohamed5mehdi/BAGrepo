package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.DaHeader;
import com.pfe.gestionsachat.model.StatutDA;
import com.pfe.gestionsachat.service.DaHeaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping({"/api/da-headers", "/api/purchase-requests", "/api/purchase-request", "/api/requests", "/api/request", "/api/da"})

public class DaHeaderController {

    @Autowired
    private DaHeaderService daHeaderService;

    @PostMapping
    public ResponseEntity<DaHeader> createPurchaseRequest(@RequestBody @org.springframework.lang.NonNull DaHeader request) {
        return ResponseEntity.ok(daHeaderService.createPurchaseRequest(request));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<DaHeader> submitPurchaseRequest(@PathVariable @org.springframework.lang.NonNull Integer id) {
        return ResponseEntity.ok(daHeaderService.submitPurchaseRequest(id));
    }

    @GetMapping
    public ResponseEntity<List<DaHeader>> getAllPurchaseRequests() {
        return ResponseEntity.ok(daHeaderService.getAllPurchaseRequests());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DaHeader> getPurchaseRequestById(@PathVariable @org.springframework.lang.NonNull Integer id) {
        return ResponseEntity.ok(daHeaderService.getPurchaseRequestById(id));
    }

    @GetMapping("/status/{statut}")
    public ResponseEntity<List<DaHeader>> getPurchaseRequestsByStatus(@PathVariable @org.springframework.lang.NonNull StatutDA statut) {
        return ResponseEntity.ok(daHeaderService.getPurchaseRequestsByStatus(statut));
    }

    @GetMapping("/demandeur/{userId}")
    public ResponseEntity<List<DaHeader>> getPurchaseRequestsByDemandeur(@PathVariable @org.springframework.lang.NonNull Integer userId) {
        return ResponseEntity.ok(daHeaderService.getPurchaseRequestsByDemandeur(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DaHeader> updatePurchaseRequest(@PathVariable @org.springframework.lang.NonNull Integer id, @RequestBody @org.springframework.lang.NonNull DaHeader requestDetails) {
        return ResponseEntity.ok(daHeaderService.updatePurchaseRequest(id, requestDetails));
    }

    @PutMapping("/{id}/valoriser-achat")
    public ResponseEntity<DaHeader> valoriserAchat(
            @PathVariable @org.springframework.lang.NonNull Integer id,
            @RequestParam @org.springframework.lang.NonNull java.math.BigDecimal prixUnitaire,
            @RequestParam @org.springframework.lang.NonNull Integer supplierId) {
        return ResponseEntity.ok(daHeaderService.valoriserAchat(id, prixUnitaire, supplierId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchaseRequest(@PathVariable @org.springframework.lang.NonNull Integer id) {
        daHeaderService.deletePurchaseRequest(id);
        return ResponseEntity.ok().build();
    }
}

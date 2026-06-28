package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.Supplier;
import com.pfe.gestionsachat.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/suppliers")

public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @PreAuthorize("hasAnyRole('ADMINISTRATEUR', 'ACHETEUR')")
    @PostMapping
    public ResponseEntity<Supplier> createSupplier(@RequestBody @org.springframework.lang.NonNull Supplier supplier) {
        return ResponseEntity.ok(supplierService.createSupplier(supplier));
    }

    // RBAC Niv.1 — audit session 3
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<Supplier>> getAllSuppliers() {
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    // RBAC Niv.1 — audit session 3
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<Supplier> getSupplierById(@PathVariable @org.springframework.lang.NonNull Integer id) {
        return ResponseEntity.ok(supplierService.getSupplierById(id));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATEUR', 'ACHETEUR')")
    @PutMapping("/{id}")
    public ResponseEntity<Supplier> updateSupplier(@PathVariable @org.springframework.lang.NonNull Integer id, @RequestBody @org.springframework.lang.NonNull Supplier supplierDetails) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, supplierDetails));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATEUR', 'ACHETEUR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable @org.springframework.lang.NonNull Integer id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok().build();
    }
}

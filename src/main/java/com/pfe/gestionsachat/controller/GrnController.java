package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.GrnHeader;
import com.pfe.gestionsachat.service.GrnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grn")
@CrossOrigin(origins = "*")
public class GrnController {

    @Autowired
    private GrnService grnService;

    @PostMapping
    public ResponseEntity<GrnHeader> createGrn(@RequestBody GrnHeader grn) {
        return ResponseEntity.ok(grnService.createGrn(grn));
    }

    @PutMapping("/{id}/valider")
    public ResponseEntity<GrnHeader> validateGrn(@PathVariable Long id) {
        return ResponseEntity.ok(grnService.validateGrn(java.util.Objects.requireNonNull(id)));
    }
    
    // Autres endpoints CRUD si nécessaire...
}

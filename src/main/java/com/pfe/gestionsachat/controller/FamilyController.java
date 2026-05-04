package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.Family;
import com.pfe.gestionsachat.service.FamilyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;
import com.pfe.gestionsachat.dto.FamilyDtoPublic;
import com.pfe.gestionsachat.dto.FamilyDtoFinancier;

@RestController
@RequestMapping({"/api/families", "/api/family", "/api/categories", "/api/category", "/api/familles"})
@CrossOrigin(origins = "*")
public class FamilyController {

    @Autowired
    private FamilyService familyService;

    @PostMapping
    public ResponseEntity<Family> createFamily(@RequestBody @org.springframework.lang.NonNull Family family) {
        return ResponseEntity.ok(familyService.createFamily(family));
    }

    @GetMapping(produces = "application/json;charset=UTF-8")
    public ResponseEntity<?> getAllFamilies(@RequestParam(required = false, defaultValue = "DEMANDEUR") String role) {
        List<Family> families = familyService.getAllFamilies();
        if ("DEMANDEUR".equalsIgnoreCase(role)) {
            List<FamilyDtoPublic> dtoList = families.stream()
                .map(f -> new FamilyDtoPublic(f.getIdFamily(), f.getLibelle(), f.getBudgetRestant()))
                .collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        } else {
            List<FamilyDtoFinancier> dtoList = families.stream()
                .map(f -> new FamilyDtoFinancier(f.getIdFamily(), f.getLibelle(), f.getBudgetInitial(), f.getBudgetRestant(), f.getBudgetEngage(), f.getBudgetDisponible()))
                .collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Family> getFamilyById(@PathVariable @org.springframework.lang.NonNull Integer id) {
        return ResponseEntity.ok(familyService.getFamilyById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Family> updateFamily(@PathVariable @org.springframework.lang.NonNull Integer id, @RequestBody @org.springframework.lang.NonNull Family familyDetails) {
        return ResponseEntity.ok(familyService.updateFamily(id, familyDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFamily(@PathVariable @org.springframework.lang.NonNull Integer id) {
        familyService.deleteFamily(id);
        return ResponseEntity.ok().build();
    }
}
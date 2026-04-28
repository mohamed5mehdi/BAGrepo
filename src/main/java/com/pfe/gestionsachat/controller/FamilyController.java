package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.Family;
import com.pfe.gestionsachat.service.FamilyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
    public ResponseEntity<List<Family>> getAllFamilies() {
        return ResponseEntity.ok(familyService.getAllFamilies());
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
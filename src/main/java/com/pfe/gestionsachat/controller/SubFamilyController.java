package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.SubFamily;
import com.pfe.gestionsachat.service.SubFamilyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping({"/api/sub-families", "/api/sub-family", "/api/subcategories", "/api/subcategory", "/api/sous-familles"})

public class SubFamilyController {

    @Autowired
    private SubFamilyService subFamilyService;

    @PreAuthorize("hasAnyRole('ADMINISTRATEUR', 'DAF')")
    @PostMapping
    public ResponseEntity<SubFamily> createSubFamily(@RequestBody @org.springframework.lang.NonNull SubFamily subFamily) {
        return ResponseEntity.ok(subFamilyService.createSubFamily(subFamily));
    }

    // RBAC Niv.1 — audit session 3
    @PreAuthorize("isAuthenticated()")
    @GetMapping(produces = "application/json;charset=UTF-8")
    @ResponseStatus(org.springframework.http.HttpStatus.OK)
    public ResponseEntity<List<SubFamily>> getAllSubFamilies() {
        return ResponseEntity.ok(subFamilyService.getAllSubFamilies());
    }

    // RBAC Niv.1 — audit session 3
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/{id}", produces = "application/json;charset=UTF-8")
    public ResponseEntity<Object> getSubFamilyByIdOrFamilyChildren(@PathVariable String id) {
        try {
            Integer numericId = Integer.parseInt(id);
            // We return a list of sub-families for this ID as if it were a family ID, 
            // because that's what the frontend seems to expect when loading sub-families.
            List<SubFamily> subFamilies = subFamilyService.getSubFamiliesByFamily(numericId);
            if (!subFamilies.isEmpty()) {
                return ResponseEntity.ok(subFamilies);
            }
            // If no children found, maybe it IS a sub-family ID?
            return ResponseEntity.ok(subFamilyService.getSubFamilyById(numericId));
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(subFamilyService.getSubFamiliesByFamilyName(id));
        }
    }

    // RBAC Niv.1 — audit session 3
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = {"/family/{familyIdOrName}", "/famille/{familyIdOrName}"}, produces = "application/json;charset=UTF-8")
    public ResponseEntity<List<SubFamily>> getSubFamiliesByFamily(@PathVariable String familyIdOrName) {
        try {
            // Try as ID first
            Integer id = Integer.parseInt(familyIdOrName);
            return ResponseEntity.ok(subFamilyService.getSubFamiliesByFamily(id));
        } catch (NumberFormatException e) {
            // If not a number, try to find the family by libelle
            return ResponseEntity.ok(subFamilyService.getSubFamiliesByFamilyName(familyIdOrName));
        }
    }

    // RBAC Niv.1 — audit session 3
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/search", produces = "application/json;charset=UTF-8")
    public ResponseEntity<List<SubFamily>> searchSubFamilies(
            @RequestParam(required = false) Integer familyId,
            @RequestParam(required = false) Integer familleId) {
        Integer id = (familyId != null) ? familyId : familleId;
        if (id != null) {
            return ResponseEntity.ok(subFamilyService.getSubFamiliesByFamily(id));
        }
        return ResponseEntity.ok(subFamilyService.getAllSubFamilies());
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATEUR', 'DAF')")
    @PutMapping("/{id}")
    public ResponseEntity<SubFamily> updateSubFamily(@PathVariable @org.springframework.lang.NonNull Integer id, @RequestBody @org.springframework.lang.NonNull SubFamily subFamilyDetails) {
        return ResponseEntity.ok(subFamilyService.updateSubFamily(id, subFamilyDetails));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATEUR', 'DAF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubFamily(@PathVariable @org.springframework.lang.NonNull Integer id) {
        subFamilyService.deleteSubFamily(id);
        return ResponseEntity.ok().build();
    }
}

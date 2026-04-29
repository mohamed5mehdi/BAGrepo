package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.GrcHeader;
import com.pfe.gestionsachat.service.GrcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grc")
@CrossOrigin(origins = "*")
public class GrcController {

    @Autowired
    private GrcService grcService;

    @PostMapping
    public ResponseEntity<GrcHeader> createGrc(@RequestBody GrcHeader grc) {
        return ResponseEntity.ok(grcService.createGrc(grc));
    }

    @PutMapping("/{id}/valider")
    public ResponseEntity<GrcHeader> validateGrc(@PathVariable Long id) {
        return ResponseEntity.ok(grcService.validateGrc(java.util.Objects.requireNonNull(id)));
    }
}

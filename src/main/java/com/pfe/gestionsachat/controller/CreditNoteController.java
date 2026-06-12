package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.dto.CreditNoteRequest;
import com.pfe.gestionsachat.model.CreditNote;
import com.pfe.gestionsachat.service.CreditNoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/creditnote")

public class CreditNoteController {

    @Autowired
    private CreditNoteService creditNoteService;

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPTABLE', 'ACHETEUR', 'ACHETEUR_INFORMATIQUE', 'ACHETEUR_BUREAUTIQUE', 'ACHETEUR_MOBILIER', 'ACHETEUR_CONSOMMABLE', 'ACHETEUR_AUTRE', 'ADMINISTRATEUR')")
    public ResponseEntity<CreditNote> createCreditNote(@RequestBody CreditNoteRequest request) {
        return ResponseEntity.ok(creditNoteService.processCreditNote(request));
    }
}


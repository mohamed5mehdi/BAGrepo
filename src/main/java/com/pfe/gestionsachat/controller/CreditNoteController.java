package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.CreditNote;
import com.pfe.gestionsachat.service.CreditNoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/creditnote")
@CrossOrigin(origins = "*")
public class CreditNoteController {

    @Autowired
    private CreditNoteService creditNoteService;

    @PostMapping
    public ResponseEntity<CreditNote> createCreditNote(@RequestBody CreditNote creditNote) {
        return ResponseEntity.ok(creditNoteService.createCreditNote(creditNote));
    }
}

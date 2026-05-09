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
    
    @Autowired
    private com.pfe.gestionsachat.service.PdfExportService pdfExportService;
    @Autowired
    private com.pfe.gestionsachat.repository.GrnHeaderRepository grnRepository;

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadGrn(@PathVariable Long id) {
        // Recherche par ID natif du GRN, ou par PO ID si non trouvé (Demo Robustness)
        GrnHeader grn = grnRepository.findById(id)
                .orElseGet(() -> grnRepository.findByPurchaseOrder_IdPo(id.intValue()).stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("GRN introuvable pour l'ID ou le PO spécifié")));
        
        byte[] pdfBytes = pdfExportService.generateGrnPdf(grn);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=GRN_" + grn.getId() + ".pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}

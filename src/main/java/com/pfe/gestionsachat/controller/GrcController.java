package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.GrcHeader;
import com.pfe.gestionsachat.model.GrnHeader;
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
    @Autowired
    private com.pfe.gestionsachat.service.PdfExportService pdfExportService;
    @Autowired
    private com.pfe.gestionsachat.repository.GrcHeaderRepository grcRepository;

    @Autowired
    private com.pfe.gestionsachat.repository.GrnHeaderRepository grnRepository;

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadGrc(@PathVariable Long id) {
        // Recherche par ID natif du GRC, ou par PO ID si non trouvé
        GrcHeader grc = grcRepository.findById(id)
                .orElseGet(() -> {
                    GrnHeader grn = grnRepository.findByPurchaseOrder_IdPo(id.intValue()).stream().findFirst()
                            .orElseThrow(() -> new RuntimeException("GRN/GRC introuvable pour cet ID"));
                    GrcHeader g = grn.getGrcHeader();
                    if (g == null) throw new RuntimeException("GRC non encore généré pour ce PO");
                    return g;
                });
        
        byte[] pdfBytes = pdfExportService.generateGrcPdf(grc);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=GRC_" + grc.getId() + ".pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}

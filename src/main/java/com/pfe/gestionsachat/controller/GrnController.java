package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.GrnHeader;
import com.pfe.gestionsachat.model.GrnStatus;
import com.pfe.gestionsachat.repository.GrnHeaderRepository;
import com.pfe.gestionsachat.service.GrnService;
import com.pfe.gestionsachat.service.PdfExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/grn")

public class GrnController {

    @Autowired private GrnService grnService;
    @Autowired private PdfExportService pdfExportService;
    @Autowired private GrnHeaderRepository grnRepository;

    @GetMapping
    public ResponseEntity<List<GrnHeader>> getAllGrns() {
        return ResponseEntity.ok(grnService.getAllGrns());
    }

    @PostMapping
    public ResponseEntity<GrnHeader> createGrn(@RequestBody GrnHeader grn) {
        return ResponseEntity.ok(grnService.createGrn(grn));
    }

    /**
     * /valider — Magasinier : PENDING → ENTRY_COMPLETED.
     * Déclenche la mise à jour du stock et génère le grnNumber.
     */
    @PutMapping("/{id}/valider")
    public ResponseEntity<GrnHeader> validateGrn(@PathVariable Long id) {
        return ResponseEntity.ok(grnService.validateGrn(Objects.requireNonNull(id)));
    }

    /**
     * Filtre les GRNs par statut — utile pour les dashboards Magasinier/Achat.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<GrnHeader>> getByStatus(@PathVariable GrnStatus status) {
        return ResponseEntity.ok(grnRepository.findByStatus(status));
    }

    /**
     * Téléchargement PDF du GRN.
     * @Transactional(readOnly=true) : maintient la session JPA ouverte pour
     * les relations lazy (details, supplier, purchaseOrder) nécessaires au PDF.
     */
    @GetMapping("/{id}/download")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadGrn(@PathVariable Long id) {
        GrnHeader grn = grnRepository.findById(id)
                .orElseGet(() -> grnRepository.findByPurchaseOrder_IdPo(id.intValue()).stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("GRN introuvable pour l'ID ou le PO : " + id)));

        byte[] pdfBytes = pdfExportService.generateGrnPdf(grn);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=GRN_" + grn.getId() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}


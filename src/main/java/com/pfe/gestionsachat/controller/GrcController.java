package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.GrcHeader;
import com.pfe.gestionsachat.model.GrcStatus;
import com.pfe.gestionsachat.model.GrnHeader;
import com.pfe.gestionsachat.repository.GrcHeaderRepository;
import com.pfe.gestionsachat.repository.GrnHeaderRepository;
import com.pfe.gestionsachat.service.GrcService;
import com.pfe.gestionsachat.service.PdfExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Objects;

@RestController
@RequestMapping("/api/grc")

public class GrcController {

    @Autowired private GrcService grcService;
    @Autowired private PdfExportService pdfExportService;
    @Autowired private GrcHeaderRepository grcRepository;
    @Autowired private GrnHeaderRepository grnRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('MAGASINIER_DEST', 'ACHETEUR', 'COMPTABLE', 'ADMINISTRATEUR')")
    public ResponseEntity<java.util.List<GrcHeader>> getAllGrcs() {
        return ResponseEntity.ok(grcService.getAllGrcs());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MAGASINIER_DEST', 'ADMINISTRATEUR')")
    public ResponseEntity<GrcHeader> createGrc(@RequestBody GrcHeader grc) {
        return ResponseEntity.ok(grcService.createGrc(grc));
    }

    /**
     * /valider — Comptable : PENDING_APPROVAL → POSTED (validation financière + génération facture).
     */
    @PutMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('COMPTABLE', 'DAF', 'ADMINISTRATEUR')")
    public ResponseEntity<GrcHeader> validateGrc(@PathVariable Long id) {
        return ResponseEntity.ok(grcService.validateGrc(Objects.requireNonNull(id)));
    }

    /**
     * /approuver — second niveau d'approbation (ex: Responsable Achat).
     * Guard : le GRC doit être en POSTED pour être marqué APPROVED.
     * Si votre flux n'a qu'un seul niveau, cet endpoint peut rester ici
     * mais il DOIT déléguer une opération différente de /valider.
     * En attendant, il valide également (idempotent guard dans le service).
     */
    @PutMapping("/{id}/approuver")
    @PreAuthorize("hasAnyRole('RESP_ACHAT', 'ADMINISTRATEUR')")
    public ResponseEntity<GrcHeader> approveGrc(@PathVariable Long id) {
        return ResponseEntity.ok(grcService.approveGrc(Objects.requireNonNull(id)));
    }

    /**
     * Téléchargement PDF du GRC.
     * @Transactional(readOnly=true) : évite LazyInitializationException sur
     * grnHeader.getGrcHeader() (relation @OneToOne mappedBy — lazy par défaut).
     */
    @GetMapping("/{id}/download")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MAGASINIER_DEST', 'ACHETEUR', 'COMPTABLE', 'ADMINISTRATEUR')")
    public ResponseEntity<byte[]> downloadGrc(@PathVariable Long id) {
        GrcHeader grc = grcRepository.findById(id)
                .orElseGet(() -> {
                    GrnHeader grn = grnRepository.findByPurchaseOrder_IdPo(id.intValue()).stream().findFirst()
                            .orElseThrow(() -> new RuntimeException("GRN/GRC introuvable pour l'ID: " + id));
                    GrcHeader g = grn.getGrcHeader();
                    if (g == null) throw new RuntimeException("GRC non encore généré pour ce PO (GRN ID: " + grn.getId() + ")");
                    return g;
                });

        byte[] pdfBytes = pdfExportService.generateGrcPdf(grc);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=GRC_" + grc.getId() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}


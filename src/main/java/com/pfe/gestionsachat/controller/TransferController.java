package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.StockItem;
import com.pfe.gestionsachat.model.TransferHeader;
import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.repository.StockItemRepository;
import com.pfe.gestionsachat.repository.UserRepository;
import com.pfe.gestionsachat.service.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import com.pfe.gestionsachat.repository.TransferHeaderRepository;
import com.pfe.gestionsachat.service.PdfGenerationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TransferController — Endpoints REST du flux transfert inter-sites.
 *
 * Pattern d'injection utilisateur : @RequestParam Integer userId (cohérent avec
 * l'ensemble du projet — RISQUE-15 : ne pas introduire @AuthenticationPrincipal).
 *
 * Base URL : /api/transfers
 */
@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    @Autowired private TransferService transferService;
    @Autowired private UserRepository userRepository;
    @Autowired private StockItemRepository stockItemRepository;
    @Autowired private PdfGenerationService pdfGenerationService;
    @Autowired private TransferHeaderRepository transferHeaderRepository;

    // ── Stock disponible ──────────────────────────────────────────────────────

    /**
     * GET /api/transfers/stock/available
     * Retourne tous les articles avec quantityAvailable > 0, avec JOIN FETCH warehouse (RISQUE-13).
     */
    @GetMapping("/stock/available")
    public ResponseEntity<List<StockItem>> getAvailableStock() {
        return ResponseEntity.ok(stockItemRepository.findAvailableStock());
    }

    // ── Soumission ────────────────────────────────────────────────────────────

    /**
     * POST /api/transfers?userId={id}
     * Soumet une nouvelle demande de transfert (EMPLOYE).
     */
    @PostMapping
    public ResponseEntity<TransferHeader> submit(
            @RequestBody TransferHeader header,
            @RequestParam Integer userId) {
        // MINEUR-02 : message explicite → HTTP 400 via handleIllegalArgumentException
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));
        return ResponseEntity.ok(transferService.submitTransfer(header, user));
    }

    // ── Expédition ────────────────────────────────────────────────────────────

    /**
     * PUT /api/transfers/{id}/ship?userId={id}
     * Expédie le transfert PENDING → IN_TRANSIT (MAGASINIER source uniquement).
     */
    @PutMapping("/{id}/ship")
    public ResponseEntity<TransferHeader> ship(
            @PathVariable Long id,
            @RequestParam Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));
        return ResponseEntity.ok(transferService.shipTransfer(id, user));
    }

    // ── Réception ─────────────────────────────────────────────────────────────

    /**
     * PUT /api/transfers/{id}/receive?userId={id}
     * Valide la réception IN_TRANSIT → RECEIVED (MAGASINIER_DEST uniquement).
     */
    @PutMapping("/{id}/receive")
    public ResponseEntity<TransferHeader> receive(
            @PathVariable Long id,
            @RequestParam Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));
        return ResponseEntity.ok(transferService.receiveTransfer(id, user));
    }

    // ── Annulation ────────────────────────────────────────────────────────────

    /**
     * DELETE /api/transfers/{id}?userId={id}
     * Annule le transfert PENDING → CANCELLED (auteur ou ADMINISTRATEUR).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<TransferHeader> cancel(
            @PathVariable Long id,
            @RequestParam Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));
        return ResponseEntity.ok(transferService.cancelTransfer(id, user));
    }

    // ── Lectures ──────────────────────────────────────────────────────────────

    /**
     * GET /api/transfers/my?userId={id}
     * Historique des transferts soumis par l'employé (vue DemandeurPage).
     */
    @GetMapping("/my")
    public ResponseEntity<List<TransferHeader>> getMyTransfers(@RequestParam Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));
        return ResponseEntity.ok(transferService.getMyTransfers(user));
    }

    /**
     * GET /api/transfers/source?userId={id}
     * File PENDING pour le MAGASINIER source (onglet "Transferts à expédier").
     */
    @GetMapping("/source")
    public ResponseEntity<List<TransferHeader>> getSourceQueue(@RequestParam Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));
        return ResponseEntity.ok(transferService.getSourceQueue(user));
    }

    /**
     * GET /api/transfers/dest?userId={id}
     * File IN_TRANSIT pour le MAGASINIER_DEST (vue réception).
     */
    @GetMapping("/dest")
    public ResponseEntity<List<TransferHeader>> getDestQueue(@RequestParam Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));
        return ResponseEntity.ok(transferService.getDestQueue(user));
    }

    /**
     * GET /api/transfers/history/source?userId={id}
     */
    @GetMapping("/history/source")
    public ResponseEntity<List<TransferHeader>> getSourceHistory(@RequestParam Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));
        return ResponseEntity.ok(transferService.getSourceHistory(user));
    }

    /**
     * GET /api/transfers/history/dest?userId={id}
     */
    @GetMapping("/history/dest")
    public ResponseEntity<List<TransferHeader>> getDestHistory(@RequestParam Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));
        return ResponseEntity.ok(transferService.getDestHistory(user));
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/pdf/lto")
    public ResponseEntity<byte[]> getLtoPdf(@PathVariable Long id) {
        byte[] pdfBytes = transferService.generateLtoPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "LTO-" + id + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    @GetMapping("/{id}/pdf/lti")
    public ResponseEntity<byte[]> getLtiPdf(@PathVariable Long id) {
        byte[] pdfBytes = transferService.generateLtiPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "LTI-" + id + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}

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
import com.pfe.gestionsachat.dto.transfer.*;
import org.springframework.security.access.prepost.PreAuthorize;

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
    // RBAC Niv.1 — audit session 3
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/stock/available")
    public ResponseEntity<List<com.pfe.gestionsachat.dto.transfer.AvailableStockDto>> getAvailableStock() {
        List<StockItem> items = stockItemRepository.findAvailableStock();
        List<com.pfe.gestionsachat.dto.transfer.AvailableStockDto> dtos = items.stream().map(item -> {
            com.pfe.gestionsachat.dto.transfer.AvailableStockDto dto = new com.pfe.gestionsachat.dto.transfer.AvailableStockDto();
            dto.setId(item.getId());
            dto.setItemCode(item.getItemCode());
            dto.setItemName(item.getItemName());
            dto.setLocationCode(item.getLocationCode());
            dto.setQuantityAvailable(item.getQuantityAvailable());
            
            // Map for MagasinierStock.tsx
            dto.setQuantity(item.getQuantityAvailable());
            dto.setLocationName(item.getWarehouse() != null ? item.getWarehouse().getName() : null);
            
            // Map for TransferDashboard.tsx
            if (item.getWarehouse() != null) {
                dto.setWarehouse(new com.pfe.gestionsachat.dto.transfer.AvailableStockDto.WarehouseDto(
                    item.getWarehouse().getId(), 
                    item.getWarehouse().getName()
                ));
            }
            return dto;
        }).toList();
        return ResponseEntity.ok(dtos);
    }

    // ── Soumission ────────────────────────────────────────────────────────────

    /**
     * POST /api/transfers?userId={id}
     * Soumet une nouvelle demande de transfert (EMPLOYE).
     */
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MAGASINIER', 'MAGASINIER_DEST', 'ADMINISTRATEUR')")
    @PostMapping
    public ResponseEntity<TransferHeader> submit(
            @RequestBody TransferHeader header,
            @RequestParam Integer userId) {
        // MINEUR-02 : message explicite → HTTP 400 via handleIllegalArgumentException
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));
        return ResponseEntity.ok(transferService.submitTransfer(header, user));
    }

    /**
     * POST /api/transfers/bulk?userId={id}
     * Soumet une demande de transfert multi-sources (MAGASINIER).
     */
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MAGASINIER', 'MAGASINIER_DEST', 'ADMINISTRATEUR')")
    @PostMapping("/bulk")
    public ResponseEntity<List<TransferHeader>> submitBulk(
            @RequestBody BulkTransferRequest request,
            @RequestParam Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));
        return ResponseEntity.ok(transferService.submitBulkTransfers(request, user));
    }

    // ── Expédition ────────────────────────────────────────────────────────────

    /**
     * PUT /api/transfers/{id}/ship?userId={id}
     * Expédie le transfert PENDING → IN_TRANSIT (MAGASINIER source uniquement).
     */
    @PreAuthorize("hasAnyRole('MAGASINIER', 'ADMINISTRATEUR')")
    @PutMapping("/{id}/ship")
    public ResponseEntity<TransferHeader> ship(
            @PathVariable Long id,
            @RequestBody TransferShipRequest request,
            @RequestParam Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));
        return ResponseEntity.ok(transferService.shipTransfer(id, user, request));
    }

    // ── Réception ─────────────────────────────────────────────────────────────

    /**
     * PUT /api/transfers/{id}/receive?userId={id}
     * Valide la réception IN_TRANSIT → RECEIVED (MAGASINIER_DEST uniquement).
     */
    @PreAuthorize("hasAnyRole('MAGASINIER_DEST', 'ADMINISTRATEUR')")
    @PutMapping("/{id}/receive")
    public ResponseEntity<TransferHeader> receive(
            @PathVariable Long id,
            @RequestBody TransferReceiveRequest request,
            @RequestParam Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));
        return ResponseEntity.ok(transferService.receiveTransfer(id, user, request));
    }

    // ── Annulation ────────────────────────────────────────────────────────────

    /**
     * DELETE /api/transfers/{id}?userId={id}
     * Annule le transfert PENDING → CANCELLED (auteur ou ADMINISTRATEUR).
     */
    @PreAuthorize("hasAnyRole('EMPLOYE', 'ADMINISTRATEUR')")
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
    @PreAuthorize("hasAnyRole('EMPLOYE', 'MAGASINIER', 'MAGASINIER_DEST', 'ADMINISTRATEUR')")
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
    @PreAuthorize("hasAnyRole('MAGASINIER', 'ADMINISTRATEUR')")
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
    @PreAuthorize("hasAnyRole('MAGASINIER_DEST', 'ADMINISTRATEUR')")
    @GetMapping("/dest")
    public ResponseEntity<List<TransferHeader>> getDestQueue(@RequestParam Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));
        return ResponseEntity.ok(transferService.getDestQueue(user));
    }

    /**
     * GET /api/transfers/all
     * Retourne TOUS les transferts pour l'Administrateur globalement (Centre de Documents).
     */
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @GetMapping("/all")
    public ResponseEntity<List<TransferHeader>> getAllTransfers() {
        return ResponseEntity.ok(transferService.getAllTransfers());
    }

    /**
     * GET /api/transfers/history/source?userId={id}
     */
    @PreAuthorize("hasAnyRole('MAGASINIER', 'ADMINISTRATEUR')")
    @GetMapping("/history/source")
    public ResponseEntity<List<TransferHeader>> getSourceHistory(@RequestParam Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));
        return ResponseEntity.ok(transferService.getSourceHistory(user));
    }

    /**
     * GET /api/transfers/history/dest?userId={id}
     */
    @PreAuthorize("hasAnyRole('MAGASINIER_DEST', 'ADMINISTRATEUR')")
    @GetMapping("/history/dest")
    public ResponseEntity<List<TransferHeader>> getDestHistory(@RequestParam Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + userId));
        return ResponseEntity.ok(transferService.getDestHistory(user));
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    // RBAC Niv.3 — audit session 3
    @PreAuthorize("hasAnyRole('ACHETEUR','ACHETEUR_INFORMATIQUE','ACHETEUR_BUREAUTIQUE','ACHETEUR_MOBILIER','ACHETEUR_CONSOMMABLE','ACHETEUR_AUTRE','MAGASINIER','MAGASINIER_DEST','COMPTABLE','DAF','DG','RESP_ACHAT','ADMINISTRATEUR')")
    @GetMapping("/{id}/pdf/lto")
    public ResponseEntity<byte[]> getLtoPdf(@PathVariable Long id) {
        byte[] pdfBytes = transferService.generateLtoPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "LTO-" + id + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    // RBAC Niv.3 — audit session 3
    @PreAuthorize("hasAnyRole('ACHETEUR','ACHETEUR_INFORMATIQUE','ACHETEUR_BUREAUTIQUE','ACHETEUR_MOBILIER','ACHETEUR_CONSOMMABLE','ACHETEUR_AUTRE','MAGASINIER','MAGASINIER_DEST','COMPTABLE','DAF','DG','RESP_ACHAT','ADMINISTRATEUR')")
    @GetMapping("/{id}/pdf/lti")
    public ResponseEntity<byte[]> getLtiPdf(@PathVariable Long id) {
        byte[] pdfBytes = transferService.generateLtiPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "LTI-" + id + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}

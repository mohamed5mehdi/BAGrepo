package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class GrcService {

    @Autowired private GrcHeaderRepository grcRepository;
    @Autowired private StockItemRepository stockItemRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private GrnHeaderRepository grnHeaderRepository;
    @Autowired private StatusHistoryRepository historyRepository;

    /**
     * Crée un GRC lié à un GRN ENTRY_COMPLETED.
     * FIX : resolveGrnHeader simplifié — plus de fallback erroné GRN.id → PO.id.
     */
    @Transactional
    public GrcHeader createGrc(GrcHeader grc) {
        // ── BUG FIX #2 — resolveGrnHeader corrigé ────────────────────────────
        if (grc.getGrnHeader() == null || grc.getGrnHeader().getId() == null) {
            throw new IllegalArgumentException("GRC doit référencer un GRN valide (grnHeader.id requis).");
        }

        GrnHeader actualGrn = grnHeaderRepository.findByIdWithLock(grc.getGrnHeader().getId())
                .orElseThrow(() -> new RuntimeException(
                    "GRN introuvable avec l'ID [" + grc.getGrnHeader().getId() + "]. "
                    + "Vérifiez que le GRN existe avant de créer un GRC."));

        // ── GUARD : GRN doit être ENTRY_COMPLETED ─────────────────────────────
        if (actualGrn.getStatus() != GrnStatus.ENTRY_COMPLETED) {
            throw new IllegalStateException(
                "GRC impossible : GRN [" + actualGrn.getId() + "] en statut ["
                + actualGrn.getStatus() + "]. Requis : ENTRY_COMPLETED.");
        }

        // ── GUARD : 1 seul GRC par GRN (Contrôle sécurisé en BD) ─────────────
        if (grcRepository.findByGrnHeader(actualGrn).isPresent()) {
            throw new IllegalStateException(
                "Un GRC existe déjà pour ce GRN [" + actualGrn.getId() + "].");
        }

        grc.setGrnHeader(actualGrn);
        grc.setStatus(GrcStatus.PENDING_APPROVAL);

        // ── Règle BAG ERP : GRC partage le numéro du GRN ─────────────────────
        // Si le GRN n'a pas encore de grnNumber (edge case : GRN créé sans passer
        // par completeGrnEntry), on le génère ici pour éviter toute désynchronisation.
        String ref = actualGrn.getGrnNumber();
        if (ref == null) {
            String yyyyMM = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
            ref = "GRN-" + yyyyMM + "-" + String.format("%05d", actualGrn.getId());
            actualGrn.setGrnNumber(ref);
            grnHeaderRepository.save(actualGrn);
        }
        grc.setGrcNumber(ref);   // même référence — immuable après création

        // ── GUARD : Validation stricte des détails ─────────────────────────────
        List<GrcDetails> details = grc.getDetails() != null ? grc.getDetails() : Collections.emptyList();
        if (details.isEmpty()) {
            throw new IllegalArgumentException("Un GRC doit contenir au moins une ligne de détail.");
        }

        details.forEach(d -> {
            if (d.getGrnDetail() == null || d.getGrnDetail().getId() == null) {
                throw new IllegalArgumentException("Chaque ligne GRC doit référencer une ligne GRN (grnDetail.id requis).");
            }

            // Vérifier que le GrnDetail appartient bien au GRN actuel (anti-injection)
            GrnDetails matchingGrnDetail = actualGrn.getDetails().stream()
                .filter(grnDetail -> grnDetail.getId().equals(d.getGrnDetail().getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "La ligne GRN référencée [" + d.getGrnDetail().getId() + "] n'appartient pas au GRN [" + actualGrn.getId() + "]."));

            int grcQty = d.getAcceptedQuantity() != null ? d.getAcceptedQuantity() : 0;
            if (grcQty <= 0) {
                throw new IllegalArgumentException("La quantité GRC doit être strictement positive pour l'article: " + d.getItemCode());
            }
            if (grcQty > matchingGrnDetail.getAcceptedQuantity()) {
                throw new IllegalArgumentException(
                    "Sur-valorisation bloquée : GRC qté=" + grcQty 
                    + " > GRN accepté=" + matchingGrnDetail.getAcceptedQuantity() 
                    + " pour l'article: " + d.getItemCode());
            }

            if (d.getUnitCost() == null || d.getUnitCost() <= 0) {
                throw new IllegalArgumentException("Le coût unitaire doit être strictement positif pour l'article: " + d.getItemCode());
            }

            d.setGrcHeader(grc);
        });

        grc.setDetails(details);

        GrcHeader saved = grcRepository.save(grc);

        historyRepository.save(new StatusHistory(
            "GrcHeader", saved.getId(),
            null, GrcStatus.PENDING_APPROVAL.name(),
            grc.getProcessedBy(), "GRC créé pour GRN [" + actualGrn.getId() + "]"
        ));

        return saved;
    }

    /**
     * Valide le GRC — Comptable : PENDING_APPROVAL → POSTED.
     * FIXES :
     *  - FIX #3 : null guard sur getDetails()
     *  - FIX #5 : montantHT calculé en cumulant les HT réels
     *  - FIX #6 : valorisation unitaire du stock via warehouse du GRN
     *  - FIX #7 : guard acceptedQuantity GRC ≤ acceptedQuantity GRN (anti sur-valorisation)
     */
    @Transactional
    public GrcHeader validateGrc(Long grcId, User comptable) {
        GrcHeader grc = grcRepository.findByIdWithLock(grcId)
                .orElseThrow(() -> new RuntimeException("GRC introuvable : " + grcId));

        if (grc.getStatus() != GrcStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                "GRC [" + grcId + "] en statut [" + grc.getStatus()
                + "]. Seul PENDING_APPROVAL → POSTED est autorisé.");
        }

        // FIX #3 : null guard
        List<GrcDetails> details = grc.getDetails() != null ? grc.getDetails() : Collections.emptyList();
        if (details.isEmpty()) {
            throw new IllegalStateException("Impossible de valider un GRC sans lignes de détails.");
        }

        BigDecimal totalHT  = BigDecimal.ZERO;
        BigDecimal totalTTC = BigDecimal.ZERO;

        for (GrcDetails detail : details) {
            // ── FIX #7 : Guard métier — intégrité stricte ──────────
            if (detail.getGrnDetail() == null) {
                throw new IllegalStateException("Règle métier violée : Ligne GRC sans référence GRN detail pour l'article [" + detail.getItemCode() + "].");
            }
            if (detail.getGrnDetail().getAcceptedQuantity() == null) {
                throw new IllegalStateException("Quantité acceptée GRN non définie pour l'article: " + detail.getItemCode());
            }

            int grnAccepted = detail.getGrnDetail().getAcceptedQuantity();
            int grcQty = detail.getAcceptedQuantity() != null ? detail.getAcceptedQuantity() : 0;
            if (grcQty <= 0) {
                throw new IllegalArgumentException("Quantité GRC invalide (<= 0) pour l'article: " + detail.getItemCode());
            }
            if (grcQty > grnAccepted) {
                throw new IllegalArgumentException(
                    "Sur-valorisation bloquée pour article [" + detail.getItemCode()
                    + "] : GRC qté=" + grcQty + " > GRN accepté=" + grnAccepted
                );
            }

            if (detail.getUnitCost() == null || detail.getUnitCost() <= 0) {
                throw new IllegalArgumentException("Coût unitaire invalide (<= 0) pour l'article: " + detail.getItemCode());
            }

            BigDecimal unitCost = BigDecimal.valueOf(detail.getUnitCost());
            BigDecimal lineHT = unitCost.multiply(BigDecimal.valueOf(grcQty));
            detail.setTotalCost(lineHT.doubleValue());

            // TVA réelle par ligne (défaut 20% si non renseigné)
            double taxRate = detail.getTaxRate() != null ? detail.getTaxRate() : 20.0;
            if (detail.getTaxRate() == null) detail.setTaxRate(taxRate);
            BigDecimal taxFactor = BigDecimal.ONE.add(
                BigDecimal.valueOf(taxRate).divide(BigDecimal.valueOf(100)));
            BigDecimal lineTTC = lineHT.multiply(taxFactor);
            detail.setMontantTTC(lineTTC.setScale(2, RoundingMode.HALF_UP).doubleValue());

            totalHT  = totalHT.add(lineHT);
            totalTTC = totalTTC.add(lineTTC);

            // FIX #6 : valorisation unitaire du stock
            if (detail.getItemCode() != null && grc.getGrnHeader() != null) {
                updateStockUnitCost(detail.getItemCode(), grc.getGrnHeader(), unitCost);
            }
        }

        BigDecimal finalTTC = totalTTC.setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalHT  = totalHT.setScale(2, RoundingMode.HALF_UP);

        grc.setTotalAmount(finalTTC);
        grc.setStatus(GrcStatus.POSTED);

        GrcHeader posted = grcRepository.save(grc);

        // FIX #8 : generateInvoice idempotent (évite doublon sur retry)
        generateInvoice(posted, finalHT, finalTTC);

        historyRepository.save(new StatusHistory(
            "GrcHeader", posted.getId(),
            GrcStatus.PENDING_APPROVAL.name(), GrcStatus.POSTED.name(),
            comptable, "GRC validé — rapprochement PO/GRN/GRC complété"
        ));

        return posted;
    }

    /** Alias endpoint /grc/{id}/valider — user null (nullable dans StatusHistory). */
    @Transactional
    public GrcHeader validateGrc(Long grcId) {
        return validateGrc(grcId, null);
    }

    /**
     * Approbation finale du GRC — distinct de validateGrc().
     * Flux métier : POSTED → APPROVED (second visas, ex: Responsable Achat).
     * Si votre processus ne requiert qu'un seul niveau, cet état reste
     * disponible pour une extension ultérieure sans régression.
     */
    @Transactional
    public GrcHeader approveGrc(Long grcId) {
        GrcHeader grc = grcRepository.findByIdWithLock(grcId)
                .orElseThrow(() -> new RuntimeException("GRC introuvable : " + grcId));

        // Guard idempotent : POSTED → APPROVED uniquement
        if (grc.getStatus() != GrcStatus.POSTED) {
            throw new IllegalStateException(
                "Approbation impossible : GRC [" + grcId + "] en statut ["
                + grc.getStatus() + "]. Requis : POSTED."
            );
        }

        grc.setStatus(GrcStatus.APPROVED);
        GrcHeader approved = grcRepository.save(grc);

        historyRepository.save(new StatusHistory(
            "GrcHeader", approved.getId(),
            GrcStatus.POSTED.name(), GrcStatus.APPROVED.name(),
            null, "GRC approuvé — circuit complet PO/GRN/GRC/Facture clôturé"
        ));

        return approved;
    }

    // ── Privates ─────────────────────────────────────────────────────────────

    /**
     * FIX #5 : montantHT réel = somme des HT par ligne (pas TTC / 1.20 hardcodé).
     * FIX #6 : cherche le stock dans l'entrepôt du GRN si disponible.
     */
    private void updateStockUnitCost(String itemCode, GrnHeader grn, BigDecimal unitCost) {
        // Architecture BAG ERP : 1 entrepôt principal. Verrouillage pour empêcher l'écrasement concurrent.
        stockItemRepository.findByItemCodeWithLock(itemCode).stream()
            .findFirst()
            .ifPresent(item -> {
                item.setUnitCost(unitCost.doubleValue());
                stockItemRepository.save(item);
            });
    }

    /**
     * FIX #8 — Idempotent : ne crée la facture que si elle n'existe pas encore.
     * Protège contre les doublons sur retry client (ex: double-clic frontend).
     */
    private void generateInvoice(GrcHeader posted, BigDecimal montantHT, BigDecimal montantTTC) {
        if (posted.getGrnHeader() == null) return;
        String invoiceNum = "GRC-" + posted.getId();

        // Guard unicité — si la facture existe déjà (retry), on ne recrée pas
        Optional<Invoice> existing = invoiceRepository.findByInvoiceNumber(invoiceNum);
        if (existing.isPresent()) return;

        Invoice invoice = new Invoice();
        invoice.setGrnHeader(posted.getGrnHeader());
        invoice.setPurchaseOrder(posted.getGrnHeader().getPurchaseOrder());
        invoice.setInvoiceNumber(invoiceNum);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setMontantHT(montantHT);
        invoice.setMontantTTC(montantTTC);
        invoice.setStatus(InvoiceStatus.RECEIVED);
        invoiceRepository.save(invoice);
    }
}

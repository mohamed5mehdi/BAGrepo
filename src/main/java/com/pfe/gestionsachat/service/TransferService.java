package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.exception.InsufficientStockTransferException;
import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.pfe.gestionsachat.dto.transfer.*;

/**
 * TransferService — Cœur du flux LTO/LTI inter-sites.
 *
 * Machine à états :
 *   submitTransfer  → PENDING
 *   shipTransfer    → IN_TRANSIT  (MAGASINIER source, lock pessimiste)
 *   receiveTransfer → RECEIVED    (MAGASINIER_DEST)
 *   cancelTransfer  → CANCELLED   (EMPLOYE auteur ou ADMINISTRATEUR)
 *
 * Risques adressés : RISQUE-01, 03, 04, 05, 06, 07, 08, 09, 11, 12, 13, 14, 21, 27.
 */
@Service
public class TransferService {

    @Autowired private TransferHeaderRepository transferRepo;
    @Autowired private StockItemRepository stockItemRepo;
    @Autowired private StockMovementRepository movementRepo;
    @Autowired private StatusHistoryRepository historyRepo;
    @Autowired private WarehouseRepository warehouseRepo;

    @Autowired private PdfGenerationService pdfGenerationService;

    // ─────────────────────────────────────────────────────────────────────────
    // SUBMIT — PENDING
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Soumet une demande de transfert. Transition : ∅ → PENDING.
     *
     * MAJEUR-02 (corrigé) : validation stock STRICTE sans lock (fast-fail légitime).
     * Le frontend ne propose que les articles visibles (quantityAvailable > 0) et
     * bloque la saisie au-delà du stock affiché — cette vérification est cohérente
     * avec l'UX. Le lock définitif est acquis au shipTransfer (double-check).
     * Contrainte : toutes les lignes doivent appartenir au même warehouseSource (RISQUE-21).
     */
    private boolean isMagasinierOrAdmin(Role role) {
        return role == Role.MAGASINIER || role == Role.MAGASINIER_DEST || 
               role == Role.ADMINISTRATEUR;
    }

    @Transactional
    public List<TransferHeader> submitBulkTransfers(BulkTransferRequest request, User requester) {
        if (!isMagasinierOrAdmin(requester.getRole())) {
            throw new SecurityException("Seul un MAGASINIER ou ADMINISTRATEUR peut soumettre un transfert.");
        }

        Warehouse destWarehouse = warehouseRepo.findById(request.getDestWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Entrepôt de destination introuvable."));

        Map<Long, List<TransferItemReq>> groupedBySource = request.getItems().stream()
                .collect(Collectors.groupingBy(TransferItemReq::getSourceWarehouseId));

        List<TransferHeader> createdHeaders = new ArrayList<>();

        for (Map.Entry<Long, List<TransferItemReq>> entry : groupedBySource.entrySet()) {
            Long sourceWarehouseId = entry.getKey();
            List<TransferItemReq> itemsReq = entry.getValue();

            Warehouse sourceWarehouse = warehouseRepo.findById(sourceWarehouseId)
                    .orElseThrow(() -> new IllegalArgumentException("Entrepôt source introuvable."));

            TransferHeader header = new TransferHeader();
            header.setWarehouseSource(sourceWarehouse);
            header.setWarehouseDest(destWarehouse);
            
            List<TransferLine> lines = new ArrayList<>();
            for(TransferItemReq itemReq : itemsReq) {
                TransferLine line = new TransferLine();
                StockItem stockItem = stockItemRepo.findById(itemReq.getStockItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Article introuvable."));
                line.setStockItem(stockItem);
                line.setQuantityRequested(itemReq.getQuantityRequested());
                lines.add(line);
            }
            header.setLines(lines);

            createdHeaders.add(submitTransfer(header, requester));
        }
        return createdHeaders;
    }

    @Transactional
    public TransferHeader submitTransfer(TransferHeader header, User requester) {

        // Guard 1 : rôle
        if (!isMagasinierOrAdmin(requester.getRole())) {
            throw new SecurityException("Seul un MAGASINIER ou ADMINISTRATEUR peut soumettre un transfert.");
        }

        // Guard 2 : warehouseSource ≠ warehouseDest
        if (header.getWarehouseSource() == null || header.getWarehouseDest() == null)
            throw new IllegalArgumentException("Le warehouse source et destination sont obligatoires.");
        if (header.getWarehouseSource().getId().equals(header.getWarehouseDest().getId()))
            throw new IllegalArgumentException("Le warehouse source et destination doivent être différents.");

        // Guard 2 : au moins une ligne
        if (header.getLines() == null || header.getLines().isEmpty())
            throw new IllegalArgumentException("Au moins une ligne de transfert est requise.");

        // Tri par stockItem.id croissant pour éviter les deadlocks au moment du flush Hibernate (RISQUE-05)
        List<TransferLine> sortedLines = header.getLines().stream()
            .sorted(Comparator.comparing(l -> l.getStockItem().getId()))
            .toList();

        // Guard 3 : validation cohérence warehouse + pré-check stock (RISQUE-21, RISQUE-06)
        for (TransferLine line : sortedLines) {
            if (line.getQuantityRequested() == null || line.getQuantityRequested() <= 0)
                throw new IllegalArgumentException("La quantité demandée doit être supérieure à zéro.");

            StockItem item = stockItemRepo.findById(line.getStockItem().getId())
                .filter(i -> i.getWarehouse().getId().equals(header.getWarehouseSource().getId()))
                .orElseThrow(() -> new IllegalArgumentException(
                    "Article avec ID [" + line.getStockItem().getId()
                    + "] introuvable dans le warehouse source."));

            // Pré-validation sans lock — informatif uniquement (RISQUE-06)
            if (item.getQuantityAvailable() < line.getQuantityRequested())
                throw new InsufficientStockTransferException(
                    item.getItemCode(), item.getQuantityAvailable(), line.getQuantityRequested());

            // CRITIQUE : Réservation mathématique du stock au lieu d'une simple vérification
            item.setQuantityAvailable(item.getQuantityAvailable() - line.getQuantityRequested());
            item.setQuantityReserved((item.getQuantityReserved() != null ? item.getQuantityReserved() : 0) + line.getQuantityRequested());
            stockItemRepo.saveAndFlush(item); // CRITIQUE-01 : force le versioning immédiat pour cohérence L1 cache

            // Snapshot warehouse source immuable (RISQUE-27)
            line.setWarehouseSourceSnapshotId(header.getWarehouseSource().getId());
            line.setStockItem(item);
            line.setHeader(header);
        }

        header.setRequestedBy(requester);
        header.setStatus(TransferStatus.PENDING);
        header.setCreatedAt(LocalDateTime.now());

        TransferHeader saved = transferRepo.save(header);
        logHistory(saved, null, TransferStatus.PENDING, requester, "Demande de transfert soumise");
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHIP — IN_TRANSIT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Expédie le transfert. Transition : PENDING → IN_TRANSIT.
     *
     * Locks pessimistes dans l'ordre croissant des stockItem.id (RISQUE-05 : anti-deadlock).
     * Re-vérification stock avec lock (RISQUE-06 : garde-fou final).
     * Décrémente le stock source + génère les StockMovements TRANSFER_OUT (RISQUE-01).
     */
    @Transactional
    public TransferHeader shipTransfer(Long headerId, User magasinier, TransferShipRequest request) {

        // Guards rôle et warehouse
        if (!isMagasinierOrAdmin(magasinier.getRole()))
            throw new SecurityException("Seul un MAGASINIER ou ADMINISTRATEUR peut expédier un transfert.");
        if (magasinier.getRole() != Role.ADMINISTRATEUR && magasinier.getWarehouse() == null)
            throw new IllegalStateException("Magasinier non assigné à un entrepôt.");

        // MAJEUR-01 : JOIN FETCH lignes + stockItems → tri déterministe, zéro N+1, proxy ids fiables
        TransferHeader header = transferRepo.findByIdWithLinesAndItems(headerId)
            .orElseThrow(() -> new RuntimeException("Transfert introuvable : " + headerId));

        // Guard statut (RISQUE-11 : idempotence garantie par machine à états)
        if (header.getStatus() != TransferStatus.PENDING)
            throw new IllegalStateException(
                "Transition impossible : " + header.getStatus() + " → IN_TRANSIT");

        // Guard warehouse source (sécurité)
        if (magasinier.getRole() != Role.ADMINISTRATEUR && !magasinier.getWarehouse().getId().equals(header.getWarehouseSource().getId()))
            throw new SecurityException("Ce transfert ne concerne pas votre entrepôt.");

        // Tri par stockItem.id croissant — ordre déterministe pour anti-deadlock (RISQUE-05)
        List<TransferLine> sortedLines = header.getLines().stream()
            .sorted(Comparator.comparing(l -> l.getStockItem().getId()))
            .toList();

        // Génération du numéro LTO avant la boucle pour l'audit trail (RISQUE-01)
        String lto = "LTO-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-" + String.format("%05d", header.getId());
        header.setLtoNumber(lto);

        for (TransferLine line : sortedLines) {
            Integer shippedQty = request.getLines().stream()
                .filter(l -> l.getLineId().equals(line.getId()))
                .map(LineQty::getQuantity)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Quantité expédiée manquante pour la ligne " + line.getId()));
            
            if (shippedQty < 0)
                throw new IllegalArgumentException("La quantité expédiée ne peut être négative.");
                
            line.setQuantityShipped(shippedQty);

            // SELECT FOR UPDATE — lock pessimiste (RISQUE-05, RISQUE-06)
            StockItem src = stockItemRepo.findByItemCodeAndWarehouseIdWithLock(
                line.getStockItem().getItemCode(),
                header.getWarehouseSource().getId()
            ).orElseThrow(() -> new RuntimeException("Article introuvable en stock source."));

            // Vérification définitive avec lock (RISQUE-06)
            // L'article ayant déjà été réservé lors du submit, on consomme la réservation
            int reserved = src.getQuantityReserved() != null ? src.getQuantityReserved() : 0;
            if (reserved < line.getQuantityRequested())
                throw new InsufficientStockTransferException(
                    src.getItemCode(), reserved, line.getQuantityRequested());

            // Libérer la réservation complète initiale
            src.setQuantityReserved(Math.max(0, reserved - line.getQuantityRequested()));
            
            // Ajuster le stock disponible si la quantité expédiée diffère de la quantité demandée
            int diff = line.getQuantityRequested() - shippedQty;
            if (diff < 0) {
                // On expédie PLUS que demandé : vérifier qu'il y a assez de dispo
                int extraNeeded = -diff;
                if (src.getQuantityAvailable() < extraNeeded) {
                    throw new InsufficientStockTransferException(src.getItemCode(), src.getQuantityAvailable(), extraNeeded);
                }
                src.setQuantityAvailable(src.getQuantityAvailable() - extraNeeded);
            } else if (diff > 0) {
                // On expédie MOINS que demandé : restituer la différence au disponible
                src.setQuantityAvailable(src.getQuantityAvailable() + diff);
            }

            // CRITIQUE-01 : saveAndFlush() — force la réconciliation @Version avant
            // la ligne suivante, évite ObjectOptimisticLockingFailureException
            // causée par un proxy L1-cache stale chargé en début de transaction.
            stockItemRepo.saveAndFlush(src);

            // Mouvement TRANSFER_OUT avec snapshot warehouse et référence document correcte (RISQUE-01)
            persistMovement(src, header.getWarehouseSource(),
                MovementType.TRANSFER_OUT, shippedQty, lto);
        }

        header.setStatus(TransferStatus.IN_TRANSIT);
        header.setShippedAt(LocalDateTime.now());

        TransferHeader saved = transferRepo.save(header);
        logHistory(saved, TransferStatus.PENDING, TransferStatus.IN_TRANSIT, magasinier,
            "Expédition confirmée — LTO : " + lto);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RECEIVE — RECEIVED
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Valide la réception. Transition : IN_TRANSIT → RECEIVED.
     *
     * Création auto du StockItem destination si l'article n'existe pas encore dans cet entrepôt.
     * Idempotence garantie par machine à états (RISQUE-11).
     */
    @Transactional
    public TransferHeader receiveTransfer(Long headerId, User magasinierDest, TransferReceiveRequest request) {

        // Guards rôle et warehouse
        if (!isMagasinierOrAdmin(magasinierDest.getRole()))
            throw new SecurityException("Seul un MAGASINIER ou ADMINISTRATEUR peut valider la réception.");
        if (magasinierDest.getRole() != Role.ADMINISTRATEUR && magasinierDest.getWarehouse() == null)
            throw new IllegalStateException("Magasinier destination non assigné à un entrepôt.");

        // MAJEUR-01 : JOIN FETCH lignes + stockItems → tri déterministe, zéro N+1
        TransferHeader header = transferRepo.findByIdWithLinesAndItems(headerId)
            .orElseThrow(() -> new RuntimeException("Transfert introuvable : " + headerId));

        // Guard statut (RISQUE-11)
        if (header.getStatus() != TransferStatus.IN_TRANSIT)
            throw new IllegalStateException(
                "Transition impossible : " + header.getStatus() + " → RECEIVED");

        // Guard warehouse destination
        if (magasinierDest.getRole() != Role.ADMINISTRATEUR && !magasinierDest.getWarehouse().getId().equals(header.getWarehouseDest().getId()))
            throw new SecurityException("Ce transfert ne concerne pas votre entrepôt destination.");

        // MAJEUR-01 : JOIN FETCH déjà fait — tri par id croissant déterministe, anti-deadlock (RISQUE-05)
        List<TransferLine> sortedLines = header.getLines().stream()
            .sorted(Comparator.comparing(l -> l.getStockItem().getId()))
            .toList();

        // Génération du numéro LTI avant la boucle pour l'audit trail (RISQUE-01)
        String lti = "LTI-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-" + String.format("%05d", header.getId());
        header.setLtiNumber(lti);

        for (TransferLine line : sortedLines) {
            Integer receivedQty = request.getLines().stream()
                .filter(l -> l.getLineId().equals(line.getId()))
                .map(LineQty::getQuantity)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Quantité reçue manquante pour la ligne " + line.getId()));
            
            if (receivedQty < 0)
                throw new IllegalArgumentException("La quantité reçue ne peut être négative.");
                
            line.setQuantityReceived(receivedQty);

            String itemCode = line.getStockItem().getItemCode();
            Long destWarehouseId = header.getWarehouseDest().getId();

            // Cherche le StockItem destination avec lock pessimiste.
            // La machine à états garantit l'unicité du receiver sur CE transfert,
            // mais deux TRANSFERTS DISTINCTS peuvent converger vers le même dest
            // avec le même article simultanément → gap non couvert par SELECT FOR UPDATE
            // sur une ligne inexistante (PostgreSQL ne pose pas de gap lock ici).
            // Pattern robuste : find-or-insert avec re-read après violation contrainte.
            StockItem dest;
            try {
                dest = stockItemRepo
                    .findByItemCodeAndWarehouseIdWithLock(itemCode, destWarehouseId)
                    .orElseGet(() -> createStockItemAtDest(line.getStockItem(), header.getWarehouseDest()));
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                dest = stockItemRepo
                    .findByItemCodeAndWarehouseIdWithLock(itemCode, destWarehouseId)
                    .orElseThrow(() -> new RuntimeException("Erreur de concurrence sur l'article " + itemCode));
            }

            dest.setQuantityAvailable(dest.getQuantityAvailable() + receivedQty);
            // CRITIQUE-01 : saveAndFlush() — réconciliation @Version avant ligne suivante
            stockItemRepo.saveAndFlush(dest);

            // Mouvement TRANSFER_IN avec snapshot warehouse et référence document correcte (LTI) (RISQUE-01)
            persistMovement(dest, header.getWarehouseDest(),
                MovementType.TRANSFER_IN, receivedQty,
                lti);
        }

        header.setStatus(TransferStatus.RECEIVED);
        header.setReceivedAt(LocalDateTime.now());

        TransferHeader saved = transferRepo.save(header);
        logHistory(saved, TransferStatus.IN_TRANSIT, TransferStatus.RECEIVED, magasinierDest,
            "Réception validée — LTI : " + lti + " (réf. LTO : " + header.getLtoNumber() + ")");
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL — CANCELLED
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Annule le transfert. Transition : PENDING → CANCELLED uniquement.
     * Aucun rollback stock (aucun mouvement n'a eu lieu avant IN_TRANSIT).
     * Autorisé à l'auteur (EMPLOYE) ou à l'ADMINISTRATEUR (RISQUE-12).
     */
    @Transactional
    public TransferHeader cancelTransfer(Long headerId, User user) {
        TransferHeader header = transferRepo.findById(headerId)
            .orElseThrow(() -> new RuntimeException("Transfert introuvable : " + headerId));

        if (header.getStatus() != TransferStatus.PENDING)
            throw new IllegalStateException(
                "Annulation impossible : le transfert est déjà " + header.getStatus());

        // Guard auteur ou administrateur (RISQUE-12)
        if (!user.getOidUser().equals(header.getRequestedBy().getOidUser())
            && user.getRole() != Role.ADMINISTRATEUR)
            throw new SecurityException("Vous n'êtes pas autorisé à annuler ce transfert.");

        // Tri par stockItem.id croissant pour éviter les deadlocks au moment du flush Hibernate (RISQUE-05)
        List<TransferLine> sortedLines = header.getLines().stream()
            .sorted(Comparator.comparing(l -> l.getStockItem().getId()))
            .toList();

        // Restitution du stock réservé vers le stock disponible
        for (TransferLine line : sortedLines) {
            StockItem item = stockItemRepo.findById(line.getStockItem().getId())
                .orElseThrow(() -> new RuntimeException("Article introuvable : " + line.getStockItem().getId()));
            
            int reserved = item.getQuantityReserved() != null ? item.getQuantityReserved() : 0;
            item.setQuantityReserved(Math.max(0, reserved - line.getQuantityRequested()));
            item.setQuantityAvailable((item.getQuantityAvailable() != null ? item.getQuantityAvailable() : 0) + line.getQuantityRequested());
            stockItemRepo.saveAndFlush(item); // CRITIQUE-01 : force le versioning immédiat pour cohérence L1 cache
        }

        header.setStatus(TransferStatus.CANCELLED);
        TransferHeader saved = transferRepo.save(header);
        logHistory(saved, TransferStatus.PENDING, TransferStatus.CANCELLED, user, "Transfert annulé");
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LECTURES
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TransferHeader> getMyTransfers(User user) {
        return transferRepo.findByRequestedBy(user);
    }

    /**
     * File PENDING pour le MAGASINIER source (onglet "Transferts à expédier").
     * RISQUE-14 : guard explicite si warehouse null.
     */
    @Transactional(readOnly = true)
    public List<TransferHeader> getSourceQueue(User magasinier) {
        if (magasinier.getWarehouse() == null)
            throw new IllegalStateException("Magasinier non assigné à un entrepôt.");
        return transferRepo.findByWarehouseSourceAndStatus(
            magasinier.getWarehouse(), TransferStatus.PENDING);
    }

    /**
     * File IN_TRANSIT pour le MAGASINIER_DEST (vue réception).
     * RISQUE-14 : guard explicite si warehouse null.
     */
    @Transactional(readOnly = true)
    public List<TransferHeader> getDestQueue(User magasinierDest) {
        if (magasinierDest.getWarehouse() == null)
            throw new IllegalStateException("Magasinier destination non assigné à un entrepôt.");
        return transferRepo.findByWarehouseDestAndStatus(
            magasinierDest.getWarehouse(), TransferStatus.IN_TRANSIT);
    }

    /**
     * Retourne tous les transferts (global) pour l'Admin dans le Centre de Documents.
     */
    @Transactional(readOnly = true)
    public List<TransferHeader> getAllTransfers() {
        return transferRepo.findAll();
    }

    /**
     * Historique des expéditions pour le MAGASINIER source (LTO générés).
     */
    @Transactional(readOnly = true)
    public List<TransferHeader> getSourceHistory(User magasinier) {
        if (magasinier.getWarehouse() == null)
            throw new IllegalStateException("Magasinier non assigné à un entrepôt.");
        return transferRepo.findByWarehouseSourceAndStatusIn(
            magasinier.getWarehouse(), List.of(TransferStatus.IN_TRANSIT, TransferStatus.RECEIVED));
    }

    /**
     * Historique des réceptions pour le MAGASINIER_DEST (LTI générés).
     */
    @Transactional(readOnly = true)
    public List<TransferHeader> getDestHistory(User magasinierDest) {
        if (magasinierDest.getWarehouse() == null)
            throw new IllegalStateException("Magasinier destination non assigné à un entrepôt.");
        return transferRepo.findByWarehouseDestAndStatus(
            magasinierDest.getWarehouse(), TransferStatus.RECEIVED);
    }

    /**
     * Génération sécurisée (isolée) du LTO PDF avec chargement des lazy fields.
     */
    @Transactional(readOnly = true)
    public byte[] generateLtoPdf(Long id) {
        TransferHeader header = transferRepo.findByIdWithLinesAndItems(id)
            .orElseThrow(() -> new IllegalArgumentException("Transfert introuvable : " + id));
        return pdfGenerationService.generateLtoPdf(header);
    }

    /**
     * Génération sécurisée (isolée) du LTI PDF avec chargement des lazy fields.
     */
    @Transactional(readOnly = true)
    public byte[] generateLtiPdf(Long id) {
        TransferHeader header = transferRepo.findByIdWithLinesAndItems(id)
            .orElseThrow(() -> new IllegalArgumentException("Transfert introuvable : " + id));
        return pdfGenerationService.generateLtiPdf(header);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Crée un StockItem vierge à destination pour un article inexistant dans ce warehouse.
     *
     * CRITIQUE-02 : pas de REQUIRES_NEW nécessaire ici — la machine à états garantit
     * qu'un seul thread peut appeler receiveTransfer sur un transfer IN_TRANSIT donné.
     * Le lock pessimiste sur findByItemCodeAndWarehouseIdWithLock protège la création.
     *
     * CRITIQUE-03 : null guard sur locationCode — si null, on utilise itemCode comme base.
     */
    private StockItem createStockItemAtDest(StockItem source, Warehouse dest) {
        StockItem newItem = new StockItem();
        newItem.setItemCode(source.getItemCode());
        newItem.setItemName(source.getItemName());
        newItem.setCategory(source.getCategory());
        newItem.setWarehouse(dest);
        newItem.setQuantityAvailable(0);
        newItem.setQuantityReserved(0);
        // CRITIQUE-03 : null guard — locationCode peut être null pour les articles non localisés
        String baseCode = source.getLocationCode() != null
            ? source.getLocationCode() : source.getItemCode();
        newItem.setLocationCode(baseCode + "-W" + dest.getId());
        return stockItemRepo.save(newItem);
    }

    /** Persiste un StockMovement avec snapshot warehouse (RISQUE-01). */
    private void persistMovement(StockItem item, Warehouse warehouse,
                                  MovementType type, Integer qty, String ref) {
        StockMovement mv = new StockMovement();
        mv.setStockItem(item);
        mv.setWarehouse(warehouse);          // snapshot immuable (RISQUE-01)
        mv.setMovementType(type);
        mv.setQuantity(qty);
        mv.setMovementDate(LocalDateTime.now());
        mv.setReferenceDocument(ref);
        movementRepo.save(mv);
    }

    /** Enregistre une ligne dans l'historique de statut (audit trail). */
    private void logHistory(TransferHeader h, TransferStatus from, TransferStatus to,
                             User user, String comment) {
        historyRepo.save(new StatusHistory(
            "TransferHeader",
            h.getId(),
            from != null ? from.name() : null,
            to.name(),
            user,
            comment
        ));
    }
}

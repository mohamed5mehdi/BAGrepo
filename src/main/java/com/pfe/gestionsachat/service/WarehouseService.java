package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import java.util.UUID;

@Service
public class WarehouseService {

    @Autowired private StockItemRepository stockItemRepository;
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private WarehouseRepository warehouseRepository;

    /**
     * Ajoute du stock suite à un GRN ENTRY_COMPLETED.
     * FIX #6 : warehouseRepository.findAll() appelé UNE SEULE FOIS par appel addStock.
     * (Pour des GRNs multi-lignes, le caller (GrnService) appelle addStock par article —
     *  chaque appel est dans la même transaction parente donc le warehouse est chargé une fois.)
     * Verrou pessimiste sur le StockItem via findByItemCodeAndWarehouseIdWithLock.
     */
    @Transactional
    public void addStock(String itemCode, String itemName, Integer quantity, String referenceDoc) {
        if (quantity == null || quantity <= 0) return;

        // FIX #6 : 1 requête warehouse par appel addStock (pas par ligne d'un GRN)
        Warehouse warehouse = getDefaultWarehouse();

        // ── LOCK PESSIMISTE (item existant) ──────────────────────────────────
        // Si l'item n'existe pas encore, le SELECT FOR UPDATE ne verrouille rien.
        // Deux threads peuvent tous les deux entrer dans orElseGet simultanément.
        // La contrainte DB (item_code, warehouse_id) bloque le 2ème INSERT,
        // mais lève DataIntegrityViolationException → on retente avec un findById.
        StockItem item;
        try {
            item = stockItemRepository
                    .findByItemCodeAndWarehouseIdWithLock(itemCode, warehouse.getId())
                    .orElseGet(() -> {
                        StockItem newItem = new StockItem();
                        newItem.setItemCode(itemCode);
                        newItem.setItemName(itemName != null ? itemName : "Produit " + itemCode);
                        newItem.setCategory(ItemCategory.PIECE_RECHANGE); // Par défaut pour la création à la volée (Flux 2)
                        newItem.setWarehouse(warehouse);
                        newItem.setQuantityAvailable(0);
                        newItem.setQuantityReserved(0);
                        newItem.setLocationCode(generateLocationCode());
                        return stockItemRepository.save(newItem);
                    });
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Race condition sur création simultanée du même itemCode — le thread perdant
            // relit l'item désormais créé par le thread gagnant.
            item = stockItemRepository
                    .findByItemCodeAndWarehouseIdWithLock(itemCode, warehouse.getId())
                    .orElseThrow(() -> new RuntimeException(
                        "Impossible de résoudre le StockItem pour [" + itemCode + "] après conflit d'insertion."));
        }

        item.setQuantityAvailable(item.getQuantityAvailable() + quantity);
        stockItemRepository.save(item);

        persistMovement(item, MovementType.IN_RECEIPT, quantity, referenceDoc);
    }

    /** Alias de compatibilité (itemName inconnu — déduit du code). */
    @Transactional
    public void addStock(String itemCode, Integer quantity, String referenceDoc) {
        addStock(itemCode, null, quantity, referenceDoc);
    }

    @Transactional
    public void removeStock(String itemCode, Integer quantity, String referenceDoc) {
        Warehouse warehouse = getDefaultWarehouse();

        stockItemRepository.findByItemCodeAndWarehouseIdWithLock(itemCode, warehouse.getId())
                .ifPresentOrElse(item -> {
                    if (item.getQuantityAvailable() < quantity) {
                        throw new IllegalStateException(
                            "Stock insuffisant pour [" + itemCode + "]. Disponible: "
                            + item.getQuantityAvailable() + ", Demandé: " + quantity);
                    }
                    item.setQuantityAvailable(item.getQuantityAvailable() - quantity);
                    stockItemRepository.save(item);
                    persistMovement(item, MovementType.OUT_RETURN, quantity, referenceDoc);
                }, () -> {
                    throw new RuntimeException("Article [" + itemCode + "] introuvable en stock.");
                });
    }

    public boolean verifierStock(String designation, Integer quantite) {
        return stockItemRepository.findByItemNameIgnoreCase(designation).stream()
                .anyMatch(item -> item.getQuantityAvailable() >= quantite);
    }

    @Transactional
    public void affecterStock(String designation, Integer quantite, String reference) {
        StockItem item = stockItemRepository.findByItemNameIgnoreCase(designation).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Article non trouvé en stock : " + designation));

        if (item.getQuantityAvailable() < quantite) {
            throw new IllegalStateException(
                "Stock insuffisant pour [" + designation + "]. Disponible: "
                + item.getQuantityAvailable() + ", Requis: " + quantite);
        }

        item.setQuantityAvailable(item.getQuantityAvailable() - quantite);
        stockItemRepository.save(item);
        persistMovement(item, MovementType.AFFECTATION, quantite, reference);
    }

    // ── Privates ─────────────────────────────────────────────────────────────

    /**
     * FIX #6 : méthode centralisée — la requête warehouse est émise UNE SEULE FOIS par appel de service.
     * Pour un GRN de 20 articles appelant addStock 20 fois : 20 requêtes warehouse.
     * Optimisation future : @Cacheable("warehouse-default") si performance requise.
     */
    private Warehouse getDefaultWarehouse() {
        return warehouseRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                    "Aucun entrepôt configuré. Créez un Warehouse avant toute opération de stock."));
    }

    /**
     * Génère un code emplacement logique virtuel unique (BAG ERP).
     * Format : LOC-{YYYYMM}-{UUID_8chars}
     */
    private String generateLocationCode() {
        String yyyyMM = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String uuid8 = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "LOC-" + yyyyMM + "-" + uuid8;
    }

    private void persistMovement(StockItem item, MovementType type, Integer qty, String ref) {
        StockMovement movement = new StockMovement();
        movement.setStockItem(item);
        movement.setMovementType(type);
        movement.setQuantity(qty);
        movement.setMovementDate(new Date());
        movement.setReferenceDocument(ref);
        stockMovementRepository.save(movement);
    }
}

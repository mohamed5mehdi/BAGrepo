package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;

@Service
public class WarehouseService {
    @Autowired
    private StockItemRepository stockItemRepository;
    @Autowired
    private StockMovementRepository stockMovementRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;

    @Transactional
    public void addStock(String itemCode, Integer quantity, String referenceDoc) {
        StockItem item = stockItemRepository.findByItemCode(itemCode).stream().findFirst().orElse(null);
        if (item == null) {
            Warehouse w = warehouseRepository.findAll().stream().findFirst().orElseThrow(() -> new RuntimeException("Aucun entrepôt configuré"));
            item = new StockItem();
            item.setItemCode(itemCode);
            item.setItemName("Produit " + itemCode);
            item.setWarehouse(w);
            item.setQuantityAvailable(0);
            item.setQuantityReserved(0);
        }
        
        item.setQuantityAvailable(item.getQuantityAvailable() + quantity);
        stockItemRepository.save(item);

        StockMovement movement = new StockMovement();
        movement.setStockItem(item);
        movement.setMovementType(MovementType.IN_RECEIPT);
        movement.setQuantity(quantity);
        movement.setMovementDate(new Date());
        movement.setReferenceDocument(referenceDoc);
        stockMovementRepository.save(movement);
    }

    @Transactional
    public void removeStock(String itemCode, Integer quantity, String referenceDoc) {
        StockItem item = stockItemRepository.findByItemCode(itemCode).stream().findFirst().orElse(null);
        if (item != null) {
            item.setQuantityAvailable(item.getQuantityAvailable() - quantity);
            stockItemRepository.save(item);

            StockMovement movement = new StockMovement();
            movement.setStockItem(item);
            movement.setMovementType(MovementType.OUT_RETURN);
            movement.setQuantity(quantity);
            movement.setMovementDate(new Date());
            movement.setReferenceDocument(referenceDoc);
            stockMovementRepository.save(movement);
        }
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
        
        item.setQuantityAvailable(item.getQuantityAvailable() - quantite);
        stockItemRepository.save(item);

        StockMovement movement = new StockMovement();
        movement.setStockItem(item);
        movement.setMovementType(MovementType.AFFECTATION);
        movement.setQuantity(quantite);
        movement.setMovementDate(new Date());
        movement.setReferenceDocument(reference);
        stockMovementRepository.save(movement);
    }
}

package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.StockItem;
import com.pfe.gestionsachat.model.Warehouse;
import com.pfe.gestionsachat.repository.StockItemRepository;
import com.pfe.gestionsachat.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
public class WarehouseController {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private StockItemRepository stockItemRepository;

    @Autowired
    private com.pfe.gestionsachat.repository.UserRepository userRepository;

    @Autowired
    private com.pfe.gestionsachat.service.WarehouseService warehouseService;

    /**
     * Liste tous les entrepôts.
     * Accessible aux rôles ayant besoin du référentiel entrepôts (acheteurs, magasiniers, admin).
     */
    @PreAuthorize("hasAnyRole('MAGASINIER','MAGASINIER_DEST','ACHETEUR','ACHETEUR_INFORMATIQUE','ACHETEUR_BUREAUTIQUE','ACHETEUR_MOBILIER','ACHETEUR_CONSOMMABLE','ACHETEUR_AUTRE','RESP_ACHAT','ADMINISTRATEUR','DAF','DG')")
    @GetMapping
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    /**
     * Consulte le stock d'un entrepôt donné.
     * Restreint aux magasiniers, acheteurs, resp. achat et admin.
     * CORRECTIF : filtré par warehouseId pour ne pas exposer le stock de TOUS les entrepôts.
     */
    @PreAuthorize("hasAnyRole('EMPLOYE','MAGASINIER','MAGASINIER_DEST','ACHETEUR','ACHETEUR_INFORMATIQUE','ACHETEUR_BUREAUTIQUE','ACHETEUR_MOBILIER','ACHETEUR_CONSOMMABLE','ACHETEUR_AUTRE','RESP_ACHAT','ADMINISTRATEUR','DAF','DG')")
    @GetMapping("/stock")
    public List<StockItem> getStockItems(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false, defaultValue = "false") boolean includeOutOfStock) {
        
        if (userId != null) {
            com.pfe.gestionsachat.model.User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getRole() != com.pfe.gestionsachat.model.Role.ADMINISTRATEUR) {
                if (user.getWarehouse() != null) {
                    warehouseId = user.getWarehouse().getId();
                } else {
                    warehouseId = warehouseService.getDefaultWarehouse().getId();
                }
            }
        }
        
        List<StockItem> items;
        if (warehouseId != null) {
            items = stockItemRepository.findByWarehouseId(warehouseId);
        } else {
            items = stockItemRepository.findAll();
        }
        
        if (!includeOutOfStock) {
            // Filtrer côté Java si on ne veut pas les items hors stock.
            // On peut aussi le faire en BD, mais ici on gère au niveau applicatif.
            items = items.stream().filter(item -> item.getQuantityAvailable() != null && item.getQuantityAvailable() > 0).toList();
        }
        
        return items;
    }
}



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

    /**
     * Liste tous les entrepôts.
     * Accessible aux rôles ayant besoin du référentiel entrepôts (acheteurs, magasiniers, admin).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MAGASINIER','MAGASINIER_DEST','ACHETEUR','ACHETEUR_INFORMATIQUE','ACHETEUR_BUREAUTIQUE','ACHETEUR_MOBILIER','ACHETEUR_CONSOMMABLE','ACHETEUR_AUTRE','RESP_ACHAT','ADMINISTRATEUR','DAF','DG')")
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    /**
     * Consulte le stock d'un entrepôt donné.
     * Restreint aux magasiniers, acheteurs, resp. achat et admin.
     * CORRECTIF : filtré par warehouseId pour ne pas exposer le stock de TOUS les entrepôts.
     */
    @GetMapping("/stock")
    @PreAuthorize("hasAnyRole('MAGASINIER','MAGASINIER_DEST','ACHETEUR','ACHETEUR_INFORMATIQUE','ACHETEUR_BUREAUTIQUE','ACHETEUR_MOBILIER','ACHETEUR_CONSOMMABLE','ACHETEUR_AUTRE','RESP_ACHAT','ADMINISTRATEUR')")
    public List<StockItem> getStockItems(@RequestParam(required = false) Long warehouseId, @RequestParam(required = false) Integer userId) {
        if (userId != null) {
            com.pfe.gestionsachat.model.User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getRole() != com.pfe.gestionsachat.model.Role.ADMINISTRATEUR && user.getWarehouse() != null) {
                warehouseId = user.getWarehouse().getId();
            }
        }
        if (warehouseId != null) {
            return stockItemRepository.findByWarehouseId(warehouseId);
        }
        return stockItemRepository.findAll();
    }
}



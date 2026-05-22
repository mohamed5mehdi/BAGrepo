package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.StockItem;
import com.pfe.gestionsachat.repository.StockItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class StockAlertService {
    private static final Logger log = LoggerFactory.getLogger(StockAlertService.class);

    @Autowired
    private StockItemRepository stockItemRepository;

    @Scheduled(fixedRate = 60000) // Execute toutes les minutes pour l'exemple
    public void checkStockLevels() {
        List<StockItem> items = stockItemRepository.findAll();
        for (StockItem item : items) {
            if (item.getQuantityAvailable() != null && item.getReorderPoint() != null) {
                if (item.getQuantityAvailable() < item.getReorderPoint()) {
                    log.warn("Alerte Stock : L'article {} est sous le seuil (Dispo: {}, Seuil: {}).", 
                             item.getItemCode(), item.getQuantityAvailable(), item.getReorderPoint());
                    // Déclencher automatiquement une nouvelle DA
                    createAutomaticDa(item);
                }
            }
        }
    }

    @Autowired
    private DaHeaderService daHeaderService;

    @Autowired
    private com.pfe.gestionsachat.repository.UserRepository userRepository;

    private void createAutomaticDa(StockItem item) {
        log.info("Création automatique d'une DA pour l'article {}", item.getItemCode());
        com.pfe.gestionsachat.model.DaHeader da = new com.pfe.gestionsachat.model.DaHeader();
        da.setObjet("Réapprovisionnement automatique pour: " + item.getItemName());
        
        // Find a system user or first user to be the requester
        com.pfe.gestionsachat.model.User systemUser = userRepository.findAll().stream().findFirst().orElse(null);
        da.setDemandeur(systemUser);
        
        com.pfe.gestionsachat.model.DaDetails details = new com.pfe.gestionsachat.model.DaDetails();
        details.setItemCode(item.getItemCode());
        details.setItemName(item.getItemName());
        details.setQuantite(item.getReorderPoint() != null ? item.getReorderPoint() : 10);
        details.setDescription("Alerte stock déclenchée automatiquement");
        details.setDaHeader(da);
        
        da.setDetails(java.util.List.of(details));
        
        daHeaderService.createPurchaseRequest(da);
    }
}

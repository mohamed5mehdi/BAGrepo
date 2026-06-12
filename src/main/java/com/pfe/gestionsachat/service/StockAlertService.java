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
                    boolean daExists = demandeAchatInterneRepository.findAll().stream()
                        .anyMatch(da -> da.getItemCode() != null && da.getItemCode().equals(item.getItemCode()) 
                            && da.getStatut() != com.pfe.gestionsachat.model.StatutDemande.REJETEE);
                    if (!daExists) {
                        log.warn("Alerte Stock : L'article {} est sous le seuil (Dispo: {}, Seuil: {}).", 
                                 item.getItemCode(), item.getQuantityAvailable(), item.getReorderPoint());
                        createAutomaticDa(item);
                    }
                }
            }
        }
    }

    @Autowired
    private DemandeAchatInterneService demandeAchatInterneService;
    
    @Autowired
    private com.pfe.gestionsachat.repository.DemandeAchatInterneRepository demandeAchatInterneRepository;

    @Autowired
    private com.pfe.gestionsachat.repository.UserRepository userRepository;

    private void createAutomaticDa(StockItem item) {
        log.info("Création automatique d'une DA pour l'article {}", item.getItemCode());
        com.pfe.gestionsachat.model.DemandeAchatInterne da = new com.pfe.gestionsachat.model.DemandeAchatInterne();
        da.setDesignation("Réapprovisionnement automatique pour: " + item.getItemName());
        da.setItemCode(item.getItemCode());
        
        // Find a system user or first user to be the requester
        userRepository.findAll().stream().findFirst().ifPresent(da::setDemandeur);
        
        da.setQuantite(item.getReorderPoint() != null ? item.getReorderPoint() : 10);
        da.setJustification("Alerte stock déclenchée automatiquement");
        da.setUrgence(com.pfe.gestionsachat.model.UrgenceDemande.CRITIQUE);
        
        com.pfe.gestionsachat.model.User sysUser = userRepository.findAll().stream().findFirst().orElse(null);
        demandeAchatInterneService.createDemande(da, sysUser);
    }
}

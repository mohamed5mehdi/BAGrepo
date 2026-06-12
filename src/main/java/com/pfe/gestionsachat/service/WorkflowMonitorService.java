package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.PurchaseOrder;
import com.pfe.gestionsachat.repository.PurchaseOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class WorkflowMonitorService {

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired
    private com.pfe.gestionsachat.repository.StatusHistoryRepository historyRepository;

    /**
     * Règle R6/R4 : Gestion des "Workflows Fantômes".
     * Exécution toutes les heures pour vérifier les POs validés sans réception après 30 jours.
     */
    @Scheduled(cron = "0 0 * * * *")
    @org.springframework.transaction.annotation.Transactional
    public void monitorGhostWorkflows() {
        log.info("Démarrage du monitoring des workflows fantômes...");
        
        LocalDate limitDate = LocalDate.now().minusDays(30);
        
        // On cherche les POs en statut 'VALIDE' (en attente de réception) créés il y a plus de 30 jours
        List<PurchaseOrder> ghostPOs = purchaseOrderRepository.findGhostPurchaseOrders(
                com.pfe.gestionsachat.model.POStatus.APPROVED, limitDate);
        
        for (PurchaseOrder po : ghostPOs) {
            String msg = "Alerte R6/R4: Le PO #" + po.getIdPo() + " (DA Interne #" + 
                         (po.getDemandeInterne() != null ? po.getDemandeInterne().getId() : "N/A") + 
                         ") n'a aucune réception après 30 jours.";
            log.warn(msg);
            
            if (po.getDemandeInterne() != null) {
                com.pfe.gestionsachat.model.User demandeur = po.getDemandeInterne().getDemandeur();
                com.pfe.gestionsachat.model.StatusHistory alerte = new com.pfe.gestionsachat.model.StatusHistory(
                    "DemandeAchatInterne", Long.valueOf(po.getDemandeInterne().getId()),
                    "PO_APPROVED", "ALERTE_RETARD_GRN", demandeur, msg
                );
                historyRepository.save(alerte);
            }
        }
    }
}

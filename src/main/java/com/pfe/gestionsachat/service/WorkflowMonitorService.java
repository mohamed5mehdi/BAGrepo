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
    private com.pfe.gestionsachat.repository.ActionRepository actionRepository;

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
        List<PurchaseOrder> ghostPOs = purchaseOrderRepository.findGhostPurchaseOrders(limitDate);
        
        for (PurchaseOrder po : ghostPOs) {
            String msg = "Alerte R6/R4: Le PO #" + po.getIdPo() + " (DA #" + 
                         (po.getDaHeader() != null ? po.getDaHeader().getOidDa() : "N/A") + 
                         ") n'a aucune réception après 30 jours.";
            log.warn(msg);
            
            if (po.getDaHeader() != null) {
                com.pfe.gestionsachat.model.Action alerte = new com.pfe.gestionsachat.model.Action();
                alerte.setDaHeader(po.getDaHeader());
                alerte.setUser(po.getDaHeader().getDemandeur()); // On alerte le demandeur par défaut
                alerte.setTypeAction(com.pfe.gestionsachat.model.TypeAction.ALERTE_RETARD);
                alerte.setMetadata(msg);
                actionRepository.save(alerte);
            }
        }
    }
}

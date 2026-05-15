package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.DemandeAchatInterne;
import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.model.Warehouse;
import com.pfe.gestionsachat.model.StockItem;
import com.pfe.gestionsachat.model.Supplier;
import com.pfe.gestionsachat.model.UrgenceDemande;
import com.pfe.gestionsachat.model.CategorieDemande;
import com.pfe.gestionsachat.model.StatutDemande;
import com.pfe.gestionsachat.repository.UserRepository;
import com.pfe.gestionsachat.repository.StockItemRepository;
import com.pfe.gestionsachat.repository.WarehouseRepository;
import com.pfe.gestionsachat.repository.SupplierRepository;
import com.pfe.gestionsachat.service.DemandeAchatInterneService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;


import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class InternalProcurementTest {

    @Autowired private DemandeAchatInterneService demandeService;
    @Autowired private UserRepository userRepository;
    @Autowired private StockItemRepository stockItemRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private SupplierRepository supplierRepository;

    @Test
    void testFullInternalFlow() {
        // 1. Setup Data
        User demandeur = userRepository.findByEmail("demandeur@test.com").orElseThrow();
        User n1 = demandeur.getN1();
        
        Warehouse w = warehouseRepository.findAll().get(0);
        StockItem item = new StockItem();
        item.setItemCode("TEST-LAPTOP");
        item.setItemName("Laptop Dell");
        item.setQuantityAvailable(5);
        item.setWarehouse(w);
        stockItemRepository.save(item);

        Supplier supplier = supplierRepository.findAll().get(0);

        // 2. Create Request (Brouillon)
        DemandeAchatInterne demande = new DemandeAchatInterne();
        demande.setDesignation("Laptop Dell");
        demande.setQuantite(2);
        demande.setUrgence(UrgenceDemande.NORMALE);
        demande.setCategorie(CategorieDemande.INFORMATIQUE);
        demande.setIsPieceRechange(true); // Flux SAV pour test stock
        
        DemandeAchatInterne saved = demandeService.createDemande(demande, demandeur);
        assertNotNull(saved.getId());
        assertEquals(StatutDemande.BROUILLON, saved.getStatut());

        // 3. Soumettre (Vérification Stock)
        // Cas 1: Stock suffisant -> AFFECTEE
        DemandeAchatInterne soumise = demandeService.soumettre(saved.getId(), demandeur);
        assertEquals(StatutDemande.DISPONIBLE_STOCK, soumise.getStatut());
        
        // Vérifier stock déduit
        StockItem updatedItem = stockItemRepository.findByItemNameIgnoreCase("Laptop Dell").get(0);
        assertEquals(3, updatedItem.getQuantityAvailable());

        // Cas 2: Stock insuffisant -> SOUMISE (N+1)
        DemandeAchatInterne demande2 = new DemandeAchatInterne();
        demande2.setDesignation("Laptop Dell");
        demande2.setQuantite(10);
        demande2.setIsPieceRechange(true);
        demande2 = demandeService.createDemande(demande2, demandeur);
        DemandeAchatInterne soumise2 = demandeService.soumettre(demande2.getId(), demandeur);
        assertEquals(StatutDemande.EN_TRAITEMENT, soumise2.getStatut());

        // 4. Acheteur prend le relais (EN_TRAITEMENT)
        User acheteur = userRepository.findByEmail("acheteur@test.com").orElseThrow(); // Assurez-vous que cet user existe ou créez-en un fictif, ou utilisez un user avec role ACHETEUR.
        // Si "acheteur" n'existe pas dans le seeder, on va forcer le rôle sur un user existant pour le test
        User acheteurTest = userRepository.findAll().stream().filter(u -> u.getRole() == com.pfe.gestionsachat.model.Role.ACHETEUR).findFirst().orElseThrow();

        DemandeAchatInterne valorisee = demandeService.valoriserDemande(soumise2.getId(), java.math.BigDecimal.valueOf(15000.0), supplier.getOidSupplier());
        assertEquals(java.math.BigDecimal.valueOf(15000.0), valorisee.getPrixUnitaire());
        
        DemandeAchatInterne traitee = demandeService.traiterAchat(soumise2.getId(), acheteurTest);
        assertEquals(StatutDemande.SOUMISE, traitee.getStatut());

        // 5. Workflow de validation (N1)
        DemandeAchatInterne valideeN1 = demandeService.validerN1(soumise2.getId(), true, "Ok pour moi", n1);
        assertEquals(StatutDemande.VALIDE_N1, valideeN1.getStatut());
    }
}

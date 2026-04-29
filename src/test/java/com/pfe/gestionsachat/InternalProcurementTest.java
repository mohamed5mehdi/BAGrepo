package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import com.pfe.gestionsachat.service.*;
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
        
        DemandeAchatInterne saved = demandeService.createDemande(demande, demandeur);
        assertNotNull(saved.getId());
        assertEquals(StatutDemande.BROUILLON, saved.getStatut());

        // 3. Soumettre (Vérification Stock)
        // Cas 1: Stock suffisant -> AFFECTEE
        DemandeAchatInterne soumise = demandeService.soumettre(saved.getId(), demandeur);
        assertEquals(StatutDemande.AFFECTEE, soumise.getStatut());
        
        // Vérifier stock déduit
        StockItem updatedItem = stockItemRepository.findByItemNameIgnoreCase("Laptop Dell").get(0);
        assertEquals(3, updatedItem.getQuantityAvailable());

        // Cas 2: Stock insuffisant -> SOUMISE (N+1)
        DemandeAchatInterne demande2 = new DemandeAchatInterne();
        demande2.setDesignation("Laptop Dell");
        demande2.setQuantite(10);
        demande2 = demandeService.createDemande(demande2, demandeur);
        DemandeAchatInterne soumise2 = demandeService.soumettre(demande2.getId(), demandeur);
        assertEquals(StatutDemande.SOUMISE, soumise2.getStatut());

        // 4. Workflow de validation
        DemandeAchatInterne valideeN1 = demandeService.validerN1(soumise2.getId(), true, "Ok pour moi", n1);
        assertEquals(StatutDemande.VALIDEE_N1, valideeN1.getStatut());

        // 5. Valorisation par l'acheteur
        DemandeAchatInterne valorisee = demandeService.valoriserDemande(valideeN1.getId(), 15000.0, supplier.getOidSupplier());
        assertEquals(15000.0, valorisee.getPrixUnitaire());
        assertEquals(BigDecimal.valueOf(150000.0).setScale(2), valorisee.getMontantEstime().setScale(2));
    }
}

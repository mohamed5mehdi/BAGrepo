package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
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
    @Autowired private FamilyRepository familyRepository;
    @Autowired private SubFamilyRepository subFamilyRepository;

    @Test
    void testSavBypassFlow() {
        // 1. Setup
        User demandeur = userRepository.findByEmail("demandeur@test.com").orElseThrow();
        Family family = familyRepository.findAll().get(0);
        SubFamily subFamily = subFamilyRepository.findAll().stream()
                .filter(sf -> sf.getFamily().getIdFamily().equals(family.getIdFamily()))
                .findFirst().orElseThrow();
        Supplier supplier = supplierRepository.findAll().get(0);
        User acheteur = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ACHETEUR).findFirst().orElseThrow();

        // 2. Création SAV
        DemandeAchatInterne da = new DemandeAchatInterne();
        da.setDesignation("Pièce SAV Test");
        da.setQuantite(1);
        da.setIsPieceRechange(true);
        da.setItemCode("PART-999");
        da.setBudgetFamille(family);
        da.setBudgetSousFamille(subFamily);
        
        DemandeAchatInterne saved = demandeService.createDemande(da, demandeur);
        DemandeAchatInterne soumise = demandeService.soumettre(saved.getId(), demandeur);
        assertEquals(StatutDemande.EN_TRAITEMENT, soumise.getStatut());

        // 3. Traitement Acheteur (Valorisation + Traiter)
        demandeService.valoriserDemande(soumise.getId(), BigDecimal.valueOf(500), supplier.getOidSupplier());
        DemandeAchatInterne traitee = demandeService.traiterAchat(soumise.getId(), acheteur);
        
        // VERIFICATION CRITIQUE : Statut APPROUVEE directement (Bypass N1/DG)
        assertEquals(StatutDemande.APPROUVEE, traitee.getStatut());
    }

    @Test
    void testStandardHierarchicalFlow() {
        // 1. Setup
        User demandeur = userRepository.findByEmail("demandeur@test.com").orElseThrow();
        User n1 = demandeur.getN1();
        Family family = familyRepository.findAll().get(0);
        SubFamily subFamily = subFamilyRepository.findAll().stream()
                .filter(sf -> sf.getFamily().getIdFamily().equals(family.getIdFamily()))
                .findFirst().orElseThrow();
        Supplier supplier = supplierRepository.findAll().get(0);
        User acheteur = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ACHETEUR).findFirst().orElseThrow();

        // 2. Création Standard
        DemandeAchatInterne da = new DemandeAchatInterne();
        da.setDesignation("Fournitures Bureau");
        da.setQuantite(10);
        da.setIsPieceRechange(false); // Flux Standard
        da.setBudgetFamille(family);
        da.setBudgetSousFamille(subFamily);
        
        DemandeAchatInterne saved = demandeService.createDemande(da, demandeur);
        DemandeAchatInterne soumise = demandeService.soumettre(saved.getId(), demandeur);
        
        // VERIFICATION : Flux Standard va au N1
        assertEquals(StatutDemande.SOUMISE, soumise.getStatut());

        // 3. Validation N1
        DemandeAchatInterne valideeN1 = demandeService.validerN1(soumise.getId(), true, "Approuvé", n1);
        assertEquals(StatutDemande.VALIDE_N1, valideeN1.getStatut());
    }
}

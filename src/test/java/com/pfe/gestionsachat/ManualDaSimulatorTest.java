package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootTest
public class ManualDaSimulatorTest {

    @Autowired private DemandeAchatInterneRepository demandeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private SubFamilyRepository subFamilyRepository;

    @Test
    public void createReadyDa() {
        User demandeur = userRepository.findByEmail("demandeur@test.com").orElseThrow();
        Supplier supplier = supplierRepository.findAll().stream().findFirst().orElseThrow();
        SubFamily sf = subFamilyRepository.findAll().stream().findFirst().orElseThrow();
        
        DemandeAchatInterne da = new DemandeAchatInterne();
        da.setDemandeur(demandeur);
        da.setDesignation("Écrans 4K Dell UltraSharp (Machine Test)");
        da.setQuantite(2);
        da.setPrixUnitaire(BigDecimal.valueOf(4500.0));
        da.setMontantEstime(BigDecimal.valueOf(9000.0));
        da.setFournisseur(supplier);
        da.setBudgetFamille(sf.getFamily());
        da.setBudgetSousFamille(sf);
        da.setStatut(StatutDemande.APPROUVEE);
        da.setDateCreation(LocalDateTime.now());
        
        demandeRepository.save(da);
        System.out.println("✅ DA SIMULÉE PRÊTE : ID=" + da.getId());
    }
}

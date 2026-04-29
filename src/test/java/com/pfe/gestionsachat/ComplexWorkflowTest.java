package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import com.pfe.gestionsachat.service.AchatWorkflowOrchestrator;
import com.pfe.gestionsachat.service.DaHeaderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ComplexWorkflowTest {

    @Autowired private DaHeaderRepository daHeaderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SubFamilyRepository subFamilyRepository;
    @Autowired private AchatWorkflowOrchestrator orchestrator;
    @Autowired private DaHeaderService daHeaderService;
    @Autowired private PurchaseOrderRepository purchaseOrderRepository;

    @Test
    @Transactional
    void testCompleteWorkflowWithBudgetShortageAndAdjustment() {
        System.out.println("--- Starting Complex Workflow Test ---");

        // 1. Setup Data
        User demandeur = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.EMPLOYE)
                .findFirst().orElseThrow();
        SubFamily sfHardware = subFamilyRepository.findAll().stream()
                .filter(sf -> sf.getLibelle().contains("Matériel"))
                .findFirst().orElseThrow();
        sfHardware.setBudgetRestant(java.math.BigDecimal.valueOf(2000.0));
        subFamilyRepository.save(sfHardware);
        
        User n1 = userRepository.findAll().stream().filter(u -> u.getRole() == Role.MANAGER_N1).findFirst().orElseThrow();
        User tech = userRepository.findAll().stream().filter(u -> u.getRole() == Role.TECHNICIEN).findFirst().orElseThrow();
        User acheteur = userRepository.findAll().stream().filter(u -> u.getRole() == Role.ACHETEUR).findFirst().orElseThrow();
        User amg = userRepository.findAll().stream().filter(u -> u.getRole() == Role.AMG).findFirst().orElseThrow();
        User daf = userRepository.findAll().stream().filter(u -> u.getRole() == Role.DAF).findFirst().orElseThrow();
        User dg = userRepository.findAll().stream().filter(u -> u.getRole() == Role.DG).findFirst().orElseThrow();

        // 2. Create DA with expensive item (3000.0) - Hardware budget is only 2000.0
        DaHeader da = new DaHeader("Achat Serveur Puissant", demandeur);
        DaDetails detail = new DaDetails(da, sfHardware, 1, "Serveur HP ProLiant", java.math.BigDecimal.valueOf(3000.0));
        da.getDetails().add(detail);
        da = daHeaderService.createPurchaseRequest(da);
        Integer daId = da.getOidDa();
        assertNotNull(daId, "DA ID should not be null");

        System.out.println("1. DA Created: " + daId + " | Status: " + da.getStatut());
        assertEquals(StatutDA.EN_ATTENTE_N1, da.getStatut());

        // 3. N1 Validation
        da = orchestrator.processValidation(daId, n1.getOidUser(), ValidationDecision.ACCEPTE, "Approuvé par N1");
        System.out.println("2. N1 Validated | Status: " + da.getStatut());
        assertEquals(StatutDA.EN_ATTENTE_TECH, da.getStatut());

        // 4. Tech Validation
        da = orchestrator.processValidation(daId, tech.getOidUser(), ValidationDecision.ACCEPTE, "Techniquement OK");
        System.out.println("3. Tech Validated | Status: " + da.getStatut());
        assertEquals(StatutDA.EN_ATTENTE_ACHAT, da.getStatut());

        // 5. Acheteur Budget Check - Should be INSUFFISANT
        AchatWorkflowOrchestrator.BudgetCheckResult check = orchestrator.verifierBudget(daId, acheteur.getOidUser());
        System.out.println("4. Budget Check: " + check);
        assertEquals(AchatWorkflowOrchestrator.BudgetCheckResult.INSUFFISANT, check);
        da = daHeaderRepository.findById(daId).orElseThrow();
        assertNotNull(da);
        assertEquals(StatutDA.EN_ATTENTE_ACHAT, da.getStatut());

        // 6. DAF Adjustment (Transfer 1000 from Software to Hardware)
        SubFamily sfSoftware = subFamilyRepository.findAll().stream()
                .filter(sf -> sf.getLibelle().contains("Logiciels"))
                .findFirst().orElseThrow();
        
        System.out.println("5. DAF Adjusting Budget...");
        da = orchestrator.ajusterBudgetSousFamille(daId, daf.getOidUser(), sfSoftware.getOidSub(), sfHardware.getOidSub(), java.math.BigDecimal.valueOf(1000.0));
        assertEquals(StatutDA.EN_ATTENTE_AMG, da.getStatut());

        // 7. AMG approval (already in EN_ATTENTE_AMG after adjustment)
        System.out.println("6. AMG Validating...");
        da = orchestrator.processValidation(daId, amg.getOidUser(), ValidationDecision.ACCEPTE, "AMG OK");
        assertEquals(StatutDA.EN_ATTENTE_DAF, da.getStatut());

        // 8. DAF approval
        da = orchestrator.processValidation(daId, daf.getOidUser(), ValidationDecision.ACCEPTE, "DAF OK");
        assertEquals(StatutDA.EN_ATTENTE_DG, da.getStatut());

        // 9. DG approval -> VALIDEE (but no PO yet)
        da = orchestrator.processValidation(daId, dg.getOidUser(), ValidationDecision.ACCEPTE, "DG OK");
        System.out.println("7. Final Approval | Status: " + da.getStatut());
        assertEquals(StatutDA.VALIDEE, da.getStatut());

        // 10. Manual PO creation by Buyer
        System.out.println("8. Buyer Creating PO...");
        orchestrator.manualCreatePO(daId, acheteur.getOidUser());
        da = daHeaderRepository.findById(daId).orElseThrow();
        assertNotNull(da);
        assertEquals(StatutDA.PO_CREE, da.getStatut());

        // 11. Final Checks
        assertTrue(purchaseOrderRepository.count() > 0);
        PurchaseOrder po = purchaseOrderRepository.findAll().stream()
                .filter(p -> p.getDaHeader().getOidDa().equals(daId))
                .findFirst().orElseThrow();
        assertEquals(java.math.BigDecimal.valueOf(3000.0), po.getMontantTotal());
        assertEquals("VALIDE", po.getStatut());

        // Check budget deduction
        Integer subId = sfHardware.getOidSub();
        assertNotNull(subId);
        SubFamily sfFinal = subFamilyRepository.findById(subId).orElseThrow();
        System.out.println("Final Budget Hardware: " + sfFinal.getBudgetRestant());
        assertEquals(0, java.math.BigDecimal.ZERO.compareTo(sfFinal.getBudgetRestant()), "Budget should be exactly 0 after 3000 purchase");

        System.out.println("--- Complex Workflow Test PASSED ---");
    }
}

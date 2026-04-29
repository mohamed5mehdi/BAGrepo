package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import com.pfe.gestionsachat.service.AchatWorkflowOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DaToPoFlowTest {

    @Autowired private AchatWorkflowOrchestrator orchestrator;
    @Autowired private DaHeaderRepository daHeaderRepository;
    @Autowired private DaDetailsRepository daDetailsRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SubFamilyRepository subFamilyRepository;
    @Autowired private FamilyRepository familyRepository;

    @Test
    void testCompleteWorkflow_FromDaToPo() {
        // 1. Setup Data
        Family family = new Family("Test Family", BigDecimal.valueOf(10000.0));
        family = familyRepository.save(family);
        
        SubFamily subFamily = new SubFamily("Test SubFamily", BigDecimal.valueOf(5000.0), family);
        subFamily = subFamilyRepository.save(subFamily);

        User n1 = userRepository.findByEmail("n1@test.com").orElseThrow();
        User tech = userRepository.findByEmail("tech@test.com").orElseThrow();
        User acheteur = userRepository.findByEmail("acheteur@test.com").orElseThrow();
        User amg = userRepository.findByEmail("amg@test.com").orElseThrow();
        User dg = userRepository.findByEmail("dg@test.com").orElseThrow();
        User demandeur = userRepository.findByEmail("demandeur@test.com").orElseThrow();

        // 2. Create DA
        DaHeader da = new DaHeader("Achat Test Flow", demandeur);
        da.setStatut(StatutDA.EN_ATTENTE_N1);
        da = daHeaderRepository.save(da);

        DaDetails detail = new DaDetails(da, subFamily, 1, "Item Test", BigDecimal.valueOf(1000.0));
        daDetailsRepository.save(detail);
        da.setDetails(new java.util.ArrayList<>(List.of(detail)));
        Integer daId = java.util.Objects.requireNonNull(da.getOidDa());

        // 3. Validation N1 -> Tech
        orchestrator.processValidation(daId, java.util.Objects.requireNonNull(n1.getOidUser()), ValidationDecision.ACCEPTE, "OK N1");
        assertEquals(StatutDA.EN_ATTENTE_TECH, daHeaderRepository.findById(daId).get().getStatut());

        // 4. Validation Tech -> Achat
        orchestrator.processValidation(daId, java.util.Objects.requireNonNull(tech.getOidUser()), ValidationDecision.ACCEPTE, "OK Tech");
        assertEquals(StatutDA.EN_ATTENTE_ACHAT, daHeaderRepository.findById(daId).get().getStatut());

        // 5. Check Budget -> AMG
        AchatWorkflowOrchestrator.BudgetCheckResult budgetResult = orchestrator.verifierBudget(daId, java.util.Objects.requireNonNull(acheteur.getOidUser()));
        assertEquals(AchatWorkflowOrchestrator.BudgetCheckResult.SUFFISANT, budgetResult);
        assertEquals(StatutDA.EN_ATTENTE_AMG, daHeaderRepository.findById(daId).get().getStatut());

        // 6. Validation AMG -> DG (Pas de transfert complexe ici, par défaut circuit DG si simple)
        orchestrator.processValidation(daId, java.util.Objects.requireNonNull(amg.getOidUser()), ValidationDecision.ACCEPTE, "OK AMG");
        assertEquals(StatutDA.EN_ATTENTE_DG, daHeaderRepository.findById(daId).get().getStatut());

        // 7. Validation DG -> VALIDEE
        orchestrator.processValidation(daId, java.util.Objects.requireNonNull(dg.getOidUser()), ValidationDecision.ACCEPTE, "OK DG");
        assertEquals(StatutDA.VALIDEE, daHeaderRepository.findById(daId).get().getStatut());

        // 8. Create PO
        PurchaseOrder po = orchestrator.manualCreatePO(daId, java.util.Objects.requireNonNull(acheteur.getOidUser()));
        assertNotNull(po);
        assertEquals("VALIDE", po.getStatut());
        assertTrue(new BigDecimal("1200.00").compareTo(po.getMontantTotal()) == 0);
        assertEquals(StatutDA.PO_CREE, daHeaderRepository.findById(daId).get().getStatut());

        // 9. Verify Budget Deduction
        SubFamily updatedSf = subFamilyRepository.findById(java.util.Objects.requireNonNull(subFamily.getOidSub())).get();
        assertTrue(new BigDecimal("4000.0").compareTo(updatedSf.getBudgetRestant()) == 0);

        System.out.println("TEST SUCCESS: Workflow complet DA -> PO validé.");
    }
}

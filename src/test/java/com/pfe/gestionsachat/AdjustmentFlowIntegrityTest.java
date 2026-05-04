package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import com.pfe.gestionsachat.service.AchatWorkflowOrchestrator;
import com.pfe.gestionsachat.service.DemandeAjustementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class AdjustmentFlowIntegrityTest {

    @Autowired private DemandeAjustementService adjustmentService;
    @Autowired private AchatWorkflowOrchestrator orchestrator;
    @Autowired private SubFamilyRepository subFamilyRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private DaHeaderRepository daHeaderRepository;
    @Autowired private DaDetailsRepository daDetailsRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BudgetTransferRepository transferRepository;

    @Test
    @Transactional
    void testFullSubFamilyAdjustmentLifecycle() {
        // 1. SETUP: Create Family and 2 SubFamilies
        Family f = new Family();
        f.setLibelle("DEPT_TEST");
        f.setBudgetInitial(new BigDecimal("5000.00"));
        f.setBudgetRestant(new BigDecimal("5000.00"));
        f = familyRepository.save(f);

        SubFamily sfSource = new SubFamily("SF_SOURCE", new BigDecimal("2000.00"), f);
        sfSource.setBudgetRestant(new BigDecimal("2000.00"));
        sfSource.setBudgetEngage(BigDecimal.ZERO);
        sfSource = subFamilyRepository.save(sfSource);

        SubFamily sfCible = new SubFamily("SF_CIBLE", new BigDecimal("0.00"), f);
        sfCible.setBudgetRestant(BigDecimal.ZERO);
        sfCible.setBudgetEngage(BigDecimal.ZERO);
        sfCible = subFamilyRepository.save(sfCible);

        User buyer = new User("Buyer", "buyer@test.com", "pwd", Role.ACHETEUR);
        buyer = userRepository.save(buyer);
        
        User daf = new User("DAF", "daf@test.com", "pwd", Role.DAF);
        daf = userRepository.save(daf);

        // 2. CREATE DA: Requesting 500 MAD from SF_CIBLE (which has 0)
        DaHeader da = new DaHeader("Besoin Urgent", buyer);
        da = daHeaderRepository.save(da);
        DaDetails detail = new DaDetails(da, sfCible, 1, "Item", new BigDecimal("500.00"));
        daDetailsRepository.save(detail);
        da.getDetails().add(detail);
        da = daHeaderRepository.save(da);

        // 3. SOUMETTRE AJUSTEMENT: Transfer 500 from SF_SOURCE to SF_CIBLE
        DemandeAjustement ajust = adjustmentService.soumettreAjustementSousFamille(
                da.getOidDa(), sfSource.getOidSub(), sfCible.getOidSub(), 
                new BigDecimal("500.00"), "Transfert pour DA urgente", Long.valueOf(buyer.getOidUser()));

        assertNotNull(ajust.getId());
        assertEquals(StatutAjustement.EN_ATTENTE_DAF, ajust.getStatut());

        // Verify Source Budget is ENGAGED (reserved)
        SubFamily sourceAfterSub = subFamilyRepository.findById(sfSource.getOidSub()).orElseThrow();
        assertEquals(new BigDecimal("500.00"), sourceAfterSub.getBudgetEngage());

        // 4. DAF DECISION: Approve the transfer
        DemandeAjustement approved = adjustmentService.deciderDaf(
                ajust.getId(), Long.valueOf(daf.getOidUser()), "VALIDE", new BigDecimal("500.00"), "Accordé");

        assertEquals(StatutAjustement.EN_ATTENTE_ACHETEUR, approved.getStatut());

        // 5. VERIFY FINAL BUDGETS
        SubFamily finalSource = subFamilyRepository.findById(sfSource.getOidSub()).orElseThrow();
        SubFamily finalCible = subFamilyRepository.findById(sfCible.getOidSub()).orElseThrow();
        
        assertEquals(0, new BigDecimal("1500.00").compareTo(finalSource.getBudgetRestant()), "Source should be 2000 - 500");
        assertEquals(0, BigDecimal.ZERO.compareTo(finalSource.getBudgetEngage()), "Engagement should be released");
        assertEquals(0, new BigDecimal("500.00").compareTo(finalCible.getBudgetRestant()), "Cible should now have 500");

        // 6. VERIFY AUDIT
        List<BudgetTransfer> transfers = transferRepository.findAll();
        assertFalse(transfers.isEmpty());
        assertEquals(new BigDecimal("500.00"), transfers.get(0).getMontant());
    }
}

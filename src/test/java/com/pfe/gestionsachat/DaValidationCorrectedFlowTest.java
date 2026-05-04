package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import com.pfe.gestionsachat.service.AchatWorkflowOrchestrator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class DaValidationCorrectedFlowTest {

    @Autowired private AchatWorkflowOrchestrator orchestrator;
    @Autowired private DaHeaderRepository daHeaderRepository;
    @Autowired private DaDetailsRepository daDetailsRepository;
    @Autowired private SubFamilyRepository subFamilyRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private DemandeAjustementRepository demandeAjustementRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void cleanup() {
        String[] tables = {
            "stock_movement", "grc_details", "grc_header", "grn_details", "grn_header",
            "credit_note", "invoice", "purchase_order", "offre_fournisseur",
            "da_details", "da_header", "demande_ajustement", "budget_sous_famille",
            "budget_famille", "sub_family", "family", "users"
        };
        for (String table : tables) {
            jdbcTemplate.execute("DELETE FROM " + table);
        }
    }

    @Test
    @Transactional
    public void testValidationDa_MultiFamilles_Success() {
        DaHeader da = preparerDaMultiFamilles(1000, 2000); 
        
        orchestrator.processValidation(da.getOidDa(), getN1Actif().getOidUser(), ValidationDecision.ACCEPTE, "Test Validation");

        Integer sfAId = java.util.Objects.requireNonNull(da.getDetails().get(0).getSubFamily().getOidSub());
        SubFamily sfA = subFamilyRepository.findById(sfAId).orElseThrow();
        Integer sfBId = java.util.Objects.requireNonNull(da.getDetails().get(1).getSubFamily().getOidSub());
        SubFamily sfB = subFamilyRepository.findById(sfBId).orElseThrow();
        
        assertEquals(new BigDecimal("1000.00"), sfA.getBudgetEngage(), "Le budget de la SF A devrait être amputé de 1000");
        assertEquals(new BigDecimal("2000.00"), sfB.getBudgetEngage(), "Le budget de la SF B devrait être amputé de 2000");
    }

    @Test
    public void testValidationDa_ConcurrentLocks_NoDeadlock() throws InterruptedException {
        DaHeader da1 = preparerDaMultiFamilles(100, 100); 
        DaHeader da2 = preparerDaMultiFamilles(100, 100); 
        inverseOrdreLignes(da2); 

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        executor.submit(() -> {
            try {
                orchestrator.processValidation(da1.getOidDa(), getN1Actif().getOidUser(), ValidationDecision.ACCEPTE, "OK");
            } catch (Exception e) {}
            latch.countDown();
        });
        executor.submit(() -> {
            try {
                orchestrator.processValidation(da2.getOidDa(), getN1Actif().getOidUser(), ValidationDecision.ACCEPTE, "OK");
            } catch (Exception e) {}
            latch.countDown();
        });

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "Un Deadlock a été détecté ! Le test s'est bloqué.");
        
        executor.shutdown();
    }

    @Test
    public void testValidationDa_BudgetInsuffisant_CreatesAjustementDespiteRollback() {
        DaHeader da = preparerDaDepassementBudget();
        
        long ajustementsAvant = demandeAjustementRepository.count();

        assertThrows(RuntimeException.class, () -> {
            orchestrator.processValidation(da.getOidDa(), getN1Actif().getOidUser(), ValidationDecision.ACCEPTE, "Test");
        });

        Integer daId = java.util.Objects.requireNonNull(da.getOidDa());
        DaHeader daEnBase = daHeaderRepository.findById(daId).orElseThrow();
        assertEquals(StatutDA.EN_AJUSTEMENT, daEnBase.getStatut());
        
        long ajustementsApres = demandeAjustementRepository.count();
        assertEquals(ajustementsAvant + 1, ajustementsApres, "La demande d'ajustement n'a pas été sauvegardée !");
    }

    @Test
    @Transactional
    public void testValidationDa_ApprobateurN2Inactif_ThrowsException() {
        DaHeader da = preparerDaMontantEleve(6000);
        desactiverN2();

        assertThrows(RuntimeException.class, () -> {
            orchestrator.processValidation(da.getOidDa(), getN1Actif().getOidUser(), ValidationDecision.ACCEPTE, "Test");
        });
        
        Integer daId = java.util.Objects.requireNonNull(da.getOidDa());
        DaHeader daApres = daHeaderRepository.findById(daId).orElseThrow();
        assertNotEquals(StatutDA.EN_ATTENTE_N2, daApres.getStatut());
    }

    private DaHeader preparerDaMultiFamilles(int m1, int m2) {
        Family family = new Family();
        family.setLibelle("IT_TEST_" + System.nanoTime());
        family.setBudgetInitial(new BigDecimal("10000.00"));
        family.setBudgetRestant(new BigDecimal("10000.00"));
        family = familyRepository.save(family);

        SubFamily sf1 = new SubFamily("Logiciels_" + System.nanoTime(), new BigDecimal("5000.00"), family);
        sf1.setBudgetEngage(BigDecimal.ZERO);
        sf1 = subFamilyRepository.save(sf1);

        SubFamily sf2 = new SubFamily("Matériels_" + System.nanoTime(), new BigDecimal("5000.00"), family);
        sf2.setBudgetEngage(BigDecimal.ZERO);
        sf2 = subFamilyRepository.save(sf2);

        DaHeader da = new DaHeader("Achat Multi-Familles", getN1Actif());
        da.setStatut(StatutDA.EN_ATTENTE_N1);
        da.setDateCreation(LocalDate.now());
        da = daHeaderRepository.save(da);

        DaDetails detail1 = new DaDetails(da, sf1, 1, "Licence", new BigDecimal(m1));
        detail1.setItemCode("C1");
        detail1.setItemName("N1");
        detail1 = daDetailsRepository.save(detail1);

        DaDetails detail2 = new DaDetails(da, sf2, 1, "Serveur", new BigDecimal(m2));
        detail2.setItemCode("C2");
        detail2.setItemName("N2");
        detail2 = daDetailsRepository.save(detail2);

        da.getDetails().add(detail1);
        da.getDetails().add(detail2);
        return daHeaderRepository.save(da);
    }

    private DaHeader preparerDaDepassementBudget() {
        Family family = new Family();
        family.setLibelle("MOBILIER_TEST_" + System.nanoTime());
        family.setBudgetInitial(new BigDecimal("1000.00"));
        family.setBudgetRestant(new BigDecimal("1000.00"));
        family = familyRepository.save(family);

        SubFamily sf = new SubFamily("Chaises_" + System.nanoTime(), new BigDecimal("500.00"), family);
        sf.setBudgetEngage(BigDecimal.ZERO);
        sf = subFamilyRepository.save(sf);

        DaHeader da = new DaHeader("Achat Chaises", getN1Actif());
        da.setStatut(StatutDA.EN_ATTENTE_N1);
        da.setDateCreation(LocalDate.now());
        da = daHeaderRepository.save(da);

        DaDetails detail = new DaDetails(da, sf, 1, "Chaise Ergonomique", new BigDecimal("2000.00"));
        detail.setItemCode("C3");
        detail.setItemName("N3");
        detail = daDetailsRepository.save(detail);

        da.getDetails().add(detail);
        return daHeaderRepository.save(da);
    }

    private DaHeader preparerDaMontantEleve(int m) {
        Family family = new Family();
        family.setLibelle("PRESTA_TEST_" + System.nanoTime());
        family.setBudgetInitial(new BigDecimal("100000.00"));
        family.setBudgetRestant(new BigDecimal("100000.00"));
        family = familyRepository.save(family);

        SubFamily sf = new SubFamily("Conseil_" + System.nanoTime(), new BigDecimal("100000.00"), family);
        sf.setBudgetEngage(BigDecimal.ZERO);
        sf = subFamilyRepository.save(sf);

        DaHeader da = new DaHeader("Mission Conseil", getN1Actif());
        da.setStatut(StatutDA.EN_ATTENTE_N1);
        da.setDateCreation(LocalDate.now());
        da = daHeaderRepository.save(da);

        DaDetails detail = new DaDetails(da, sf, 1, "Audit Sécurité", new BigDecimal(m));
        detail.setItemCode("C4");
        detail.setItemName("N4");
        detail = daDetailsRepository.save(detail);

        da.getDetails().add(detail);
        return daHeaderRepository.save(da);
    }

    private void inverseOrdreLignes(DaHeader da) {
        if (da.getDetails() != null && da.getDetails().size() > 1) {
            List<DaDetails> details = new ArrayList<>(da.getDetails());
            Collections.reverse(details);
            da.setDetails(details);
            daHeaderRepository.save(da);
        }
    }

    private User getN1Actif() {
        List<User> users = userRepository.findAll();
        for (User u : users) {
            if (u.getRole() == Role.MANAGER_N1 && u.getActif() != null && u.getActif()) {
                return u;
            }
        }
        User n1 = new User("TestN1", "n1@test.com", "pass", Role.MANAGER_N1);
        n1.setActif(true);
        n1.setService("IT");
        return userRepository.save(n1);
    }

    private void desactiverN2() {
        List<User> users = userRepository.findAll();
        boolean n2Found = false;
        for (User u : users) {
            if (u.getRole() == Role.MANAGER_N2) {
                u.setActif(false);
                userRepository.save(u);
                n2Found = true;
            }
        }
        if (!n2Found) {
            User n2 = new User("TestN2", "n2@test.com", "pass", Role.MANAGER_N2);
            n2.setActif(false);
            n2.setService("IT");
            userRepository.save(n2);
        }
    }
}

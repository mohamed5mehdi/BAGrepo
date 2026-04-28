package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import com.pfe.gestionsachat.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = GestionsAchatApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullIntegrationTest {

    // ─── Repositories ──────────────────────────────────────────────────────────
    @Autowired private UserRepository       userRepository;
    @Autowired private FamilyRepository     familyRepository;
    @Autowired private SubFamilyRepository  subFamilyRepository;
    @Autowired private DaHeaderRepository   daHeaderRepository;
    @Autowired private DaDetailsRepository  daDetailsRepository;

    // ─── Services ──────────────────────────────────────────────────────────────
    @Autowired private DaHeaderService          daHeaderService;
    @Autowired private AchatWorkflowOrchestrator orchestrator;
    @Autowired private FamilyService            familyService;
    @Autowired private SubFamilyService         subFamilyService;

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 1 : Vérification que le contexte et les données de démo sont chargés
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @Order(1)
    void test01_contextLoads_andDemoDataExists() {
        System.out.println("\n=== TEST 1 : Context + Données de démo ===");

        assertTrue(userRepository.count() > 0,     "❌ Aucun utilisateur en base !");
        assertTrue(familyRepository.count() > 0,    "❌ Aucune famille en base !");
        assertTrue(subFamilyRepository.count() > 0, "❌ Aucune sous-famille en base !");

        System.out.println("✅ Utilisateurs     : " + userRepository.count());
        System.out.println("✅ Familles         : " + familyRepository.count());
        System.out.println("✅ Sous-familles    : " + subFamilyRepository.count());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 2 : Vérification des rôles des utilisateurs
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @Order(2)
    void test02_users_haveCorrectRoles() {
        System.out.println("\n=== TEST 2 : Rôles des utilisateurs ===");

        List<User> users = userRepository.findAll();
        long countN1   = users.stream().filter(u -> u.getRole() == Role.ROLE_N1).count();
        long countDAF  = users.stream().filter(u -> u.getRole() == Role.ROLE_DAF).count();
        long countDG   = users.stream().filter(u -> u.getRole() == Role.ROLE_DG).count();
        long countDEM  = users.stream().filter(u -> u.getRole() == Role.ROLE_DEMANDEUR).count();

        assertTrue(countN1  >= 1, "❌ Aucun manager N1 !");
        assertTrue(countDAF >= 1, "❌ Aucun DAF !");
        assertTrue(countDG  >= 1, "❌ Aucun DG !");
        assertTrue(countDEM >= 1, "❌ Aucun Demandeur !");

        users.forEach(u -> System.out.println("✅ " + u.getNom() + " → " + u.getRole()));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 3 : Vérification de la relation Famille → Sous-Famille
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @Order(3)
    void test03_family_subFamily_relation() {
        System.out.println("\n=== TEST 3 : Relation Famille → Sous-Famille ===");

        List<Family> families = familyService.getAllFamilies();
        assertFalse(families.isEmpty(), "❌ Aucune famille trouvée !");

        Family family = families.get(0);
        System.out.println("✅ Famille : " + family.getLibelle() + " (Budget initial: " + family.getBudgetInitial() + ")");
        assertNotNull(family.getBudgetInitial(), "❌ Budget initial null !");
        assertNotNull(family.getBudgetRestant(), "❌ Budget restant null !");

        // Vérifier les sous-familles
        List<SubFamily> subFamilies = subFamilyService.getAllSubFamilies();
        assertFalse(subFamilies.isEmpty(), "❌ Aucune sous-famille trouvée !");
        subFamilies.forEach(sf ->
            System.out.println("  ✅ Sous-famille : " + sf.getLibelle() + " (Budget: " + sf.getBudgetRestant() + ")")
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 4 : Création complète d'une demande d'achat (DA)
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @Order(4)
    @Transactional
    void test04_createDemande_fullWorkflow() {
        System.out.println("\n=== TEST 4 : Création complète d'une DA ===");

        // Récupérer un demandeur existant
        User demandeur = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_DEMANDEUR)
                .findFirst()
                .orElseThrow();
        assertNotNull(demandeur, "❌ Demandeur introuvable !");
        System.out.println("✅ Demandeur : " + demandeur.getNom() + " (ID: " + demandeur.getOidUser() + ")");

        // Récupérer une sous-famille pour les détails
        SubFamily subFamily = subFamilyRepository.findAll().get(0);
        assertNotNull(subFamily, "❌ Sous-famille introuvable !");

        // Construire la demande
        DaHeader da = new DaHeader();
        da.setObjet("Test DA - Achat PC Portable Stage");
        da.setJustification("Besoin d'un ordinateur pour le stagiaire");
        da.setUrgencyLevel("Normal");
        da.setDemandeur(demandeur);
        // Le statut et la date sont forcés dans le service

        // Construire les lignes de détails
        DaDetails detail = new DaDetails();
        detail.setItemCode("REQ-2604-001");
        detail.setItemName("Dell Latitude 5420");
        detail.setDescription("16Go RAM, 512Go SSD, i7");
        detail.setQuantite(1);
        detail.setJustification("Besoin stagiaire informatique");
        detail.setSubFamily(subFamily);

        da.getDetails().add(detail);

        // Appel du service de création
        DaHeader saved = daHeaderService.createPurchaseRequest(da);

        // Vérifications
        assertNotNull(saved, "❌ La DA créée est null !");
        assertNotNull(saved.getOidDa(), "❌ L'ID de la DA n'a pas été généré !");
        assertEquals(StatutDA.EN_ATTENTE_N1, saved.getStatut(), "❌ Le statut initial est incorrect !");
        assertNotNull(saved.getDateCreation(), "❌ La date de création est null !");
        assertEquals("Test DA - Achat PC Portable Stage", saved.getObjet(), "❌ L'objet de la DA est incorrect !");

        System.out.println("✅ DA créée avec succès ! ID: " + saved.getOidDa());
        System.out.println("✅ Statut initial : " + saved.getStatut());
        System.out.println("✅ Date de création : " + saved.getDateCreation());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 5 : Vérification de la relation DA → Détails
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @Order(5)
    void test05_daHeader_hasDetails() {
        System.out.println("\n=== TEST 5 : Relation DA → Détails ===");

        List<DaHeader> allDAs = daHeaderRepository.findAll();
        assertFalse(allDAs.isEmpty(), "❌ Aucune DA trouvée en base !");

        DaHeader da = allDAs.get(0);
        System.out.println("✅ DA trouvée : " + da.getObjet() + " (Statut: " + da.getStatut() + ")");

        List<DaDetails> details = daDetailsRepository.findAll();
        assertFalse(details.isEmpty(), "❌ Aucun détail trouvé en base !");

        DaDetails detail = details.get(0);
        assertNotNull(detail.getDaHeader(), "❌ Le détail n'est pas lié à une DA !");
        System.out.println("✅ Détail : " + detail.getDescription() + " x" + detail.getQuantite());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 6 : Vérification de la transition de statut (Workflow N1)
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @Order(6)
    @Transactional
    void test06_workflowTransition_N1_to_TECH() {
        System.out.println("\n=== TEST 6 : Transition Workflow N1 → TECHNIQUE ===");

        // Créer une DA en état EN_ATTENTE_N1
        User demandeur = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_DEMANDEUR)
                .findFirst().orElseThrow();
        DaHeader da = new DaHeader("Workflow Test DA", demandeur);
        da.setStatut(StatutDA.EN_ATTENTE_N1);
        da = daHeaderRepository.save(da);
        Integer daId = da.getOidDa();

        // Récupérer le User N1
        User n1 = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_N1)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Aucun N1 trouvé !"));
        Integer n1Id = n1.getOidUser();

        System.out.println("✅ DA créée (ID=" + daId + ") | Statut: " + da.getStatut());
        System.out.println("✅ N1 validateur : " + n1.getNom() + " (ID=" + n1Id + ")");

        // Appliquer la validation N1
        DaHeader updated = orchestrator.processValidation(daId, n1Id, ValidationDecision.ACCEPTE, "RAS");

        assertEquals(StatutDA.EN_ATTENTE_TECH, updated.getStatut(),
                "❌ Après validation N1, le statut devrait être EN_ATTENTE_TECH !");
        System.out.println("✅ Nouveau statut après N1 : " + updated.getStatut());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 7 : Vérification du rejet d'une DA
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @Order(7)
    @Transactional
    void test07_workflowTransition_Reject() {
        System.out.println("\n=== TEST 7 : Rejet d'une DA ===");

        User demandeur = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_DEMANDEUR)
                .findFirst().orElseThrow();
        DaHeader da = new DaHeader("DA à rejeter", demandeur);
        da.setStatut(StatutDA.EN_ATTENTE_N1);
        da = daHeaderRepository.save(da);

        User n1 = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_N1)
                .findFirst().orElseThrow();

        DaHeader rejected = orchestrator.processValidation(da.getOidDa(), n1.getOidUser(),
                ValidationDecision.REJETE, "Budget insuffisant");

        assertEquals(StatutDA.REJETEE, rejected.getStatut(), "❌ Le statut devrait être REJETEE !");
        System.out.println("✅ DA rejetée correctement. Statut : " + rejected.getStatut());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 8 : Authentification Demandeur
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @Order(8)
    void test08_demandeur_auth_data() {
        System.out.println("\n=== TEST 8 : Données d'authentification ===");

        List<User> demandeurs = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_DEMANDEUR)
                .toList();
        assertFalse(demandeurs.isEmpty(), "❌ Aucun demandeur en base !");

        User d = demandeurs.get(0);
        assertNotNull(d.getEmail(), "❌ Email du demandeur null !");
        assertNotNull(d.getPassword(), "❌ Mot de passe du demandeur null !");
        assertNotNull(d.getOidUser(), "❌ ID du demandeur null !");

        System.out.println("✅ Demandeur : " + d.getNom());
        System.out.println("✅ Email     : " + d.getEmail());
        System.out.println("✅ ID        : " + d.getOidUser());
        System.out.println("✅ N1 associé (id): " + d.getN1Id());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 9 : Budget SubFamily suffisant
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @Order(9)
    void test09_subFamily_budget_check() {
        System.out.println("\n=== TEST 9 : Vérification Budget Sous-Famille ===");

        SubFamily sf = subFamilyRepository.findAll().get(0);
        assertNotNull(sf.getBudgetRestant(), "❌ Budget restant null !");

        boolean sufficient = sf.hasEnoughBudget(java.math.BigDecimal.valueOf(100.0));
        System.out.println("✅ Sous-famille : " + sf.getLibelle());
        System.out.println("✅ Budget restant : " + sf.getBudgetRestant());
        System.out.println("✅ Budget suffisant pour 100€ : " + sufficient);
        assertTrue(sufficient, "❌ Budget insuffisant pour 100€ dans la sous-famille de test !");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST 10 : Récupération des DA par statut
    // ──────────────────────────────────────────────────────────────────────────
    @Test
    @Order(10)
    void test10_getDAs_byStatus() {
        System.out.println("\n=== TEST 10 : Récupération des DA par statut ===");

        List<DaHeader> pending = daHeaderService.getPurchaseRequestsByStatus(StatutDA.EN_ATTENTE_N1);
        System.out.println("✅ DA en attente N1 : " + pending.size());

        List<DaHeader> all = daHeaderService.getAllPurchaseRequests();
        System.out.println("✅ Total DA en base : " + all.size());
        assertFalse(all.isEmpty(), "❌ Aucune DA trouvée en base !");

        all.forEach(da -> System.out.println("   - [" + da.getStatut() + "] " + da.getObjet()));
    }
}

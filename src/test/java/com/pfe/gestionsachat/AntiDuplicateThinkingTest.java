package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import com.pfe.gestionsachat.service.DemandeAchatInterneService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AntiDuplicateThinkingTest {

    @Autowired
    private DemandeAchatInterneService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DemandeAchatInterneRepository repository;

    @Test
    @Transactional
    public void testIdempotencyWithToken() {
        System.out.println("=== TEST IDEMPOTENCE (Thinking Ultime) ===");
        User demandeur = userRepository.findAll().get(0);
        String token = "TOKEN-" + UUID.randomUUID().toString();

        DemandeAchatInterne da1 = new DemandeAchatInterne();
        da1.setDesignation("Laptop Dell XPS");
        da1.setQuantite(2);
        da1.setSubmissionToken(token);

        DemandeAchatInterne saved1 = service.createDemande(da1, demandeur);
        System.out.println("Première insertion effectuée : ID=" + saved1.getId());
        assertNotNull(saved1.getId());

        // Seconde tentative avec le MÊME jeton
        DemandeAchatInterne da2 = new DemandeAchatInterne();
        da2.setDesignation("Laptop Dell XPS");
        da2.setQuantite(2);
        da2.setSubmissionToken(token);

        DemandeAchatInterne saved2 = service.createDemande(da2, demandeur);
        System.out.println("Seconde insertion (même token) : ID=" + saved2.getId());

        assertEquals(saved1.getId(), saved2.getId(), "L'idempotence a échoué : les IDs devraient être identiques.");
        System.out.println("✅ Succès Idempotence : Aucun doublon créé.");
    }

    @Test
    @Transactional
    public void testAntiFloodProtection() {
        System.out.println("=== TEST ANTI-FLOOD (Thinking Ultime) ===");
        User demandeur = userRepository.findAll().get(0);

        DemandeAchatInterne da1 = new DemandeAchatInterne();
        da1.setDesignation("Fournitures Bureau");
        da1.setQuantite(100);
        da1.setSubmissionToken("TOKEN-A-" + UUID.randomUUID());

        service.createDemande(da1, demandeur);
        System.out.println("Première demande 'Fournitures Bureau' enregistrée.");

        // Seconde demande identique mais jeton différent (ou absent) - Simulation Spam
        DemandeAchatInterne da2 = new DemandeAchatInterne();
        da2.setDesignation("Fournitures Bureau");
        da2.setQuantite(100);
        da2.setSubmissionToken("TOKEN-B-" + UUID.randomUUID());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            service.createDemande(da2, demandeur);
        });

        System.out.println("Message d'erreur intercepté : " + exception.getMessage());
        assertTrue(exception.getMessage().contains("identique a été soumise"), "L'anti-flood n'a pas bloqué la demande identique.");
        System.out.println("✅ Succès Anti-Flood : Le spam a été détecté et bloqué.");
    }
}

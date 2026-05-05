package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import com.pfe.gestionsachat.service.AchatWorkflowOrchestrator;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class LocalhostPoTest {

    private static final Logger log = LoggerFactory.getLogger(LocalhostPoTest.class);

    @Autowired private AchatWorkflowOrchestrator orchestrator;
    @Autowired private DaHeaderRepository daHeaderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SubFamilyRepository subFamilyRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private PurchaseOrderRepository purchaseOrderRepository;

    @Test
    @Transactional
    public void testFullPoGenerationFlow() {
        log.info("=== 🚀 DÉBUT DU TEST LOCALHOST : GÉNÉRATION BON DE COMMANDE ===");

        // 1. Identification des acteurs (depuis le seeding)
        User acheteur = userRepository.findByEmail("acheteur@test.com").orElseThrow();
        User amg      = userRepository.findByEmail("amg@test.com").orElseThrow();
        User daf      = userRepository.findByEmail("daf@test.com").orElseThrow();
        User dg       = userRepository.findByEmail("dg@test.com").orElseThrow();

        // 2. Récupération d'une DA en attente d'achat (Seedée par DataInitializer)
        DaHeader da = daHeaderRepository.findAll().stream()
                .filter(d -> d.getStatut() == StatutDA.EN_ATTENTE_ACHAT)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Aucune DA en attente d'achat trouvée"));

        log.info("📍 DA trouvée : {} (ID: {}) | Statut: {}", da.getObjet(), da.getOidDa(), da.getStatut());
        
        SubFamily sf = da.getDetails().get(0).getSubFamily();
        BigDecimal budgetInitialSf = sf.getBudgetRestant();
        log.info("💰 Budget disponible avant : {} MAD (SF: {})", budgetInitialSf, sf.getLibelle());

        // 3. Étape Acheteur : Vérification Budget
        log.info("⚖️ Étape 1 : L'acheteur vérifie le budget...");
        orchestrator.verifierBudget(da.getOidDa(), acheteur.getOidUser());
        da = daHeaderRepository.findById(da.getOidDa()).orElseThrow();
        assertEquals(StatutDA.EN_ATTENTE_AMG, da.getStatut());
        log.info("✅ Budget suffisant. Statut -> {}", da.getStatut());

        // 4. Étape AMG : Validation
        log.info("👤 Étape 2 : Validation AMG...");
        orchestrator.processValidation(da.getOidDa(), amg.getOidUser(), ValidationDecision.ACCEPTE, "Approuvé par AMG");
        da = daHeaderRepository.findById(da.getOidDa()).orElseThrow();
        // Selon le workflow, sans transfert, ça va vers DG ou DAF
        log.info("✅ AMG validé. Statut -> {}", da.getStatut());

        // 5. Étape DAF (si nécessaire) et DG : Validation Finale
        if (da.getStatut() == StatutDA.EN_ATTENTE_DAF) {
            log.info("👤 Étape 3 : Validation DAF...");
            orchestrator.processValidation(da.getOidDa(), daf.getOidUser(), ValidationDecision.ACCEPTE, "Budget OK pour DAF");
            da = daHeaderRepository.findById(da.getOidDa()).orElseThrow();
        }

        log.info("👤 Étape 4 : Validation DG (Arbitrage Final)...");
        orchestrator.processValidation(da.getOidDa(), dg.getOidUser(), ValidationDecision.ACCEPTE, "Approbation finale DG");
        da = daHeaderRepository.findById(da.getOidDa()).orElseThrow();
        assertEquals(StatutDA.VALIDEE, da.getStatut());
        log.info("🎉 DA VALIDÉE ! Prête pour le Bon de Commande.");

        // 6. GÉNÉRATION DU BON DE COMMANDE (PO)
        log.info("📦 Étape 5 : Génération du Bon de Commande (PO)...");
        PurchaseOrder po = orchestrator.manualCreatePO(da.getOidDa(), acheteur.getOidUser());
        
        assertNotNull(po);
        log.info("📄 PO GÉNÉRÉ avec succès ! ID: PO-{}", po.getIdPo());
        log.info("💵 Montant Total TTC (20%): {} MAD", po.getMontantTotal());

        // 7. Vérification de l'imputation budgétaire
        SubFamily sfApres = subFamilyRepository.findById(sf.getOidSub()).orElseThrow();
        BigDecimal montantHt = da.getDetails().get(0).getTotalPrice();
        BigDecimal budgetAttendu = budgetInitialSf.subtract(montantHt);
        
        log.info("📊 Vérification Budget : Avant={} | MontantHT={} | Après={}", budgetInitialSf, montantHt, sfApres.getBudgetRestant());
        assertEquals(0, budgetAttendu.compareTo(sfApres.getBudgetRestant()), "L'imputation budgétaire sur la Sous-Famille est incorrecte !");

        Family famApres = familyRepository.findById(sf.getFamily().getIdFamily()).orElseThrow();
        log.info("📊 Budget Famille '{}' mis à jour : {} MAD", famApres.getLibelle(), famApres.getBudgetRestant());

        log.info("=== ✅ TEST RÉUSSI : FLUX PO OPÉRATIONNEL (Thinking Ultime) ===");
    }
}

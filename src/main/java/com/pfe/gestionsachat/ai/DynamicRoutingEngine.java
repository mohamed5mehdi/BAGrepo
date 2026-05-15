package com.pfe.gestionsachat.ai;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.DemandeAchatInterneRepository;
import com.pfe.gestionsachat.repository.FamilyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DynamicRoutingEngine — Moteur de scoring multi-critères pour le routage des DAs.
 *
 * Score = f(montant, urgence, consommation_budget_famille, anomalie)
 *
 * Seuils de routage :
 *   score < 35  → Validateur Manager (circuit normal)
 *   35 ≤ s < 65 → AMG + DAF requis
 *   65 ≤ s < 85 → DAF obligatoire
 *   score ≥ 85  → Escalade DG (Direction Générale)
 */
@Service
@Transactional(readOnly = true)
public class DynamicRoutingEngine {

    @Autowired private DemandeAchatInterneRepository daRepo;
    @Autowired private FamilyRepository familyRepo;
    @Autowired private AnomalyDetectionService anomalyService;

    // ─── Seuils montants (MAD) ────────────────────────────────────────────────
    @org.springframework.beans.factory.annotation.Value("${bag.routing.seuil-daf:30000}")
    private BigDecimal seuilDaf;
    
    @org.springframework.beans.factory.annotation.Value("${bag.routing.seuil-dg:100000}")
    private BigDecimal seuilDg;

    // ─── DTO ──────────────────────────────────────────────────────────────────

    public record RoutingDecision(
        Long   daId,
        String designation,
        int    score,
        String suggestedRole,
        double confidence,
        List<String> facteurs,
        String  badgeLabel,
        String  badgeColor   // "green" / "orange" / "red"
    ) {}

    // ─── API ──────────────────────────────────────────────────────────────────

    /**
     * Calcule le routage pour une DA spécifique.
     */
    public RoutingDecision calculerRoutage(Long daId) {
        DemandeAchatInterne da = daRepo.findById(daId)
            .orElseThrow(() -> new IllegalArgumentException("DA introuvable : " + daId));
        
        List<AnomalyDetectionService.AnomalyResult> singleAnomaly = anomalyService.scorerDa(daId)
            .map(List::of)
            .orElse(Collections.emptyList());
            
        return scorerDA(da, singleAnomaly);
    }

    /**
     * Calcule le routage pour toutes les DAs en attente.
     * Utilisé pour pré-afficher les badges sur ValidatorPage / DAFPage / DGPage.
     */
    public List<RoutingDecision> routerDasEnAttente() {
        List<StatutDemande> enAttente = List.of(
            StatutDemande.SOUMISE, StatutDemande.VALIDE_N1, StatutDemande.VALIDE_TECH,
            StatutDemande.VALIDE_AMG, StatutDemande.VALIDE_DAF,
            StatutDemande.VALIDE_DG, StatutDemande.EN_TRAITEMENT
        );

        List<DemandeAchatInterne> das = daRepo.findByStatutIn(enAttente);

        List<AnomalyDetectionService.AnomalyResult> anomalies = anomalyService.detecterAnomalies();

        return das.stream()
            .map(da -> scorerDA(da, anomalies))
            .sorted(Comparator.comparingInt(RoutingDecision::score).reversed())
            .collect(Collectors.toList());
    }

    // ─── Moteur de scoring ────────────────────────────────────────────────────

    private RoutingDecision scorerDA(DemandeAchatInterne da,
                                      List<AnomalyDetectionService.AnomalyResult> anomalies) {
        List<String> facteurs = new ArrayList<>();
        int score = 0;

        // ── Critère 1 : Montant (0-40 pts) ───────────────────────────────────
        BigDecimal montant = da.getMontantEstime();
        boolean bypassDg = false;

        if (montant == null || montant.compareTo(BigDecimal.ZERO) == 0) {
            // Faille de contournement : une DA sans montant est très suspecte
            score += 30;
            facteurs.add("⚠️ Montant non renseigné : estimation requise");
            montant = BigDecimal.ZERO;
        } else if (montant.compareTo(seuilDg) >= 0) {
            score += 40;
            bypassDg = true; // Règle impérative DG
            facteurs.add(String.format("Montant élevé : %.0f MAD (> %s MAD)", montant, seuilDg.toPlainString()));
        } else if (montant.compareTo(seuilDaf) >= 0) {
            int pts = 20 + montant.subtract(seuilDaf)
                .multiply(BigDecimal.valueOf(20))
                .divide(seuilDg.subtract(seuilDaf), 0, RoundingMode.HALF_UP)
                .intValue();
            score += Math.min(pts, 40);
            facteurs.add(String.format("Montant intermédiaire : %.0f MAD", montant));
        } else if (montant.compareTo(BigDecimal.valueOf(10_000)) >= 0) {
            score += 10;
            facteurs.add(String.format("Montant modéré : %.0f MAD", montant));
        }

        // ── Critère 2 : Urgence (0-20 pts) ───────────────────────────────────
        if (da.getUrgence() == UrgenceDemande.CRITIQUE) {
            score += 20;
            facteurs.add("Urgence CRITIQUE — escalade recommandée");
        } else if (da.getUrgence() == UrgenceDemande.URGENTE) {
            score += 10;
            facteurs.add("Urgence URGENTE");
        }

        // ── Critère 3 : Consommation budget famille (0-20 pts) ────────────────
        if (da.getBudgetFamille() != null) {
            Family f = da.getBudgetFamille();
            if (f.getBudgetInitial() != null
                    && f.getBudgetInitial().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal engage = f.getBudgetEngage() != null ? f.getBudgetEngage() : BigDecimal.ZERO;
                double tauxConso = engage.multiply(BigDecimal.valueOf(100))
                    .divide(f.getBudgetInitial(), 2, RoundingMode.HALF_UP)
                    .doubleValue();

                if (tauxConso >= 90) {
                    score += 20;
                    facteurs.add(String.format("Budget famille à %.0f%% (critique)", tauxConso));
                } else if (tauxConso >= 70) {
                    score += 12;
                    facteurs.add(String.format("Budget famille à %.0f%% (élevé)", tauxConso));
                } else if (tauxConso >= 50) {
                    score += 5;
                    facteurs.add(String.format("Budget famille à %.0f%%", tauxConso));
                }
            }
        }

        // ── Critère 4 : Anomalie détectée (0-20 pts) ─────────────────────────
        Optional<AnomalyDetectionService.AnomalyResult> anomalie = anomalies.stream()
            .filter(a -> a.daId().equals(da.getId()))
            .findFirst();

        if (anomalie.isPresent()) {
            AnomalyDetectionService.AnomalyResult a = anomalie.get();
            if ("CRITIQUE".equals(a.niveau())) {
                score += 20;
                facteurs.add("⚠️ Anomalie CRITIQUE détectée : " + a.raison());
            } else {
                score += 10;
                facteurs.add("⚠️ Anomalie SUSPECT : " + a.raison());
            }
        }

        score = Math.min(score, 100);

        // ── Décision de routage ───────────────────────────────────────────────
        String role;
        String badgeLabel;
        String badgeColor;
        double confidence;

        if (bypassDg || score >= 85) {
            role = "DG";
            badgeLabel = "Escalade DG requise";
            badgeColor = "red";
            confidence = bypassDg ? 1.0 : 0.90 + (score - 85) * 0.001;
        } else if (score >= 65) {
            role = "DAF";
            badgeLabel = "Validation DAF obligatoire";
            badgeColor = "orange";
            confidence = 0.75 + (score - 65) * 0.005;
        } else if (score >= 35) {
            role = "AMG + DAF";
            badgeLabel = "Revue AMG conseillée";
            badgeColor = "orange";
            confidence = 0.60 + (score - 35) * 0.005;
        } else {
            role = "Manager N1";
            badgeLabel = "Circuit standard";
            badgeColor = "green";
            confidence = 0.85;
        }

        return new RoutingDecision(
            da.getId(),
            da.getDesignation(),
            score,
            role,
            Math.round(Math.min(confidence, 0.99) * 100.0) / 100.0,
            facteurs,
            badgeLabel,
            badgeColor
        );
    }
}

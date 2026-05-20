package com.pfe.gestionsachat.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AIController — Expose les 5 endpoints IA.
 *
 * GET /api/ai/dashboard → KPIs + DepensesParCategorie + EvolutionMensuelle +
 * ConsommationBudget
 * GET /api/ai/anomalies → Liste anomalies Z-score
 * GET /api/ai/routing → Routage de toutes les DAs en attente
 * GET /api/ai/routing/{id}→ Routage d'une DA spécifique
 * GET /api/ai/insights → Rapport textuel + résumé chatbot
 * GET /api/ai/delays → Prédictions délais par rôle
 */
@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AggregationService aggregationService;
    @Autowired
    private AnomalyDetectionService anomalyService;
    @Autowired
    private DynamicRoutingEngine routingEngine;
    @Autowired
    private InsightGeneratorService insightService;
    @Autowired
    private DelayPredictionService delayService;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(Map.of(
                "kpi", aggregationService.getKpiGlobal(),
                "depensesCategorie", aggregationService.getDepensesParCategorie(),
                "evolutionMensuelle", aggregationService.getEvolutionMensuelle(),
                "depensesDepartement", aggregationService.getDepensesParDepartement(),
                "consommationBudget", aggregationService.getConsommationBudget()));
    }

    @GetMapping("/anomalies")
    public ResponseEntity<List<AnomalyDetectionService.AnomalyResult>> getAnomalies() {
        return ResponseEntity.ok(anomalyService.detecterAnomalies());
    }

    @GetMapping("/routing")
    public ResponseEntity<List<DynamicRoutingEngine.RoutingDecision>> getRoutingAll() {
        return ResponseEntity.ok(routingEngine.routerDasEnAttente());
    }

    @GetMapping("/routing/{daId}")
    public ResponseEntity<DynamicRoutingEngine.RoutingDecision> getRoutingForDa(
            @PathVariable Long daId) {
        return ResponseEntity.ok(routingEngine.calculerRoutage(daId));
    }

    @GetMapping("/insights")
    public ResponseEntity<InsightGeneratorService.InsightReport> getInsights() {
        return ResponseEntity.ok(insightService.genererInsights());
    }

    @GetMapping("/delays")
    public ResponseEntity<DelayPredictionService.PredictionGlobale> getDelays() {
        return ResponseEntity.ok(delayService.getPredictionsGlobales());
    }

    /**
     * GET /api/ai/decision/{daId}
     * Agrège : routage + anomalie + recommandation finale VALIDER / ATTENTION /
     * REJETER
     * Consommé par ValidatorPage et DgPage au moment de l'arbitrage.
     */
    @GetMapping("/decision/{daId}")
    public ResponseEntity<?> getDecision(@PathVariable Long daId) {
        try {
            DynamicRoutingEngine.RoutingDecision routing = routingEngine.calculerRoutage(daId);
            Optional<AnomalyDetectionService.AnomalyResult> anomalie = anomalyService.scorerDa(daId);

            // ── Recommandation finale ─────────────────────────────────────────────
            String recommandation;
            String recommandationColor; // green / orange / red
            String recommandationIcon;
            List<String> justifications = new ArrayList<>(routing.facteurs());

            boolean anomalieCritique = anomalie.map(a -> "CRITIQUE".equals(a.niveau())).orElse(false);
            boolean anomalieSuspect = anomalie.map(a -> "SUSPECT".equals(a.niveau())).orElse(false);

            if (anomalieCritique || routing.score() >= 85) {
                recommandation = "REJETER";
                recommandationColor = "red";
                recommandationIcon = "🔴";
                if (anomalieCritique)
                    justifications.add(0, "⛔ Anomalie financière CRITIQUE détectée (Z-score)");
            } else if (anomalieSuspect || routing.score() >= 50) {
                recommandation = "ATTENTION";
                recommandationColor = "orange";
                recommandationIcon = "🟠";
                if (anomalieSuspect)
                    justifications.add(0, "⚠️ Montant statistiquement suspect (Z-score)");
            } else {
                recommandation = "VALIDER";
                recommandationColor = "green";
                recommandationIcon = "🟢";
            }

            return ResponseEntity.ok(Map.of(
                    "daId", daId,
                    "score", routing.score(),
                    "suggestedRole", routing.suggestedRole(),
                    "confidence", routing.confidence(),
                    "recommandation", recommandation,
                    "recommandationColor", recommandationColor,
                    "recommandationIcon", recommandationIcon,
                    "justifications", justifications,
                    "anomalie", anomalie.<Object>map(a -> Map.of(
                            "niveau", a.niveau(),
                            "score", a.score(),
                            "raison", a.raison(),
                            "zScore", a.zScore())).orElse(null)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur interne IA: " + e.getMessage()));
        }
    }
}

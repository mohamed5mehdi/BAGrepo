package com.pfe.gestionsachat.ai;

import com.pfe.gestionsachat.repository.FamilyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * InsightGeneratorService — Génère des recommandations textuelles paramétrées.
 *
 * Pas de LLM : templates basés sur les vraies données agrégées.
 * Le résultat est prêt pour le dashboard BI et l'intégration chatbot
 * (commande "résumé" / "analyse" dans DialogService).
 */
@Service
@Transactional(readOnly = true)
public class InsightGeneratorService {

    @Autowired private AggregationService aggregationService;
    @Autowired private AnomalyDetectionService anomalyService;
    @Autowired private DelayPredictionService delayService;
    @Autowired private FamilyRepository familyRepo;

    // ─── DTO ──────────────────────────────────────────────────────────────────

    public record Insight(
        String  type,       // "BUDGET" / "ANOMALIE" / "DELAI" / "OPPORTUNITE"
        String  icone,
        String  titre,
        String  description,
        String  priorite,   // "HAUTE" / "MOYENNE" / "INFO"
        String  actionSuggeree
    ) {}

    public record InsightReport(
        List<Insight> insights,
        String        resumeChatbot  // Version courte pour réponse chatbot
    ) {}

    // ─── API ──────────────────────────────────────────────────────────────────

    /**
     * Génère jusqu'à 5 insights triés par priorité.
     * Appelé par le dashboard BI et le chatbot.
     */
    public InsightReport genererInsights() {
        List<Insight> all = new ArrayList<>();

        List<AggregationService.ConsommationBudget> consos = aggregationService.getConsommationBudget();

        all.addAll(insightsBudget(consos));
        all.addAll(insightsAnomalies());
        all.addAll(insightsDelais());
        all.addAll(insightsOpportunites(consos));

        // Trier : HAUTE → MOYENNE → INFO
        List<Insight> sorted = all.stream()
            .sorted(Comparator.comparingInt(i -> prioriteOrdre(i.priorite())))
            .limit(5)
            .collect(Collectors.toList());

        String resume = buildResumeChatbot(sorted);
        return new InsightReport(sorted, resume);
    }

    // ─── Insights Budget ──────────────────────────────────────────────────────

    private List<Insight> insightsBudget(List<AggregationService.ConsommationBudget> consos) {
        List<Insight> insights = new ArrayList<>();

        for (AggregationService.ConsommationBudget conso : consos) {
            double taux = conso.tauxConsommation();

            if (taux >= 90) {
                insights.add(new Insight(
                    "BUDGET",
                    "🚨",
                    "Budget critique : " + conso.familleLibelle(),
                    String.format(
                        "Le budget '%s' est consommé à %.0f%% (%.0f MAD restants sur %.0f MAD). " +
                        "Toute nouvelle demande dans cette catégorie nécessitera une approbation DG.",
                        conso.familleLibelle(), taux,
                        conso.budgetRestant(), conso.budgetInitial()),
                    "HAUTE",
                    "Solliciter un transfert budgétaire ou reporter les demandes non urgentes."
                ));
            } else if (taux >= 70) {
                insights.add(new Insight(
                    "BUDGET",
                    "⚠️",
                    "Budget élevé : " + conso.familleLibelle(),
                    String.format(
                        "La catégorie '%s' a consommé %.0f%% de son budget annuel. " +
                        "Il reste %.0f MAD disponibles.",
                        conso.familleLibelle(), taux, conso.budgetRestant()),
                    "MOYENNE",
                    "Prioriser les demandes restantes et effectuer une revue des besoins."
                ));
            }
        }

        return insights;
    }

    // ─── Insights Anomalies ───────────────────────────────────────────────────

    private List<Insight> insightsAnomalies() {
        List<Insight> insights = new ArrayList<>();

        List<AnomalyDetectionService.AnomalyResult> anomalies = anomalyService.detecterAnomalies();

        long critiques = anomalies.stream().filter(a -> "CRITIQUE".equals(a.niveau())).count();
        long suspectes = anomalies.stream().filter(a -> "SUSPECT".equals(a.niveau())).count();

        if (critiques > 0) {
            AnomalyDetectionService.AnomalyResult top = anomalies.get(0);
            insights.add(new Insight(
                "ANOMALIE",
                "🔴",
                critiques + " anomalie(s) critique(s) détectée(s)",
                String.format(
                    "%d demande(s) présentent des montants statistiquement anormaux. " +
                    "La plus critique : DA #%d '%s' — %s",
                    critiques, top.daId(), top.designation(), top.raison()),
                "HAUTE",
                "Examiner et valider manuellement ces demandes avant approbation."
            ));
        } else if (suspectes > 0) {
            insights.add(new Insight(
                "ANOMALIE",
                "🟡",
                suspectes + " demande(s) suspecte(s)",
                String.format(
                    "%d demande(s) ont des montants supérieurs à 2 écarts-types de la normale " +
                    "pour leur catégorie. Vérification recommandée.",
                    suspectes),
                "MOYENNE",
                "Demander des devis comparatifs pour ces articles."
            ));
        }

        return insights;
    }

    // ─── Insights Délais ──────────────────────────────────────────────────────

    private List<Insight> insightsDelais() {
        List<Insight> insights = new ArrayList<>();

        DelayPredictionService.PredictionGlobale pred = delayService.getPredictionsGlobales();

        if (pred.bottleneck() != null && !pred.bottleneck().equals("N/A")) {
            DelayPredictionService.PredictionDelai bottleneck = pred.parRole().stream()
                .filter(p -> p.role().equals(pred.bottleneck()))
                .findFirst().orElse(null);

            if (bottleneck != null && "ELEVE".equals(bottleneck.niveauCharge())) {
                insights.add(new Insight(
                    "DELAI",
                    "⏱️",
                    "Goulot d'étranglement : " + bottleneck.role(),
                    String.format(
                        "%s est le validateur le plus sollicité actuellement (%d DA(s) en attente). " +
                        "Délai de validation prédit : %.0f heures (%.1f jours).",
                        bottleneck.role(), bottleneck.dasEnAttente(),
                        bottleneck.heuresPredites(), bottleneck.heuresPredites() / 24),
                    "MOYENNE",
                    "Envisager une délégation temporaire ou replanifier les soumissions."
                ));
            }
        }

        return insights;
    }

    // ─── Insights Opportunités ────────────────────────────────────────────────

    private List<Insight> insightsOpportunites(List<AggregationService.ConsommationBudget> consos) {
        List<Insight> insights = new ArrayList<>();

        // Identifier la catégorie avec le plus de dépenses → suggestion renégociation
        List<AggregationService.DepenseParCategorie> depenses = aggregationService.getDepensesParCategorie();

        if (!depenses.isEmpty()) {
            AggregationService.DepenseParCategorie top = depenses.get(0);
            BigDecimal economie = top.montant()
                .multiply(BigDecimal.valueOf(0.07))
                .setScale(0, RoundingMode.HALF_UP);

            insights.add(new Insight(
                "OPPORTUNITE",
                "💡",
                "Opportunité d'économie : " + top.categorie(),
                String.format(
                    "La catégorie '%s' représente %.0f MAD de dépenses (%d demandes). " +
                    "Une renégociation des contrats fournisseurs pourrait générer ~%s MAD d'économies (7%%).",
                    top.categorie(), top.montant(), top.count(), economie.toPlainString()),
                "INFO",
                "Lancer un appel d'offres groupé pour cette catégorie."
            ));
        }

        // Identifier les budgets sous-utilisés (< 20% consommés)
        consos.stream()
            .filter(c -> c.tauxConsommation() < 20 && c.budgetRestant().compareTo(BigDecimal.valueOf(50_000)) > 0)
            .findFirst()
            .ifPresent(c -> insights.add(new Insight(
                "OPPORTUNITE",
                "💰",
                "Budget sous-utilisé : " + c.familleLibelle(),
                String.format(
                    "Le budget '%s' est utilisé à seulement %.0f%%. " +
                    "%.0f MAD sont disponibles et pourraient financer des projets différés.",
                    c.familleLibelle(), c.tauxConsommation(), c.budgetRestant()),
                "INFO",
                "Proposer aux départements concernés de soumettre des besoins différés."
            )));

        return insights;
    }

    // ─── Résumé chatbot ───────────────────────────────────────────────────────

    private String buildResumeChatbot(List<Insight> insights) {
        if (insights.isEmpty()) {
            return "📊 Tout est nominal. Aucune anomalie ni alerte budgétaire détectée.";
        }

        StringBuilder sb = new StringBuilder("📊 **Résumé IA — BAG Procurement**\n\n");

        long hautes   = insights.stream().filter(i -> "HAUTE".equals(i.priorite())).count();
        long moyennes = insights.stream().filter(i -> "MOYENNE".equals(i.priorite())).count();

        if (hautes > 0) {
            sb.append("🔴 **").append(hautes).append(" alerte(s) haute(s) nécessitent votre attention.**\n");
        }
        if (moyennes > 0) {
            sb.append("🟡 **").append(moyennes).append(" point(s) à surveiller.**\n");
        }

        sb.append("\n");
        for (Insight i : insights) {
            sb.append(i.icone()).append(" **").append(i.titre()).append("**\n");
            sb.append("  → ").append(i.actionSuggeree()).append("\n\n");
        }

        sb.append("*Analyse basée sur ").append(getNumberOfDAs()).append(" demandes d'achat.*");
        return sb.toString();
    }

    private int prioriteOrdre(String priorite) {
        return switch (priorite) {
            case "HAUTE"   -> 0;
            case "MOYENNE" -> 1;
            default        -> 2;
        };
    }

    private long getNumberOfDAs() {
        try {
            return aggregationService.getKpiGlobal().totalDAs();
        } catch (Exception e) {
            return 0;
        }
    }
}

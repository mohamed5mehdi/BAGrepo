package com.pfe.gestionsachat.ai;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DelayPredictionService — Prédit le délai de validation par rôle.
 *
 * Modèle : moyenne glissante pondérée sur les 30 dernières validations
 * réelles (StatusHistory), avec facteur de charge dynamique.
 *
 * Résultat transmis au chatbot et aux badges des pages validateur/DAF/DG.
 */
@Service
@Transactional(readOnly = true)
public class DelayPredictionService {

    @PersistenceContext
    private EntityManager em;

    @Autowired private StatusHistoryRepository statusHistoryRepo;
    @Autowired private DemandeAchatInterneRepository daRepo;

    // Seuils de charge (nb DAs en attente pour ce rôle)
    private static final int CHARGE_HAUTE  = 15;
    private static final int CHARGE_MOYENNE = 7;

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    public record PredictionDelai(
        String role,
        double moyenneHeuresHistorique,
        double heuresPredites,
        String niveauCharge,   // FAIBLE / MOYEN / ELEVE
        long   dasEnAttente,
        String messageDisplay  // Prêt pour le chatbot et les badges UI
    ) {}

    public record PredictionGlobale(
        List<PredictionDelai> parRole,
        double delaiTotalEstime,
        String bottleneck
    ) {}

    // ─── API principale ───────────────────────────────────────────────────────

    /**
     * Retourne les prédictions pour tous les rôles du circuit de validation.
     */
    public PredictionGlobale getPredictionsGlobales() {
        List<PredictionDelai> predictions = new ArrayList<>();

        Map<String, TransitionSpec> transitions = buildTransitionSpecs();

        for (Map.Entry<String, TransitionSpec> entry : transitions.entrySet()) {
            String role = entry.getKey();
            TransitionSpec spec = entry.getValue();

            double moyenne = calculerMoyenneHistorique(spec.statutAvant(), spec.statutApres());
            long charge = compterDasEnAttente(spec.statutEnAttente());
            String niveauCharge = niveauCharge(charge);
            double facteurCharge = facteurCharge(niveauCharge);
            double prediction = moyenne * facteurCharge;

            predictions.add(new PredictionDelai(
                role,
                Math.round(moyenne * 10.0) / 10.0,
                Math.round(prediction * 10.0) / 10.0,
                niveauCharge,
                charge,
                buildMessage(role, prediction, niveauCharge)
            ));
        }

        // Identifier le bottleneck (rôle avec le délai prédit le plus élevé)
        String bottleneck = predictions.stream()
            .max(Comparator.comparingDouble(PredictionDelai::heuresPredites))
            .map(PredictionDelai::role)
            .orElse("N/A");

        double totalEstime = predictions.stream()
            .mapToDouble(PredictionDelai::heuresPredites)
            .sum();

        return new PredictionGlobale(predictions,
            Math.round(totalEstime * 10.0) / 10.0, bottleneck);
    }

    /**
     * Prédit le délai pour une DA spécifique selon son statut courant.
     */
    public PredictionDelai getPredictionPourStatut(StatutDemande statut) {
        Map<String, TransitionSpec> specs = buildTransitionSpecs();

        for (Map.Entry<String, TransitionSpec> entry : specs.entrySet()) {
            TransitionSpec spec = entry.getValue();
            if (spec.statutEnAttente().contains(statut)) {
                String role = entry.getKey();
                double moyenne = calculerMoyenneHistorique(spec.statutAvant(), spec.statutApres());
                long charge = compterDasEnAttente(spec.statutEnAttente());
                String niveauCharge = niveauCharge(charge);
                double prediction = moyenne * facteurCharge(niveauCharge);

                return new PredictionDelai(role,
                    Math.round(moyenne * 10.0) / 10.0,
                    Math.round(prediction * 10.0) / 10.0,
                    niveauCharge, charge,
                    buildMessage(role, prediction, niveauCharge));
            }
        }

        return new PredictionDelai("N/A", 0, 0, "FAIBLE", 0, "Statut non prévisible.");
    }

    // ─── Calcul historique ────────────────────────────────────────────────────

    /**
     * Moyenne pondérée des délais de transition entre deux statuts.
     * Source : StatusHistory (vraies données).
     * Fenêtre : 30 dernières transitions.
     * Pondération : les transitions récentes ont un poids 2× supérieur.
     */
    private double calculerMoyenneHistorique(String statutAvant, String statutApres) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
            "SELECT h1.dateModification, h2.dateModification " +
            "FROM StatusHistory h1 JOIN StatusHistory h2 ON h1.entiteId = h2.entiteId AND h1.entiteType = h2.entiteType " +
            "WHERE h1.entiteType = 'DemandeAchatInterne' " +
            "  AND h1.statutApres = :avant " +
            "  AND h2.statutApres = :apres " +
            "  AND h2.dateModification > h1.dateModification " +
            "ORDER BY h1.dateModification DESC"
        )
        .setParameter("avant", statutAvant)
        .setParameter("apres", statutApres)
        .setMaxResults(30)
        .getResultList();

        if (rows.isEmpty()) return fallbackDelai(statutApres);

        // Moyenne pondérée : rang 1 (plus récent) → poids 2, rang n → poids 1
        double totalPoids = 0;
        double totalHeures = 0;
        int n = rows.size();

        for (int i = 0; i < n; i++) {
            java.time.LocalDateTime d1 = (java.time.LocalDateTime) rows.get(i)[0];
            java.time.LocalDateTime d2 = (java.time.LocalDateTime) rows.get(i)[1];
            
            // Calcul optimal des heures ouvrées (Lun-Ven, 08:00 - 18:00) avec protection anti-boucle infinie (OOM/CPU sink)
            long heures = 0;
            long maxHours = Math.min(java.time.temporal.ChronoUnit.HOURS.between(d1, d2), 8760); // Max 1 an
            java.time.LocalDateTime current = d1;
            for (long j = 0; j <= maxHours; j++) {
                java.time.DayOfWeek day = current.getDayOfWeek();
                int hour = current.getHour();
                if (day != java.time.DayOfWeek.SATURDAY && day != java.time.DayOfWeek.SUNDAY) {
                    if (hour >= 8 && hour < 18) {
                        heures++;
                    }
                }
                current = current.plusHours(1);
            }
            
            if (heures < 0) continue;
            double poids = 1.0 + (double)(n - i) / n; // 2.0 pour le plus récent, ~1.0 pour le plus ancien
            totalHeures += heures * poids;
            totalPoids  += poids;
        }

        return totalPoids == 0 ? fallbackDelai(statutApres) : totalHeures / totalPoids;
    }

    private long compterDasEnAttente(List<StatutDemande> statuts) {
        Object result = em.createQuery(
            "SELECT COUNT(d) FROM DemandeAchatInterne d WHERE d.statut IN :statuts"
        ).setParameter("statuts", statuts).getSingleResult();
        return ((Number) result).longValue();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private double facteurCharge(String niveau) {
        return switch (niveau) {
            case "ELEVE"  -> 1.8;
            case "MOYEN"  -> 1.3;
            default       -> 1.0;
        };
    }

    private String niveauCharge(long count) {
        if (count >= CHARGE_HAUTE)   return "ELEVE";
        if (count >= CHARGE_MOYENNE) return "MOYEN";
        return "FAIBLE";
    }

    private double fallbackDelai(String statutApres) {
        // Valeurs par défaut secteur automobile si aucun historique
        return switch (statutApres) {
            case "VALIDE_N1"    -> 12.0;
            case "VALIDE_TECH"  -> 24.0;
            case "VALIDE_AMG"   -> 36.0;
            case "VALIDE_DAF"   -> 48.0;
            case "VALIDE_DG"    -> 72.0;
            default                   -> 24.0;
        };
    }

    private String buildMessage(String role, double heures, String charge) {
        String duree = heures < 24
            ? String.format("~%.0fh", heures)
            : String.format("~%.1f jour(s)", heures / 24);

        return switch (charge) {
            case "ELEVE" -> String.format(
                "⚠️ %s est très sollicité(e) actuellement. Délai estimé : %s (charge élevée).", role, duree);
            case "MOYEN" -> String.format(
                "📊 %s traite les demandes sous %s en moyenne.", role, duree);
            default -> String.format(
                "✅ %s disponible. Traitement attendu sous %s.", role, duree);
        };
    }

    /**
     * Spécification d'une transition de validation.
     */
    private record TransitionSpec(
        String statutAvant,
        String statutApres,
        List<StatutDemande> statutEnAttente
    ) {}

    private Map<String, TransitionSpec> buildTransitionSpecs() {
        Map<String, TransitionSpec> specs = new LinkedHashMap<>();
        specs.put("Manager N1", new TransitionSpec(
            "SOUMISE", "VALIDE_N1",
            List.of(StatutDemande.SOUMISE)));
        specs.put("Technicien", new TransitionSpec(
            "VALIDE_N1", "VALIDE_TECH",
            List.of(StatutDemande.VALIDE_N1)));
        specs.put("AMG", new TransitionSpec(
            "VALIDE_TECH", "VALIDE_AMG",
            List.of(StatutDemande.VALIDE_TECH)));
        specs.put("DAF", new TransitionSpec(
            "VALIDE_AMG", "VALIDE_DAF",
            List.of(StatutDemande.VALIDE_AMG, StatutDemande.VALIDE_DAF)));
        specs.put("DG", new TransitionSpec(
            "VALIDE_DAF", "VALIDE_DG",
            List.of(StatutDemande.VALIDE_DG)));
        return specs;
    }
}

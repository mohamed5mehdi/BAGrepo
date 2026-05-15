package com.pfe.gestionsachat.ai;

import com.pfe.gestionsachat.model.*;
import com.pfe.gestionsachat.repository.DemandeAchatInterneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AnomalyDetectionService — Détection d'anomalies financières par Z-score.
 *
 * Algorithme :
 *   μ  = moyenne des montantEstime pour une (famille, sous-famille)
 *   σ  = écart-type de la même distribution
 *   z  = (montant - μ) / σ
 *
 *   |z| > 2.0 → SUSPECT  (score 60-79)
 *   |z| > 3.0 → CRITIQUE (score 80-100)
 *
 * Plancher statistique : n >= 5 observations par groupe.
 * En dessous → PAS de signal (évite les faux positifs sur petits échantillons).
 */
@Service
@Transactional(readOnly = true)
public class AnomalyDetectionService {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private DemandeAchatInterneRepository daRepo;

    private static final double Z_SUSPECT  = 2.0;
    private static final double Z_CRITIQUE = 3.0;
    private static final int    MIN_SAMPLE = 5;

    // ─── DTO ──────────────────────────────────────────────────────────────────

    public record AnomalyResult(
        Long   daId,
        String designation,
        String departement,
        String familleLibelle,
        String sousFamilleLibelle,
        BigDecimal montant,
        BigDecimal moyenneGroupe,
        BigDecimal ecartType,
        double  zScore,
        int     score,          // 0-100
        String  niveau,         // NORMAL / SUSPECT / CRITIQUE
        String  raison
    ) {}

    // ─── Cache des statistiques ───────────────────────────────────────────────
    private Map<String, double[]> statsCache = new HashMap<>();
    private long cacheLastUpdated = 0;
    private static final long CACHE_TTL_MS = 60_000; // 1 minute

    private synchronized void refreshStatsCacheIfNecessary() {
        if (System.currentTimeMillis() - cacheLastUpdated < CACHE_TTL_MS) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
            "SELECT " +
            "       CASE WHEN d.budgetFamille IS NOT NULL THEN d.budgetFamille.idFamily ELSE 0 END, " +
            "       CASE WHEN d.budgetSousFamille IS NOT NULL THEN d.budgetSousFamille.oidSub ELSE 0 END, " +
            "       d.montantEstime " +
            "FROM DemandeAchatInterne d " +
            "WHERE d.montantEstime IS NOT NULL AND d.budgetFamille IS NOT NULL"
        ).getResultList();

        Map<String, List<BigDecimal>> groupes = new HashMap<>();
        for (Object[] row : rows) {
            String key = row[0] + "_" + row[1];
            groupes.computeIfAbsent(key, k -> new ArrayList<>()).add((BigDecimal) row[2]);
        }

        Map<String, double[]> newStats = new HashMap<>();
        for (Map.Entry<String, List<BigDecimal>> entry : groupes.entrySet()) {
            List<BigDecimal> vals = entry.getValue();
            if (vals.size() < MIN_SAMPLE) continue;
            
            List<Double> sorted = vals.stream().map(BigDecimal::doubleValue).sorted().collect(Collectors.toList());
            double median = sorted.get(sorted.size() / 2);
            
            List<Double> deviations = sorted.stream().map(v -> Math.abs(v - median)).sorted().collect(Collectors.toList());
            double mad = deviations.get(deviations.size() / 2);
            double stdApprox = mad == 0 ? 1.0 : mad * 1.4826;

            newStats.put(entry.getKey(), new double[]{median, stdApprox, vals.size()});
        }

        this.statsCache = newStats;
        this.cacheLastUpdated = System.currentTimeMillis();
    }

    // ─── Point d'entrée ───────────────────────────────────────────────────────

    /**
     * Analyse toutes les DAs avec montantEstime non nul.
     * Retourne uniquement les anomalies détectées (niveau != NORMAL).
     */
    public List<AnomalyResult> detecterAnomalies() {
        return detecterAnomaliesInternes(null);
    }

    private List<AnomalyResult> detecterAnomaliesInternes(Long targetDaId) {
        refreshStatsCacheIfNecessary();

        List<AnomalyResult> anomalies = new ArrayList<>();
        
        String queryStr = targetDaId != null 
            ? "SELECT d.id, d.designation, d.departement, f.idFamily, f.libelle, sf.oidSub, sf.libelle, d.montantEstime " +
              "FROM DemandeAchatInterne d LEFT JOIN d.budgetFamille f LEFT JOIN d.budgetSousFamille sf " +
              "WHERE d.id = :daId AND d.montantEstime IS NOT NULL"
            : "SELECT d.id, d.designation, d.departement, f.idFamily, f.libelle, sf.oidSub, sf.libelle, d.montantEstime " +
              "FROM DemandeAchatInterne d LEFT JOIN d.budgetFamille f LEFT JOIN d.budgetSousFamille sf " +
              "WHERE d.montantEstime IS NOT NULL";
            
        var query = em.createQuery(queryStr, Object[].class);
        if (targetDaId != null) query.setParameter("daId", targetDaId);
        
        List<Object[]> dasAAnalyser = query.getResultList();

        for (Object[] row : dasAAnalyser) {
            Long daId = (Long) row[0];
            String designation = (String) row[1];
            String departement = (String) row[2];
            Integer fid = row[3] != null ? (Integer) row[3] : 0;
            String fLibelle = row[4] != null ? (String) row[4] : "N/A";
            Integer sfid = row[5] != null ? (Integer) row[5] : 0;
            String sfLibelle = row[6] != null ? (String) row[6] : "N/A";
            BigDecimal montantEstime = (BigDecimal) row[7];

            String key = fid + "_" + sfid;
            double[] stat = this.statsCache.get(key);
            if (stat == null) continue;

            double median = stat[0];
            double stdApprox = stat[1];

            double montant = montantEstime.doubleValue();
            double z = (montant - median) / stdApprox;
            double absZ = Math.abs(z);

            if (absZ < Z_SUSPECT) continue;

            String niveau;
            int score;
            if (absZ >= Z_CRITIQUE) {
                niveau = "CRITIQUE";
                score  = Math.min(100, 80 + (int)((absZ - Z_CRITIQUE) * 10));
            } else {
                niveau = "SUSPECT";
                score  = 60 + (int)((absZ - Z_SUSPECT) * 20);
            }

            double ratio = median > 0 ? montant / median : 1;
            String dir   = z > 0 ? "supérieur" : "inférieur";
            String raison = String.format(
                "Montant %.0f MAD %s de %.1fx à la normale (%.0f MAD) pour '%s'",
                montant, dir, ratio, median, fLibelle
            );

            anomalies.add(new AnomalyResult(
                daId,
                designation,
                departement,
                fLibelle,
                sfLibelle,
                montantEstime,
                BigDecimal.valueOf(median).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(stdApprox).setScale(2, RoundingMode.HALF_UP),
                Math.round(z * 100.0) / 100.0,
                score,
                niveau,
                raison
            ));
        }

        anomalies.sort(Comparator.comparingInt(AnomalyResult::score).reversed());
        return anomalies;
    }

    /**
     * Scorer une DA spécifique (pour badge en temps réel lors de la saisie).
     */
    public Optional<AnomalyResult> scorerDa(Long daId) {
        return detecterAnomaliesInternes(daId).stream().findFirst();
    }
}

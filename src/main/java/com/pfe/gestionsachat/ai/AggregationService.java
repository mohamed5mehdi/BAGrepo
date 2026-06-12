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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AggregationService — Moteur d'agrégation analytique.
 * Produit les KPIs et séries temporelles pour le dashboard BI.
 * Toutes les requêtes sont en JPQL natif sur les entités existantes.
 */
@Service
@Transactional(readOnly = true)
public class AggregationService {

    @PersistenceContext
    private EntityManager em;

    @Autowired private FamilyRepository familyRepo;
    @Autowired private DemandeAchatInterneRepository daRepo;


    // ─── DTOs internes ────────────────────────────────────────────────────────

    public record KpiGlobal(
        long totalDAs,
        long daEnAttente,
        long daApprouvees,
        long daRejetees,
        BigDecimal montantTotalEngage,
        BigDecimal montantEnAttente,
        double tauxApprobation,
        long anomaliesDetectees
    ) {}

    public record DepenseParCategorie(String categorie, BigDecimal montant, long count) {}

    public record EvolutionMensuelle(int annee, int mois, BigDecimal montant, long count) {}

    public record DepenseParDepartement(String departement, BigDecimal montant, long count) {}

    public record ConsommationBudget(
        String familleLibelle,
        BigDecimal budgetInitial,
        BigDecimal budgetEngage,
        BigDecimal budgetRestant,
        double tauxConsommation
    ) {}

    // ─── 1. KPIs Globaux ─────────────────────────────────────────────────────

    public KpiGlobal getKpiGlobal() {
        long total = daRepo.count();

        // DemandeAchatInterne Stats
        List<StatutDemande> enAttenteDAI = List.of(
            StatutDemande.SOUMISE, StatutDemande.VALIDE_N1, StatutDemande.VALIDE_TECH,
            StatutDemande.VALIDE_AMG, StatutDemande.VALIDE_DAF, StatutDemande.VALIDE_DG,
            StatutDemande.EN_TRAITEMENT
        );

        @SuppressWarnings("unchecked")
        List<Object[]> countByStatutDAI = em.createQuery(
            "SELECT d.statut, COUNT(d) FROM DemandeAchatInterne d GROUP BY d.statut"
        ).getResultList();

        long enAttenteCnt = 0L;
        long approuveeCnt = 0L;
        long rejeteeCount = 0L;

        for (Object[] row : countByStatutDAI) {
            StatutDemande s = (StatutDemande) row[0];
            long cnt = (long) row[1];
            if (enAttenteDAI.contains(s)) enAttenteCnt += cnt;
            if (s == StatutDemande.APPROUVEE || s == StatutDemande.PO_CREE || s == StatutDemande.AFFECTEE) approuveeCnt += cnt;
            if (s == StatutDemande.REJETEE) rejeteeCount += cnt;
        }

        BigDecimal montantEngageDAI = montantAggregeDAI(List.of(StatutDemande.APPROUVEE, StatutDemande.PO_CREE, StatutDemande.AFFECTEE));
        BigDecimal montantAttenteDAI = montantAggregeDAI(enAttenteDAI);

        double tauxApprobation = total == 0 ? 0.0 : Math.round((double) approuveeCnt / total * 10000.0) / 100.0;

        return new KpiGlobal(total, enAttenteCnt, approuveeCnt, rejeteeCount,
            montantEngageDAI, montantAttenteDAI, tauxApprobation, 0L);
    }

    // ─── 2. Dépenses par Catégorie ────────────────────────────────────────────

    public List<DepenseParCategorie> getDepensesParCategorie() {
        Map<String, DepenseParCategorie> map = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<Object[]> rowsDAI = em.createQuery(
            "SELECT d.categorie, SUM(d.montantEstime), COUNT(d) " +
            "FROM DemandeAchatInterne d " +
            "WHERE d.montantEstime IS NOT NULL " +
            "  AND d.statut IN :statuts " +
            "GROUP BY d.categorie"
        ).setParameter("statuts", List.of(StatutDemande.APPROUVEE, StatutDemande.PO_CREE, StatutDemande.AFFECTEE, StatutDemande.VALIDE_DAF, StatutDemande.VALIDE_DG)).getResultList();

        for(Object[] r : rowsDAI) {
            if (r[0] == null) continue;
            map.put(r[0].toString(), new DepenseParCategorie(r[0].toString(), ((BigDecimal) r[1]).setScale(2, RoundingMode.HALF_UP), (long) r[2]));
        }

        return map.values().stream()
            .sorted((a, b) -> b.montant().compareTo(a.montant()))
            .collect(Collectors.toList());
    }

    // ─── 3. Évolution Mensuelle (6 derniers mois) ─────────────────────────────

    public List<EvolutionMensuelle> getEvolutionMensuelle() {
        LocalDateTime since = LocalDateTime.now().minusMonths(6).withDayOfMonth(1);
        Map<String, EvolutionMensuelle> map = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<Object[]> rowsDAI = em.createQuery(
            "SELECT YEAR(d.dateCreation), MONTH(d.dateCreation), COALESCE(SUM(d.montantEstime), 0), COUNT(d) " +
            "FROM DemandeAchatInterne d WHERE d.dateCreation >= :since " +
            "GROUP BY YEAR(d.dateCreation), MONTH(d.dateCreation)"
        ).setParameter("since", since).getResultList();

        for(Object[] r : rowsDAI) {
            String key = r[0] + "-" + r[1];
            map.put(key, new EvolutionMensuelle(((Number) r[0]).intValue(), ((Number) r[1]).intValue(), ((BigDecimal) r[2]).setScale(2, RoundingMode.HALF_UP), (long) r[3]));
        }

        return map.values().stream()
            .sorted(Comparator.comparingInt(EvolutionMensuelle::annee).thenComparingInt(EvolutionMensuelle::mois))
            .collect(Collectors.toList());
    }

    // ─── 4. Dépenses par Département ─────────────────────────────────────────

    public List<DepenseParDepartement> getDepensesParDepartement() {
        Map<String, DepenseParDepartement> map = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<Object[]> rowsDAI = em.createQuery(
            "SELECT d.departement, COALESCE(SUM(d.montantEstime), 0), COUNT(d) " +
            "FROM DemandeAchatInterne d " +
            "WHERE d.montantEstime IS NOT NULL AND d.statut IN :statuts " +
            "GROUP BY d.departement"
        ).setParameter("statuts", List.of(StatutDemande.APPROUVEE, StatutDemande.PO_CREE, StatutDemande.AFFECTEE)).getResultList();

        for(Object[] r : rowsDAI) {
            if (r[0] == null) continue;
            map.put((String) r[0], new DepenseParDepartement((String) r[0], ((BigDecimal) r[1]).setScale(2, RoundingMode.HALF_UP), (long) r[2]));
        }

        return map.values().stream()
            .sorted((a, b) -> b.montant().compareTo(a.montant()))
            .collect(Collectors.toList());
    }

    // ─── 5. Consommation Budgétaire par Famille ───────────────────────────────

    public List<ConsommationBudget> getConsommationBudget() {
        return familyRepo.findAll().stream().map(f -> {
            BigDecimal ini  = orZero(f.getBudgetInitial());
            BigDecimal eng  = orZero(f.getBudgetEngage());
            BigDecimal rest = orZero(f.getBudgetRestant());
            double taux = ini.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : eng.multiply(BigDecimal.valueOf(100))
                     .divide(ini, 2, RoundingMode.HALF_UP)
                     .doubleValue();
            return new ConsommationBudget(f.getLibelle(), ini, eng, rest, taux);
        }).collect(Collectors.toList());
    }

    // ─── Helper privé ─────────────────────────────────────────────────────────

    private BigDecimal montantAggregeDAI(List<StatutDemande> statuts) {
        if (statuts.isEmpty()) return BigDecimal.ZERO;
        Object result = em.createQuery(
            "SELECT COALESCE(SUM(d.montantEstime), 0) FROM DemandeAchatInterne d " +
            "WHERE d.statut IN :statuts AND d.montantEstime IS NOT NULL"
        ).setParameter("statuts", statuts).getSingleResult();
        return result instanceof BigDecimal bd ? bd.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }


    private static BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}

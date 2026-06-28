package com.pfe.gestionsachat.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;

@Service
@Transactional(readOnly = true)
public class OverviewBiService {

    private final EntityManager entityManager;

    @Autowired
    public OverviewBiService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public BiOverviewDto getOverview() {
        BiOverviewDto dto = new BiOverviewDto();

        String statsSql = """
            SELECT
              COUNT(*) as total_demandes,
              COUNT(*) FILTER (WHERE statut IN ('SOUMISE','VALIDE_N1','VALIDE_TECH','VALIDE_DG','VALIDE_DAF')) as en_attente,
              COALESCE(COUNT(*) FILTER (WHERE statut = 'APPROUVEE') * 100.0 / NULLIF(COUNT(*) FILTER (WHERE statut IN ('APPROUVEE','REJETEE','PO_CREE','AFFECTEE')), 0), 0.0) as pct_approuvee,
              COALESCE(COUNT(*) FILTER (WHERE statut = 'REJETEE') * 100.0 / NULLIF(COUNT(*) FILTER (WHERE statut IN ('APPROUVEE','REJETEE','PO_CREE','AFFECTEE')), 0), 0.0) as pct_rejetee,
              COALESCE(COUNT(*) FILTER (WHERE statut = 'PO_CREE') * 100.0 / NULLIF(COUNT(*) FILTER (WHERE statut IN ('APPROUVEE','REJETEE','PO_CREE','AFFECTEE')), 0), 0.0) as pct_po_cree,
              COALESCE(COUNT(*) FILTER (WHERE statut = 'AFFECTEE') * 100.0 / NULLIF(COUNT(*) FILTER (WHERE statut IN ('APPROUVEE','REJETEE','PO_CREE','AFFECTEE')), 0), 0.0) as pct_affectee
            FROM demande_achat_interne
        """;

        Tuple stats = (Tuple) entityManager.createNativeQuery(statsSql, Tuple.class).getSingleResult();
        
        dto.setTotalDemandes(((Number) stats.get("total_demandes")).longValue());
        dto.setEnAttenteValidation(((Number) stats.get("en_attente")).longValue());
        dto.setPourcentageApprouvees(((Number) stats.get("pct_approuvee")).doubleValue());
        dto.setPourcentageRejetees(((Number) stats.get("pct_rejetee")).doubleValue());
        dto.setPourcentagePoCree(((Number) stats.get("pct_po_cree")).doubleValue());
        dto.setPourcentageAffectee(((Number) stats.get("pct_affectee")).doubleValue());

        String budgetsSql = """
            SELECT
              categorie,
              libelle,
              budget_initial,
              budget_restant,
              COALESCE((budget_initial - budget_restant) / NULLIF(budget_initial, 0) * 100.0, 0.0) as taux_consommation
            FROM family
        """;

        @SuppressWarnings("unchecked")
        List<Tuple> budgets = entityManager.createNativeQuery(budgetsSql, Tuple.class).getResultList();
        
        List<BudgetFamilleDto> budgetList = new ArrayList<>();
        for (Tuple b : budgets) {
            BudgetFamilleDto bDto = new BudgetFamilleDto();
            bDto.setCategorie(b.get("categorie", String.class));
            bDto.setLibelle(b.get("libelle", String.class));
            
            // On gère les nulls possibles de la base pour les décimales si nécessaire, bien que budget_initial ne devrait pas être null
            bDto.setBudgetInitial(b.get("budget_initial") != null ? ((Number) b.get("budget_initial")).doubleValue() : 0.0);
            bDto.setBudgetRestant(b.get("budget_restant") != null ? ((Number) b.get("budget_restant")).doubleValue() : 0.0);
            bDto.setTauxConsommation(b.get("taux_consommation") != null ? ((Number) b.get("taux_consommation")).doubleValue() : 0.0);
            budgetList.add(bDto);
        }
        
        dto.setBudgetsParFamille(budgetList);

        return dto;
    }

    public static class BiOverviewDto {
        private Long totalDemandes;
        private Double pourcentageApprouvees;
        private Double pourcentageRejetees;
        private Double pourcentagePoCree;
        private Double pourcentageAffectee;
        private Long enAttenteValidation;
        private List<BudgetFamilleDto> budgetsParFamille;

        public Long getTotalDemandes() { return totalDemandes; }
        public void setTotalDemandes(Long totalDemandes) { this.totalDemandes = totalDemandes; }
        public Double getPourcentageApprouvees() { return pourcentageApprouvees; }
        public void setPourcentageApprouvees(Double pourcentageApprouvees) { this.pourcentageApprouvees = pourcentageApprouvees; }
        public Double getPourcentageRejetees() { return pourcentageRejetees; }
        public void setPourcentageRejetees(Double pourcentageRejetees) { this.pourcentageRejetees = pourcentageRejetees; }
        public Double getPourcentagePoCree() { return pourcentagePoCree; }
        public void setPourcentagePoCree(Double pourcentagePoCree) { this.pourcentagePoCree = pourcentagePoCree; }
        public Double getPourcentageAffectee() { return pourcentageAffectee; }
        public void setPourcentageAffectee(Double pourcentageAffectee) { this.pourcentageAffectee = pourcentageAffectee; }
        public Long getEnAttenteValidation() { return enAttenteValidation; }
        public void setEnAttenteValidation(Long enAttenteValidation) { this.enAttenteValidation = enAttenteValidation; }
        public List<BudgetFamilleDto> getBudgetsParFamille() { return budgetsParFamille; }
        public void setBudgetsParFamille(List<BudgetFamilleDto> budgetsParFamille) { this.budgetsParFamille = budgetsParFamille; }
    }

    public static class BudgetFamilleDto {
        private String categorie;
        private String libelle;
        private Double budgetInitial;
        private Double budgetRestant;
        private Double tauxConsommation;

        public String getCategorie() { return categorie; }
        public void setCategorie(String categorie) { this.categorie = categorie; }
        public String getLibelle() { return libelle; }
        public void setLibelle(String libelle) { this.libelle = libelle; }
        public Double getBudgetInitial() { return budgetInitial; }
        public void setBudgetInitial(Double budgetInitial) { this.budgetInitial = budgetInitial; }
        public Double getBudgetRestant() { return budgetRestant; }
        public void setBudgetRestant(Double budgetRestant) { this.budgetRestant = budgetRestant; }
        public Double getTauxConsommation() { return tauxConsommation; }
        public void setTauxConsommation(Double tauxConsommation) { this.tauxConsommation = tauxConsommation; }
    }
}

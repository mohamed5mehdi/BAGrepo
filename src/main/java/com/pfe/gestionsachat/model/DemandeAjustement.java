package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "demande_ajustement")
public class DemandeAjustement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "da_id", nullable = false)
    private DaHeader da;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeAjustement type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAjustement statut;

    @Column(name = "source_sous_famille_id")
    private Integer sourceSousFamilleId;

    @Column(name = "cible_sous_famille_id")
    private Integer cibleSousFamilleId;

    @Column(name = "famille_cible_id")
    private Integer familleCibleId;

    @Column(name = "montant_demande", nullable = false)
    private BigDecimal montantDemande;

    @Column(name = "montant_final")
    private BigDecimal montantFinal;

    @Column(name = "justification_acheteur", nullable = false, length = 1000)
    private String justificationAcheteur;

    @Column(name = "justification_valideur", length = 1000)
    private String justificationValideur;

    @Column(name = "budget_avant_demande", length = 500)
    private String budgetAvantDemande;

    @Column(name = "budget_apres_demande", length = 500)
    private String budgetApresDemande;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_da_avant_ajustement")
    private StatutDA statutDaAvantAjustement;

    @Column(name = "acheteur_id")
    private Long acheteurId;

    @Column(name = "valideur_id")
    private Long valideurId;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    public DemandeAjustement() {}

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DaHeader getDa() { return da; }
    public void setDa(DaHeader da) { this.da = da; }

    public TypeAjustement getType() { return type; }
    public void setType(TypeAjustement type) { this.type = type; }

    public StatutAjustement getStatut() { return statut; }
    public void setStatut(StatutAjustement statut) { this.statut = statut; }

    public Integer getSourceSousFamilleId() { return sourceSousFamilleId; }
    public void setSourceSousFamilleId(Integer sourceSousFamilleId) { this.sourceSousFamilleId = sourceSousFamilleId; }

    public Integer getCibleSousFamilleId() { return cibleSousFamilleId; }
    public void setCibleSousFamilleId(Integer cibleSousFamilleId) { this.cibleSousFamilleId = cibleSousFamilleId; }

    public Integer getFamilleCibleId() { return familleCibleId; }
    public void setFamilleCibleId(Integer familleCibleId) { this.familleCibleId = familleCibleId; }

    public BigDecimal getMontantDemande() { return montantDemande; }
    public void setMontantDemande(BigDecimal montantDemande) { this.montantDemande = montantDemande; }

    public BigDecimal getMontantFinal() { return montantFinal; }
    public void setMontantFinal(BigDecimal montantFinal) { this.montantFinal = montantFinal; }

    public String getJustificationAcheteur() { return justificationAcheteur; }
    public void setJustificationAcheteur(String justificationAcheteur) { this.justificationAcheteur = justificationAcheteur; }

    public String getJustificationValideur() { return justificationValideur; }
    public void setJustificationValideur(String justificationValideur) { this.justificationValideur = justificationValideur; }

    public String getBudgetAvantDemande() { return budgetAvantDemande; }
    public void setBudgetAvantDemande(String budgetAvantDemande) { this.budgetAvantDemande = budgetAvantDemande; }

    public String getBudgetApresDemande() { return budgetApresDemande; }
    public void setBudgetApresDemande(String budgetApresDemande) { this.budgetApresDemande = budgetApresDemande; }

    public StatutDA getStatutDaAvantAjustement() { return statutDaAvantAjustement; }
    public void setStatutDaAvantAjustement(StatutDA statutDaAvantAjustement) { this.statutDaAvantAjustement = statutDaAvantAjustement; }

    public Long getAcheteurId() { return acheteurId; }
    public void setAcheteurId(Long acheteurId) { this.acheteurId = acheteurId; }

    public Long getValideurId() { return valideurId; }
    public void setValideurId(Long valideurId) { this.valideurId = valideurId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateDecision() { return dateDecision; }
    public void setDateDecision(LocalDateTime dateDecision) { this.dateDecision = dateDecision; }
}

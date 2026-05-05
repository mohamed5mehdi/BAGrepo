package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "demande_achat_interne")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DemandeAchatInterne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "demandeur_id")
    private User demandeur;

    private String departement;

    @Enumerated(EnumType.STRING)
    private CategorieDemande categorie;

    private String designation;

    private Integer quantite;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Enumerated(EnumType.STRING)
    private UrgenceDemande urgence;

    @ManyToOne
    @JoinColumn(name = "budget_famille_id")
    private Family budgetFamille;

    @ManyToOne
    @JoinColumn(name = "budget_sous_famille_id")
    private SubFamily budgetSousFamille;

    @Enumerated(EnumType.STRING)
    private StatutDemande statut;

    @Enumerated(EnumType.STRING)
    private TypeAjustement typeAjustement;

    private BigDecimal montantEstime;

    private BigDecimal prixUnitaire;

    @ManyToOne
    @JoinColumn(name = "fournisseur_id")
    private Supplier fournisseur;

    private LocalDateTime dateCreation;

    private LocalDateTime dateValidation;

    @Column(columnDefinition = "TEXT")
    private String commentaireRejet;

    @Column(unique = true)
    private String submissionToken;

    @OneToMany(mappedBy = "demandeAchatInterne", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<DaDetails> details = new java.util.ArrayList<>();

    public DemandeAchatInterne() {
        this.dateCreation = LocalDateTime.now();
        this.statut = StatutDemande.BROUILLON;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getDemandeur() { return demandeur; }
    public void setDemandeur(User demandeur) { this.demandeur = demandeur; }

    public String getDepartement() { return departement; }
    public void setDepartement(String departement) { this.departement = departement; }

    public CategorieDemande getCategorie() { return categorie; }
    public void setCategorie(CategorieDemande categorie) { this.categorie = categorie; }

    @PrePersist
    @PreUpdate
    private void syncCategorie() {
        if (this.budgetFamille != null && this.budgetFamille.getCategorie() != null) {
            this.categorie = this.budgetFamille.getCategorie();
        }
    }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public UrgenceDemande getUrgence() { return urgence; }
    public void setUrgence(UrgenceDemande urgence) { this.urgence = urgence; }

    public Family getBudgetFamille() { return budgetFamille; }
    public void setBudgetFamille(Family budgetFamille) { 
        this.budgetFamille = budgetFamille;
        if (budgetFamille != null && budgetFamille.getCategorie() != null) {
            this.categorie = budgetFamille.getCategorie();
        }
    }

    public SubFamily getBudgetSousFamille() { return budgetSousFamille; }
    public void setBudgetSousFamille(SubFamily budgetSousFamille) { this.budgetSousFamille = budgetSousFamille; }

    public StatutDemande getStatut() { return statut; }
    public void setStatut(StatutDemande statut) { this.statut = statut; }

    public TypeAjustement getTypeAjustement() { return typeAjustement; }
    public void setTypeAjustement(TypeAjustement typeAjustement) { this.typeAjustement = typeAjustement; }

    public BigDecimal getMontantEstime() { return montantEstime; }
    public void setMontantEstime(BigDecimal montantEstime) { this.montantEstime = montantEstime; }

    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public Supplier getFournisseur() { return fournisseur; }
    public void setFournisseur(Supplier fournisseur) { this.fournisseur = fournisseur; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateValidation() { return dateValidation; }
    public void setDateValidation(LocalDateTime dateValidation) { this.dateValidation = dateValidation; }

    public String getCommentaireRejet() { return commentaireRejet; }
    public void setCommentaireRejet(String commentaireRejet) { this.commentaireRejet = commentaireRejet; }

    public java.util.List<DaDetails> getDetails() { return details; }
    public void setDetails(java.util.List<DaDetails> details) { this.details = details; }

    public String getSubmissionToken() { return submissionToken; }
    public void setSubmissionToken(String submissionToken) { this.submissionToken = submissionToken; }
}

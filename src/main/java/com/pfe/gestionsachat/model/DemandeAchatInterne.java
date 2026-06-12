package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "demande_achat_interne")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class DemandeAchatInterne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_famille_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(
        {"hibernateLazyInitializer", "handler", "subFamilies"})
    private Family budgetFamille;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_sous_famille_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(
        {"hibernateLazyInitializer", "handler", "family", "details",
         "transfersSource", "transfersCible"})
    private SubFamily budgetSousFamille;

    @Enumerated(EnumType.STRING)
    private StatutDemande statut;

    @Enumerated(EnumType.STRING)
    private TypeAjustement typeAjustement;

    private BigDecimal montantEstime;

    private BigDecimal prixUnitaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id")
    private Supplier fournisseur;

    private LocalDateTime dateCreation;

    private LocalDateTime dateValidation;

    @Column(columnDefinition = "TEXT")
    private String commentaireRejet;

    @Column(unique = true)
    private String submissionToken;

    private Boolean isPieceRechange = false;
    private String itemCode;
    private Boolean isAvailableInStock = false;



    /**
     * Lien inverse vers le PO généré depuis cette DA.
     * Permet la traçabilité complète DA→PO→GRN→GRC pour le reporting.
     */
    @OneToOne(mappedBy = "demandeInterne")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private PurchaseOrder purchaseOrder;

    /**
     * Expose l'id_po du PO associé pour le téléchargement PDF côté frontend.
     * @JsonIgnore sur purchaseOrder évite la boucle infinie DA→PO→DA.
     * Ce champ calculé permet au Demandeur de télécharger son BC sans navigation supplémentaire.
     */
    @com.fasterxml.jackson.annotation.JsonGetter("id_po")
    public Integer getIdPoAssociated() {
        return purchaseOrder != null ? purchaseOrder.getIdPo() : null;
    }

    public DemandeAchatInterne() {
        this.dateCreation = LocalDateTime.now();
        this.statut = StatutDemande.BROUILLON;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getDemandeur() {
        return demandeur;
    }

    public void setDemandeur(User demandeur) {
        this.demandeur = demandeur;
    }

    public String getDepartement() {
        return departement;
    }

    public void setDepartement(String departement) {
        this.departement = departement;
    }

    public CategorieDemande getCategorie() {
        return categorie;
    }

    public void setCategorie(CategorieDemande categorie) {
        this.categorie = categorie;
    }

    /**
     * Synchronisation de la catégorie depuis la famille budgétaire.
     * RÈGLE : ce @PrePersist/@PreUpdate est l'UNIQUE point de sync — source de vérité exclusive.
     * Le setter setBudgetFamille() ne touche PAS à categorie pour éviter un double override silencieux.
     * Si budgetFamille est null (DA interne sans famille), categorie reste à sa valeur courante.
     */
    @PrePersist
    @PreUpdate
    private void prePersistUpdate() {
        if (this.categorie == null && this.budgetFamille != null && this.budgetFamille.getCategorie() != null) {
            this.categorie = this.budgetFamille.getCategorie();
        }
        
        /**
         * BUG-FORENSIQUE-03 : Validation de positivité.
         * Une DA avec quantité ou montants négatifs corrompt tout le circuit aval (PO, GRC).
         */
        if (quantite != null && quantite <= 0) {
            throw new IllegalStateException("DemandeAchatInterne : quantite (" + quantite + ") doit être strictement positive.");
        }
        if (prixUnitaire != null && prixUnitaire.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("DemandeAchatInterne : prixUnitaire (" + prixUnitaire + ") ne peut pas être négatif.");
        }
        if (montantEstime != null && montantEstime.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("DemandeAchatInterne : montantEstime (" + montantEstime + ") ne peut pas être négatif.");
        }
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public Integer getQuantite() {
        return quantite;
    }

    public void setQuantite(Integer quantite) {
        this.quantite = quantite;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public UrgenceDemande getUrgence() {
        return urgence;
    }

    public void setUrgence(UrgenceDemande urgence) {
        this.urgence = urgence;
    }

    public Family getBudgetFamille() {
        return budgetFamille;
    }

    public void setBudgetFamille(Family budgetFamille) {
        // BUG-01 FIX : ne pas toucher à this.categorie ici.
        // La sync famille→categorie est gérée exclusivement par syncCategorie() (@PrePersist/@PreUpdate).
        // Supprimer la sync dans ce setter évite l'override silencieux d'un setCategorie() explicite.
        this.budgetFamille = budgetFamille;
    }

    public SubFamily getBudgetSousFamille() {
        return budgetSousFamille;
    }

    public void setBudgetSousFamille(SubFamily budgetSousFamille) {
        this.budgetSousFamille = budgetSousFamille;
    }

    public StatutDemande getStatut() {
        return statut;
    }

    public void setStatut(StatutDemande statut) {
        this.statut = statut;
    }

    public TypeAjustement getTypeAjustement() {
        return typeAjustement;
    }

    public void setTypeAjustement(TypeAjustement typeAjustement) {
        this.typeAjustement = typeAjustement;
    }

    public BigDecimal getMontantEstime() {
        return montantEstime;
    }

    public void setMontantEstime(BigDecimal montantEstime) {
        this.montantEstime = montantEstime;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public Supplier getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(Supplier fournisseur) {
        this.fournisseur = fournisseur;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateValidation() {
        return dateValidation;
    }

    public void setDateValidation(LocalDateTime dateValidation) {
        this.dateValidation = dateValidation;
    }

    public String getCommentaireRejet() {
        return commentaireRejet;
    }

    public void setCommentaireRejet(String commentaireRejet) {
        this.commentaireRejet = commentaireRejet;
    }



    public String getSubmissionToken() {
        return submissionToken;
    }

    public void setSubmissionToken(String submissionToken) {
        this.submissionToken = submissionToken;
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    public Boolean getIsPieceRechange() {
        return isPieceRechange;
    }

    public void setIsPieceRechange(Boolean isPieceRechange) {
        this.isPieceRechange = isPieceRechange;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public Boolean getIsAvailableInStock() {
        return isAvailableInStock;
    }

    public void setIsAvailableInStock(Boolean isAvailableInStock) {
        this.isAvailableInStock = isAvailableInStock;
    }
}

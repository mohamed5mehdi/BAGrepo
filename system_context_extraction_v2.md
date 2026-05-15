## Action.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "action")
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oid_action")
    private Integer oidAction;

    @ManyToOne
    @JoinColumn(name = "oid_user")
    private User user;

    @ManyToOne
    @JoinColumn(name = "id_da")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private DaHeader daHeader;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_action")
    private TypeAction typeAction;

    @Column(name = "date_action")
    private LocalDate dateAction;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    public Action() {
        this.dateAction = LocalDate.now();
    }

    public Action(User user, DaHeader daHeader, TypeAction typeAction) {
        this();
        this.user = user;
        this.daHeader = daHeader;
        this.typeAction = typeAction;
    }

    // Getters
    public Integer getOidAction() { return oidAction; }
    public User getUser() { return user; }
    public DaHeader getDaHeader() { return daHeader; }
    public TypeAction getTypeAction() { return typeAction; }
    public LocalDate getDateAction() { return dateAction; }
    public String getMetadata() { return metadata; }

    // Setters
    public void setOidAction(Integer oidAction) { this.oidAction = oidAction; }
    public void setUser(User user) { this.user = user; }
    public void setDaHeader(DaHeader daHeader) { this.daHeader = daHeader; }
    public void setTypeAction(TypeAction typeAction) { this.typeAction = typeAction; }
    public void setDateAction(LocalDate dateAction) { this.dateAction = dateAction; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}

`

## AuditLog.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;

    private String entite;

    private Long entiteId;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id")
    private User utilisateur;

    private LocalDateTime dateAction;

    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String valeurAvant;

    @Column(columnDefinition = "TEXT")
    private String valeurApres;

    public AuditLog() {
        this.dateAction = LocalDateTime.now();
    }

    public AuditLog(String action, String entite, Long entiteId, User utilisateur, String valeurAvant, String valeurApres) {
        this();
        this.action = action;
        this.entite = entite;
        this.entiteId = entiteId;
        this.utilisateur = utilisateur;
        this.valeurAvant = valeurAvant;
        this.valeurApres = valeurApres;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntite() { return entite; }
    public void setEntite(String entite) { this.entite = entite; }

    public Long getEntiteId() { return entiteId; }
    public void setEntiteId(Long entiteId) { this.entiteId = entiteId; }

    public User getUtilisateur() { return utilisateur; }
    public void setUtilisateur(User utilisateur) { this.utilisateur = utilisateur; }

    public LocalDateTime getDateAction() { return dateAction; }
    public void setDateAction(LocalDateTime dateAction) { this.dateAction = dateAction; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getValeurAvant() { return valeurAvant; }
    public void setValeurAvant(String valeurAvant) { this.valeurAvant = valeurAvant; }

    public String getValeurApres() { return valeurApres; }
    public void setValeurApres(String valeurApres) { this.valeurApres = valeurApres; }
}

`

## BudgetTransfer.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "budget_transfer")
public class BudgetTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transfert")
    private Integer idTransfert;

    @ManyToOne
    @JoinColumn(name = "id_da")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private DaHeader daHeader;

    @ManyToOne
    @JoinColumn(name = "id_sub_source")
    private SubFamily subSource;

    @ManyToOne
    @JoinColumn(name = "id_sub_cible")
    private SubFamily subCible;

    private BigDecimal montant;

    @Column(name = "date_transfert")
    private LocalDate dateTransfert;

    @ManyToOne
    @JoinColumn(name = "id_daf")
    private User daf;

    public BudgetTransfer() {
        this.dateTransfert = LocalDate.now();
    }

    public BudgetTransfer(DaHeader daHeader, SubFamily subSource, SubFamily subCible,
                          BigDecimal montant, User daf) {
        this();
        this.daHeader = daHeader;
        this.subSource = subSource;
        this.subCible = subCible;
        this.montant = montant;
        this.daf = daf;
    }

    // Getters
    public Integer getIdTransfert() { return idTransfert; }
    public DaHeader getDaHeader() { return daHeader; }
    public SubFamily getSubSource() { return subSource; }
    public SubFamily getSubCible() { return subCible; }
    public BigDecimal getMontant() { return montant; }
    public LocalDate getDateTransfert() { return dateTransfert; }
    public User getDaf() { return daf; }

    // Setters
    public void setIdTransfert(Integer idTransfert) { this.idTransfert = idTransfert; }
    public void setDaHeader(DaHeader daHeader) { this.daHeader = daHeader; }
    public void setSubSource(SubFamily subSource) { this.subSource = subSource; }
    public void setSubCible(SubFamily subCible) { this.subCible = subCible; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public void setDateTransfert(LocalDate dateTransfert) { this.dateTransfert = dateTransfert; }
    public void setDaf(User daf) { this.daf = daf; }
}

`

## CategorieDemande.java
`java
package com.pfe.gestionsachat.model;

public enum CategorieDemande {
    INFORMATIQUE,
    BUREAUTIQUE,
    MOBILIER,
    CONSOMMABLE,
    AUTRE
}

`

## CreditNote.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "credit_note")
public class CreditNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "grn_header_id")
    private GrnHeader grnHeader;

    private String creditNoteNumber;
    private java.util.Date creditNoteDate;
    private java.math.BigDecimal montant;

    @Enumerated(EnumType.STRING)
    private CreditNoteStatus status;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public GrnHeader getGrnHeader() { return grnHeader; }
    public void setGrnHeader(GrnHeader grnHeader) { this.grnHeader = grnHeader; }
    public String getCreditNoteNumber() { return creditNoteNumber; }
    public void setCreditNoteNumber(String creditNoteNumber) { this.creditNoteNumber = creditNoteNumber; }
    public Date getCreditNoteDate() { return creditNoteDate; }
    public void setCreditNoteDate(Date creditNoteDate) { this.creditNoteDate = creditNoteDate; }
    public java.math.BigDecimal getMontant() { return montant; }
    public void setMontant(java.math.BigDecimal montant) { this.montant = montant; }
    public CreditNoteStatus getStatus() { return status; }
    public void setStatus(CreditNoteStatus status) { this.status = status; }
}

`

## CreditNoteStatus.java
`java
package com.pfe.gestionsachat.model;

public enum CreditNoteStatus {
    PENDING,
    ISSUED,
    RECONCILED,
    COMPLETED
}

`

## DaDetails.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "da_details")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class DaDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oid_detail")
    @com.fasterxml.jackson.annotation.JsonProperty("oid_detail")
    private Integer oidDetail;

    @com.fasterxml.jackson.annotation.JsonProperty("id")
    public Integer getId() { return oidDetail; }

    @ManyToOne
    @JoinColumn(name = "id_da")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private DaHeader daHeader;

    @ManyToOne
    @JoinColumn(name = "id_demande_interne")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private DemandeAchatInterne demandeAchatInterne;

    @ManyToOne
    @JoinColumn(name = "id_sous_famille")
    private SubFamily subFamily;

    private Integer quantite;
    private String itemCode;
    private String itemName;
    private String description;
    private String justification;

    @Column(name = "prix_unitaire")
    @com.fasterxml.jackson.annotation.JsonProperty("prix_unitaire")
    private BigDecimal prixUnitaire;

    @ManyToOne
    @JoinColumn(name = "id_fournisseur", nullable = true)
    private Supplier fournisseur;

    public DaDetails() {}

    public DaDetails(DaHeader daHeader, SubFamily subFamily, Integer quantite,
                     String description, BigDecimal prixUnitaire) {
        this.daHeader = daHeader;
        this.subFamily = subFamily;
        this.quantite = quantite;
        this.description = description;
        this.prixUnitaire = prixUnitaire;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("totalPrice")
    public BigDecimal getTotalPrice() {
        if (prixUnitaire == null || quantite == null) return BigDecimal.ZERO;
        return prixUnitaire.multiply(BigDecimal.valueOf(quantite));
    }

    // Getters
    public Integer getOidDetail() { return oidDetail; }
    public DaHeader getDaHeader() { return daHeader; }
    public SubFamily getSubFamily() { return subFamily; }
    public Integer getQuantite() { return quantite; }
    public String getDescription() { return description; }
    public String getItemCode() { return itemCode; }
    public String getItemName() { return itemName; }
    public String getJustification() { return justification; }
    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public Supplier getFournisseur() { return fournisseur; }

    // Setters
    public void setOidDetail(Integer oidDetail) { this.oidDetail = oidDetail; }
    public void setDaHeader(DaHeader daHeader) { this.daHeader = daHeader; }
    public void setSubFamily(SubFamily subFamily) { this.subFamily = subFamily; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setDescription(String description) { this.description = description; }
    public void setJustification(String justification) { this.justification = justification; }
    public void setPrixUnitaire(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }
    public void setFournisseur(Supplier fournisseur) { this.fournisseur = fournisseur; }
    public DemandeAchatInterne getDemandeAchatInterne() { return demandeAchatInterne; }
    public void setDemandeAchatInterne(DemandeAchatInterne demandeAchatInterne) { this.demandeAchatInterne = demandeAchatInterne; }
}
`

## DaHeader.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "da_header")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class DaHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oid_da")
    @com.fasterxml.jackson.annotation.JsonProperty("oid_da")
    private Integer oidDa;

    @com.fasterxml.jackson.annotation.JsonProperty("id")
    public Integer getId() { return oidDa; }

    @ManyToOne
    @JoinColumn(name = "id_demandeur")
    private User demandeur;

    @Column(name = "date_creation")
    private LocalDate dateCreation;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    @com.fasterxml.jackson.annotation.JsonProperty("statut")
    private StatutDA statut;

    private String objet;
    
    private String urgencyLevel;
    
    @Column(columnDefinition = "TEXT")
    private String justification;

    @OneToMany(mappedBy = "daHeader", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DaDetails> details = new ArrayList<>();

    @OneToMany(mappedBy = "daHeader")
    private List<Action> actions = new ArrayList<>();

    @OneToMany(mappedBy = "daHeader")
    private List<Justification> justifications = new ArrayList<>();

    @OneToOne(mappedBy = "daHeader", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private PurchaseOrder purchaseOrder;

    @OneToMany(mappedBy = "daHeader")
    private List<BudgetTransfer> budgetTransfers = new ArrayList<>();

    public DaHeader() {
        this.dateCreation = LocalDate.now();
        this.statut = StatutDA.EN_ATTENTE_N1; // Set default as per first state
    }

    public DaHeader(String objet, User demandeur) {
        this();
        this.objet = objet;
        this.demandeur = demandeur;
    }

    public void soumettre() {
        if (this.statut != StatutDA.EN_ATTENTE_N1) {
            throw new IllegalStateException(
                    "Impossible de soumettre la DA : statut actuel [" + this.statut + "] — seul EN_ATTENTE_N1 est autorisé.");
        }
        // Le statut reste EN_ATTENTE_N1 car la soumission déclenche le circuit de validation N1
        // Aucun changement d'état ici — c'est l'approbation N1 qui fait avancer le flux
    }

    // Getters
    public Integer getOidDa() { return oidDa; }
    public User getDemandeur() { return demandeur; }
    public LocalDate getDateCreation() { return dateCreation; }
    public StatutDA getStatut() { return statut; }
    public String getObjet() { return objet; }
    public List<DaDetails> getDetails() { return details; }
    public List<Action> getActions() { return actions; }
    public List<Justification> getJustifications() { return justifications; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public String getUrgencyLevel() { return urgencyLevel; }
    public String getJustification() { return justification; }

    // Setters
    public void setOidDa(Integer oidDa) { this.oidDa = oidDa; }
    public void setDemandeur(User demandeur) { this.demandeur = demandeur; }
    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }
    public void setStatut(StatutDA statut) { this.statut = statut; }
    public void setObjet(String objet) { this.objet = objet; }
    public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }
    public void setJustification(String justification) { this.justification = justification; }
    public void setDetails(List<DaDetails> details) { this.details = details; }
    public void setActions(List<Action> actions) { this.actions = actions; }
    public void setJustifications(List<Justification> justifications) { this.justifications = justifications; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    public void setBudgetTransfers(List<BudgetTransfer> budgetTransfers) { this.budgetTransfers = budgetTransfers; }
}
`

## DemandeAchatInterne.java
`java
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

    private Boolean isPieceRechange = false;
    private String itemCode;
    private Boolean isAvailableInStock = false;

    @OneToMany(mappedBy = "demandeAchatInterne", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<DaDetails> details = new java.util.ArrayList<>();

    /**
     * Lien inverse vers le PO généré depuis cette DA.
     * Permet la traçabilité complète DA→PO→GRN→GRC pour le reporting.
     */
    @OneToOne(mappedBy = "demandeInterne")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private PurchaseOrder purchaseOrder;

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

    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }

    public Boolean getIsPieceRechange() { return isPieceRechange; }
    public void setIsPieceRechange(Boolean isPieceRechange) { this.isPieceRechange = isPieceRechange; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public Boolean getIsAvailableInStock() { return isAvailableInStock; }
    public void setIsAvailableInStock(Boolean isAvailableInStock) { this.isAvailableInStock = isAvailableInStock; }
}

`

## DemandeAjustement.java
`java
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

`

## Family.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

@Entity
@Table(name = "family")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "ignoreUnknown"})
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @com.fasterxml.jackson.annotation.JsonProperty("id")
    @com.fasterxml.jackson.annotation.JsonAlias({"id_family", "familyId", "oid_family"})
    private Integer idFamily;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "categorie")
    private CategorieDemande categorie;

    @Version
    private Long version;

    @com.fasterxml.jackson.annotation.JsonProperty("name")
    @com.fasterxml.jackson.annotation.JsonAlias({"libelle", "label"})
    private String libelle;

    @Column(name = "budget_initial")
    @com.fasterxml.jackson.annotation.JsonProperty("budget_initial")
    private BigDecimal budgetInitial;

    @Column(name = "budget_restant")
    @com.fasterxml.jackson.annotation.JsonProperty("budget_restant")
    private BigDecimal budgetRestant;

    @Column(name = "budget_engage")
    @com.fasterxml.jackson.annotation.JsonProperty("budget_engage")
    private BigDecimal budgetEngage = BigDecimal.ZERO;

    @OneToMany(mappedBy = "family", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<SubFamily> subFamilies = new ArrayList<>();

    public Family() {}

    public Family(String libelle, BigDecimal budgetInitial) {
        this.libelle = libelle;
        this.budgetInitial = budgetInitial;
        this.budgetRestant = budgetInitial;
    }

    public void deductBudget(BigDecimal amount) {
        if (this.budgetRestant != null) {
            this.budgetRestant = this.budgetRestant.subtract(amount);
        }
        this.budgetEngage = (this.budgetEngage != null ? this.budgetEngage : BigDecimal.ZERO).add(amount);
    }

    public void addBudget(BigDecimal amount) {
        if (this.budgetRestant != null) {
            this.budgetRestant = this.budgetRestant.add(amount);
        }
    }

    // Getters
    public Integer getIdFamily() { return idFamily; }
    public String getLibelle() { return libelle; }
    public BigDecimal getBudgetInitial() { return budgetInitial; }
    public BigDecimal getBudgetRestant() { return budgetRestant; }
    public BigDecimal getBudgetEngage() { return budgetEngage; }

    @com.fasterxml.jackson.annotation.JsonProperty("budget_disponible")
    @Transient
    public BigDecimal getBudgetDisponible() {
        return budgetRestant != null ? budgetRestant : BigDecimal.ZERO;
    }
    public List<SubFamily> getSubFamilies() { return subFamilies; }

    // Setters
    public void setIdFamily(Integer idFamily) { this.idFamily = idFamily; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
    public void setBudgetInitial(BigDecimal budgetInitial) { this.budgetInitial = budgetInitial; }
    public void setBudgetRestant(BigDecimal budgetRestant) { this.budgetRestant = budgetRestant; }
    public void setBudgetEngage(BigDecimal budgetEngage) { this.budgetEngage = budgetEngage; }

    public CategorieDemande getCategorie() { return categorie; }
    public void setCategorie(CategorieDemande categorie) { this.categorie = categorie; }

    public void setSubFamilies(List<SubFamily> subFamilies) { this.subFamilies = subFamilies; }
}
`

## GrcDetails.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "grc_details")
public class GrcDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "grc_header_id")
    @JsonIgnore
    private GrcHeader grcHeader;

    @OneToOne
    @JoinColumn(name = "grn_detail_id")
    private GrnDetails grnDetail;

    private String itemCode;
    private Integer acceptedQuantity;
    private Double unitCost;
    private Double totalCost;
    private Double taxRate;
    private Double montantTTC;

    private String mainAccount;
    private String subAccount;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public GrcHeader getGrcHeader() { return grcHeader; }
    public void setGrcHeader(GrcHeader grcHeader) { this.grcHeader = grcHeader; }
    public GrnDetails getGrnDetail() { return grnDetail; }
    public void setGrnDetail(GrnDetails grnDetail) { this.grnDetail = grnDetail; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public Integer getAcceptedQuantity() { return acceptedQuantity; }
    public void setAcceptedQuantity(Integer acceptedQuantity) { this.acceptedQuantity = acceptedQuantity; }
    public Double getUnitCost() { return unitCost; }
    public void setUnitCost(Double unitCost) { this.unitCost = unitCost; }
    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }
    public Double getTaxRate() { return taxRate; }
    public void setTaxRate(Double taxRate) { this.taxRate = taxRate; }
    public Double getMontantTTC() { return montantTTC; }
    public void setMontantTTC(Double montantTTC) { this.montantTTC = montantTTC; }

    public String getMainAccount() { return mainAccount; }
    public void setMainAccount(String mainAccount) { this.mainAccount = mainAccount; }
    public String getSubAccount() { return subAccount; }
    public void setSubAccount(String subAccount) { this.subAccount = subAccount; }
}

`

## GrcHeader.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "grc_header")
public class GrcHeader {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "grn_header_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private GrnHeader grnHeader;

    private LocalDate costingDate = LocalDate.now();

    @ManyToOne
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Enumerated(EnumType.STRING)
    private GrcStatus status;

    private java.math.BigDecimal totalAmount;
    private String devise;

    /**
     * Règle BAG ERP : grcNumber = grnNumber du GRN associé.
     * Assigné dans GrcService.createGrc() depuis grnHeader.grnNumber.
     * Unique — garantit l'absence de doublon GRC.
     */
    @Column(name = "grc_number", unique = true)
    private String grcNumber;

    @OneToMany(mappedBy = "grcHeader", cascade = CascadeType.ALL)
    private List<GrcDetails> details;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public GrnHeader getGrnHeader() { return grnHeader; }
    public void setGrnHeader(GrnHeader grnHeader) { this.grnHeader = grnHeader; }
    public LocalDate getCostingDate() { return costingDate; }
    public void setCostingDate(LocalDate costingDate) { this.costingDate = costingDate; }
    public User getProcessedBy() { return processedBy; }
    public void setProcessedBy(User processedBy) { this.processedBy = processedBy; }
    public GrcStatus getStatus() { return status; }
    public void setStatus(GrcStatus status) { this.status = status; }
    public java.math.BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(java.math.BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getDevise() { return devise; }
    public void setDevise(String devise) { this.devise = devise; }
    public List<GrcDetails> getDetails() { return details; }
    public void setDetails(List<GrcDetails> details) { this.details = details; }
    public String getGrcNumber() { return grcNumber; }
    public void setGrcNumber(String grcNumber) { this.grcNumber = grcNumber; }
}

`

## GrcStatus.java
`java
package com.pfe.gestionsachat.model;

/**
 * Statuts du GRC — alignés sur le flux BAG ERP.
 * PENDING_APPROVAL : GRC créé, en attente de validation financière (Comptable).
 * POSTED           : GRC validé — rapprochement PO/GRN/GRC complété, facture générée.
 * APPROVED         : Second visa (Responsable Achat) — circuit complet clôturé.
 */
public enum GrcStatus {
    DRAFT,
    PENDING_APPROVAL,
    POSTED,
    APPROVED
}


`

## GrnDetails.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "grn_details")
public class GrnDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "grn_header_id")
    @JsonIgnore
    private GrnHeader grnHeader;

    private String itemCode;
    private String itemName;
    private Integer orderedQuantity;
    /**
     * Shipped Qty — solde restant à recevoir (règle BAG ERP).
     * = orderedQuantity - somme(receivedQuantity de tous les GRNs précédents) - receivedQuantity de CE GRN.
     * Calculé et persisté à la création du GRN. PO clôturé quand shippedQuantity = 0 pour toutes les lignes.
     */
    private Integer shippedQuantity;
    private Integer receivedQuantity;
    private Integer acceptedQuantity;
    private Integer rejectedQuantity;

    @Enumerated(EnumType.STRING)
    private QualityStatus qualityStatus;

    private String notes;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public GrnHeader getGrnHeader() { return grnHeader; }
    public void setGrnHeader(GrnHeader grnHeader) { this.grnHeader = grnHeader; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public Integer getOrderedQuantity() { return orderedQuantity; }
    public void setOrderedQuantity(Integer orderedQuantity) { this.orderedQuantity = orderedQuantity; }
    public Integer getShippedQuantity() { return shippedQuantity; }
    public void setShippedQuantity(Integer shippedQuantity) { this.shippedQuantity = shippedQuantity; }
    public Integer getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    public Integer getAcceptedQuantity() { return acceptedQuantity; }
    public void setAcceptedQuantity(Integer acceptedQuantity) { this.acceptedQuantity = acceptedQuantity; }
    public Integer getRejectedQuantity() { return rejectedQuantity; }
    public void setRejectedQuantity(Integer rejectedQuantity) { this.rejectedQuantity = rejectedQuantity; }
    public QualityStatus getQualityStatus() { return qualityStatus; }
    public void setQualityStatus(QualityStatus qualityStatus) { this.qualityStatus = qualityStatus; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

`

## GrnHeader.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "grn_header")
public class GrnHeader {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    private String deliveryNoteNumber;
    private LocalDate receiptDate = LocalDate.now();

    /**
     * Numéro de référence BAG ERP du GRN — partagé avec le GRC associé.
     * Formats : GRN-YYYYMM-XXXXX
     * Règle BAG ERP : GRC = MÊME numéro que son GRN.
     */
    @Column(name = "grn_number", unique = true)
    private String grnNumber;

    @ManyToOne
    @JoinColumn(name = "received_by")
    private User receivedBy;

    @ManyToOne
    @JoinColumn(name = "parent_grn_id")
    private GrnHeader parentGrn;

    @Enumerated(EnumType.STRING)
    private GrnStatus status;

    @OneToMany(mappedBy = "grnHeader", cascade = CascadeType.ALL)
    private List<GrnDetails> details;

    @OneToOne(mappedBy = "grnHeader")
    private GrcHeader grcHeader;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public String getDeliveryNoteNumber() { return deliveryNoteNumber; }
    public void setDeliveryNoteNumber(String deliveryNoteNumber) { this.deliveryNoteNumber = deliveryNoteNumber; }
    public String getGrnNumber() { return grnNumber; }
    public void setGrnNumber(String grnNumber) { this.grnNumber = grnNumber; }
    public LocalDate getReceiptDate() { return receiptDate; }
    public void setReceiptDate(LocalDate receiptDate) { this.receiptDate = receiptDate; }
    public User getReceivedBy() { return receivedBy; }
    public void setReceivedBy(User receivedBy) { this.receivedBy = receivedBy; }
    public GrnHeader getParentGrn() { return parentGrn; }
    public void setParentGrn(GrnHeader parentGrn) { this.parentGrn = parentGrn; }
    public GrnStatus getStatus() { return status; }
    public void setStatus(GrnStatus status) { this.status = status; }
    public List<GrnDetails> getDetails() { return details; }
    public void setDetails(List<GrnDetails> details) { this.details = details; }
    public GrcHeader getGrcHeader() { return grcHeader; }
    public void setGrcHeader(GrcHeader grcHeader) { this.grcHeader = grcHeader; }
}

`

## GrnStatus.java
`java
package com.pfe.gestionsachat.model;

/**
 * Statuts du GRN — alignés sur le flux BAG ERP.
 * PENDING       : GRN créé, marchandises en cours de vérification.
 * ENTRY_COMPLETED : Magasinier a coché "Entry Completed" — stock mis à jour.
 *                   Aucune approbation hiérarchique sur le GRN (règle BAG ERP).
 */
public enum GrnStatus {
    PENDING,
    ENTRY_COMPLETED
}

`

## Invoice.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "invoice")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @ManyToOne
    @JoinColumn(name = "grn_header_id")
    private GrnHeader grnHeader;

    private String invoiceNumber;
    private LocalDate invoiceDate = LocalDate.now();
    private java.math.BigDecimal montantHT;
    private java.math.BigDecimal montantTTC;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    public GrnHeader getGrnHeader() { return grnHeader; }
    public void setGrnHeader(GrnHeader grnHeader) { this.grnHeader = grnHeader; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public java.math.BigDecimal getMontantHT() { return montantHT; }
    public void setMontantHT(java.math.BigDecimal montantHT) { this.montantHT = montantHT; }
    public java.math.BigDecimal getMontantTTC() { return montantTTC; }
    public void setMontantTTC(java.math.BigDecimal montantTTC) { this.montantTTC = montantTTC; }
    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }
}

`

## InvoiceStatus.java
`java
package com.pfe.gestionsachat.model;

public enum InvoiceStatus {
    RECEIVED,
    MATCHED,
    APPROVED,
    PAID,
    REJECTED
}

`

## ItemCategory.java
`java
package com.pfe.gestionsachat.model;

public enum ItemCategory {
    PIECE_RECHANGE,
    CONSOMMABLE,
    ADMINISTRATIF
}

`

## Justification.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "justification")
public class Justification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_justif")
    private Integer idJustif;

    @ManyToOne
    @JoinColumn(name = "id_da")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private DaHeader daHeader;

    @Column(columnDefinition = "TEXT")
    private String texte;

    @Column(name = "date_justif")
    private LocalDate dateJustif;

    public Justification() {
        this.dateJustif = LocalDate.now();
    }

    public Justification(DaHeader daHeader, String texte) {
        this();
        this.daHeader = daHeader;
        this.texte = texte;
    }

    // Getters
    public Integer getIdJustif() { return idJustif; }
    public DaHeader getDaHeader() { return daHeader; }
    public String getTexte() { return texte; }
    public LocalDate getDateJustif() { return dateJustif; }

    // Setters
    public void setIdJustif(Integer idJustif) { this.idJustif = idJustif; }
    public void setDaHeader(DaHeader daHeader) { this.daHeader = daHeader; }
    public void setTexte(String texte) { this.texte = texte; }
    public void setDateJustif(LocalDate dateJustif) { this.dateJustif = dateJustif; }
}

`

## MovementType.java
`java
package com.pfe.gestionsachat.model;

public enum MovementType {
    IN_RECEIPT,
    OUT_RETURN,
    TRANSFER_IN,
    TRANSFER_OUT,
    CONSUMPTION,
    AFFECTATION
}

`

## OffreFournisseur.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "offre_fournisseur")
public class OffreFournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "da_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private DemandeAchatInterne da;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier fournisseur;

    private BigDecimal prixPropose;
    
    @Column(length = 500)
    private String conditions;

    private Integer delaiLivraisonOffert; // Optional: specific to this offer

    public OffreFournisseur() {}

    public OffreFournisseur(DemandeAchatInterne da, Supplier fournisseur, BigDecimal prixPropose, String conditions, Integer delai) {
        this.da = da;
        this.fournisseur = fournisseur;
        this.prixPropose = prixPropose;
        this.conditions = conditions;
        this.delaiLivraisonOffert = delai;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DemandeAchatInterne getDa() { return da; }
    public void setDa(DemandeAchatInterne da) { this.da = da; }
    public Supplier getFournisseur() { return fournisseur; }
    public void setFournisseur(Supplier fournisseur) { this.fournisseur = fournisseur; }
    public BigDecimal getPrixPropose() { return prixPropose; }
    public void setPrixPropose(BigDecimal prixPropose) { this.prixPropose = prixPropose; }
    public String getConditions() { return conditions; }
    public void setConditions(String conditions) { this.conditions = conditions; }
    public Integer getDelaiLivraisonOffert() { return delaiLivraisonOffert; }
    public void setDelaiLivraisonOffert(Integer delaiLivraisonOffert) { this.delaiLivraisonOffert = delaiLivraisonOffert; }
}

`

## POStatus.java
`java
package com.pfe.gestionsachat.model;

/**
 * Statuts du Bon de Commande (PO) — alignés sur le flux BAG ERP.
 * Machine à états stricte : toute transition hors du chemin défini lève une exception.
 *
 * DRAFT → PENDING_APPROVAL → APPROVED → (SHORT_CLOSED)
 *                          ↘ REJECTED
 */
public enum POStatus {
    /** PO créé mais non soumis à approbation */
    DRAFT,
    /** Soumis au Responsable Service Achat — en attente de décision */
    PENDING_APPROVAL,
    /** Approuvé — le Magasinier peut créer un GRN */
    APPROVED,
    /** Statut hérité de la version précédente (synonyme de APPROVED) */
    VALIDEE,
    /** Refusé par le Responsable Service Achat */
    REJECTED,
    /** Clôture manuelle forcée avant réception complète (Shipped > 0) */
    SHORT_CLOSED
}

`

## PurchaseOrder.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_order")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_po")
    @com.fasterxml.jackson.annotation.JsonProperty("id_po")
    private Integer idPo;

    @OneToOne
    @JoinColumn(name = "id_da")
    private DaHeader daHeader;

    @OneToOne
    @JoinColumn(name = "id_demande_interne")
    private DemandeAchatInterne demandeInterne;

    @ManyToOne
    @JoinColumn(name = "id_supplier")
    private Supplier fournisseur;

    @Column(name = "date_creation")
    @com.fasterxml.jackson.annotation.JsonProperty("date_creation")
    private LocalDate dateCreation;

    /**
     * Statut typé — remplace l'ancien String libre.
     * Machine à états : DRAFT → PENDING_APPROVAL → APPROVED | REJECTED | SHORT_CLOSED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private POStatus statut;

    @Column(name = "montant_total")
    @com.fasterxml.jackson.annotation.JsonProperty("montant_total")
    private BigDecimal montantTotal;

    /**
     * Numéro de référence BAG ERP (format : PO-YYYYMM-XXXXX).
     * Partagé dans les rapports de rapprochement PO/GRN/GRC.
     */
    @Column(name = "po_number", unique = true)
    private String poNumber;

    /**
     * GRNs associés à ce PO — traçabilité complète pour le reporting.
     */
    @OneToMany(mappedBy = "purchaseOrder", fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<GrnHeader> grns = new ArrayList<>();

    public PurchaseOrder() {
        this.dateCreation = LocalDate.now();
        this.statut = POStatus.DRAFT;
    }

    public PurchaseOrder(DaHeader daHeader, BigDecimal montantTotal) {
        this();
        this.daHeader = daHeader;
        this.montantTotal = montantTotal;
    }

    // ── Getters ────────────────────────────────────────────────
    public Integer getIdPo() { return idPo; }
    public DaHeader getDaHeader() { return daHeader; }
    public LocalDate getDateCreation() { return dateCreation; }
    public POStatus getStatut() { return statut; }
    /** Alias for AI/reporting layers that use getStatut() as String */
    @com.fasterxml.jackson.annotation.JsonGetter("statut")
    public String getStatutAsString() { return statut != null ? statut.name() : null; }
    public BigDecimal getMontantTotal() { return montantTotal; }
    public BigDecimal getTotalAmount() { return montantTotal; }
    public DemandeAchatInterne getDemandeInterne() { return demandeInterne; }
    public Supplier getFournisseur() { return fournisseur; }
    public String getPoNumber() { return poNumber; }
    public List<GrnHeader> getGrns() { return grns; }

    // ── Setters ────────────────────────────────────────────────
    public void setIdPo(Integer idPo) { this.idPo = idPo; }
    public void setDaHeader(DaHeader daHeader) { this.daHeader = daHeader; }
    public void setDemandeInterne(DemandeAchatInterne demandeInterne) { this.demandeInterne = demandeInterne; }
    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }
    public void setStatut(POStatus statut) { this.statut = statut; }
    public void setFournisseur(Supplier fournisseur) { this.fournisseur = fournisseur; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }
    public void setGrns(List<GrnHeader> grns) { this.grns = grns; }
}
`

## QualityStatus.java
`java
package com.pfe.gestionsachat.model;

public enum QualityStatus {
    PENDING,
    APPROVED,
    REJECTED,
    QUARANTINE
}

`

## Role.java
`java
package com.pfe.gestionsachat.model;

public enum Role {
    EMPLOYE,
    MANAGER_N1,
    MANAGER_N2,
    TECHNICIEN,
    ACHETEUR,
    /** Responsable Service Achat — BAG ERP : approuve le PO */
    RESP_ACHAT,
    /** Magasinier — BAG ERP : reçoit les marchandises et génère le GRN */
    MAGASINIER,
    /** Comptable — BAG ERP : valide le GRC (costing financier) */
    COMPTABLE,
    AMG,
    DAF,
    DG,
    ADMINISTRATEUR
}


`

## StatusHistory.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "status_history")
public class StatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entiteType;

    private Long entiteId;

    private String statutAvant;

    private String statutApres;

    @ManyToOne
    @JoinColumn(name = "modifie_par_id")
    private User modifiePar;

    private LocalDateTime dateModification;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    public StatusHistory() {
        this.dateModification = LocalDateTime.now();
    }

    public StatusHistory(String entiteType, Long entiteId, String statutAvant, String statutApres, User modifiePar, String commentaire) {
        this();
        this.entiteType = entiteType;
        this.entiteId = entiteId;
        this.statutAvant = statutAvant;
        this.statutApres = statutApres;
        this.modifiePar = modifiePar;
        this.commentaire = commentaire;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEntiteType() { return entiteType; }
    public void setEntiteType(String entiteType) { this.entiteType = entiteType; }

    public Long getEntiteId() { return entiteId; }
    public void setEntiteId(Long entiteId) { this.entiteId = entiteId; }

    public String getStatutAvant() { return statutAvant; }
    public void setStatutAvant(String statutAvant) { this.statutAvant = statutAvant; }

    public String getStatutApres() { return statutApres; }
    public void setStatutApres(String statutApres) { this.statutApres = statutApres; }

    public User getModifiePar() { return modifiePar; }
    public void setModifiePar(User modifiePar) { this.modifiePar = modifiePar; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
}

`

## StatutAjustement.java
`java
package com.pfe.gestionsachat.model;

public enum StatutAjustement {
    EN_ATTENTE_DG,
    EN_ATTENTE_DAF,
    EN_ATTENTE_ACHETEUR,
    EN_ATTENTE_AMG,
    VALIDE,
    REJETE
}

`

## StatutDA.java
`java
package com.pfe.gestionsachat.model;

public enum StatutDA {
    EN_ATTENTE_N1,
    EN_ATTENTE_N2,
    EN_ATTENTE_TECH,
    EN_ATTENTE_ACHAT,
    EN_ATTENTE_AMG,
    EN_ATTENTE_DAF,
    EN_ATTENTE_DG,
    EN_AJUSTEMENT,
    EN_ATTENTE_AJUSTEMENT_DAF,
    EN_ATTENTE_AJUSTEMENT_DG,
    EN_ATTENTE_PO,
    VALIDEE,
    PO_CREE,
    REJETEE
}

`

## StatutDemande.java
`java
package com.pfe.gestionsachat.model;

public enum StatutDemande {
    BROUILLON,
    SOUMISE,
    VALIDEE_N1,
    VALIDEE_TECH,
    EN_TRAITEMENT,
    AJUSTEMENT_DAF,
    AJUSTEMENT_DG,
    EN_VALIDATION_AMG,
    EN_VALIDATION_DAF,
    EN_VALIDATION_DG,
    APPROUVEE,
    PO_CREE,
    EN_LIVRAISON,
    AFFECTEE,
    DISPONIBLE_STOCK,
    REJETEE
}

`

## StockItem.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;

@Entity
@Table(name = "stock_item", uniqueConstraints = {
    @UniqueConstraint(name = "uk_stock_item_code_warehouse", columnNames = {"item_code", "warehouse_id"})
})
public class StockItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    private String itemCode;
    private String itemName;

    @Enumerated(EnumType.STRING)
    private ItemCategory category;

    /**
     * Code emplacement — généré automatiquement, logique virtuel (BAG ERP).
     * Format : LOC-{YYYYMM}-{UUID_SHORT}
     * Règle : 1 emplacement = 1 article unique (unicité stricte via @UniqueConstraint).
     */
    @Column(name = "location_code", unique = true)
    private String locationCode;

    private Integer quantityAvailable;
    private Integer quantityReserved;

    private Integer minStock;
    private Integer reorderPoint;
    private Double unitCost;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    public Integer getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(Integer quantityAvailable) { this.quantityAvailable = quantityAvailable; }
    public Integer getQuantityReserved() { return quantityReserved; }
    public void setQuantityReserved(Integer quantityReserved) { this.quantityReserved = quantityReserved; }
    public Integer getMinStock() { return minStock; }
    public void setMinStock(Integer minStock) { this.minStock = minStock; }
    public Integer getReorderPoint() { return reorderPoint; }
    public void setReorderPoint(Integer reorderPoint) { this.reorderPoint = reorderPoint; }
    public Double getUnitCost() { return unitCost; }
    public void setUnitCost(Double unitCost) { this.unitCost = unitCost; }
}

`

## StockMovement.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "stock_movement")
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "stock_item_id")
    private StockItem stockItem;

    @Enumerated(EnumType.STRING)
    private MovementType movementType;

    private Integer quantity;
    private Date movementDate;
    private String referenceDocument; // e.g. "GRN-123"

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public StockItem getStockItem() { return stockItem; }
    public void setStockItem(StockItem stockItem) { this.stockItem = stockItem; }
    public MovementType getMovementType() { return movementType; }
    public void setMovementType(MovementType movementType) { this.movementType = movementType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Date getMovementDate() { return movementDate; }
    public void setMovementDate(Date movementDate) { this.movementDate = movementDate; }
    public String getReferenceDocument() { return referenceDocument; }
    public void setReferenceDocument(String referenceDocument) { this.referenceDocument = referenceDocument; }
}

`

## SubFamily.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

@Entity
@Table(name = "sub_family")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "ignoreUnknown"})
public class SubFamily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @com.fasterxml.jackson.annotation.JsonProperty("id")
    @com.fasterxml.jackson.annotation.JsonAlias({"oid_sub", "id_sous_famille", "subFamilyId", "oidSub"})
    private Integer oidSub;

    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "id_family")
    @JsonIgnore
    private Family family;

    @com.fasterxml.jackson.annotation.JsonProperty("name")
    @com.fasterxml.jackson.annotation.JsonAlias({"libelle", "label"})
    private String libelle;

    @Column(name = "budget_initial")
    @com.fasterxml.jackson.annotation.JsonProperty("budget_initial")
    private BigDecimal budgetInitial;

    @Column(name = "budget_restant")
    @com.fasterxml.jackson.annotation.JsonProperty("budget_restant")
    private BigDecimal budgetRestant;

    @Column(name = "budget_engage")
    @com.fasterxml.jackson.annotation.JsonProperty("budget_engage")
    private BigDecimal budgetEngage = BigDecimal.ZERO;

    @OneToMany(mappedBy = "subFamily")
    @JsonIgnore
    private List<DaDetails> details = new ArrayList<>();

    @OneToMany(mappedBy = "subSource")
    @JsonIgnore
    private List<BudgetTransfer> transfersSource = new ArrayList<>();

    @OneToMany(mappedBy = "subCible")
    @JsonIgnore
    private List<BudgetTransfer> transfersCible = new ArrayList<>();

    public SubFamily() {}

    public SubFamily(String libelle, BigDecimal budgetInitial, Family family) {
        this.libelle = libelle;
        this.budgetInitial = budgetInitial;
        this.budgetRestant = budgetInitial;
        this.family = family;
    }

    public boolean hasEnoughBudget(BigDecimal amount) {
        return budgetRestant != null && budgetRestant.compareTo(amount) >= 0;
    }

    public void deductBudget(BigDecimal amount) {
        if (this.budgetRestant != null) {
            this.budgetRestant = this.budgetRestant.subtract(amount);
        }
        this.budgetEngage = (this.budgetEngage != null ? this.budgetEngage : BigDecimal.ZERO).add(amount);
    }

    // Getters
    public Integer getOidSub() { return oidSub; }
    public Family getFamily() { return family; }
    public String getLibelle() { return libelle; }
    public BigDecimal getBudgetInitial() { return budgetInitial; }
    public BigDecimal getBudgetRestant() { return budgetRestant; }
    public BigDecimal getBudgetEngage() { return budgetEngage; }

    @com.fasterxml.jackson.annotation.JsonProperty("budget_disponible")
    @Transient
    public BigDecimal getBudgetDisponible() {
        return budgetRestant != null ? budgetRestant : BigDecimal.ZERO;
    }
    public List<DaDetails> getDetails() { return details; }
    public List<BudgetTransfer> getTransfersSource() { return transfersSource; }
    public List<BudgetTransfer> getTransfersCible() { return transfersCible; }

    // Setters
    public void setOidSub(Integer oidSub) { this.oidSub = oidSub; }
    public void setFamily(Family family) { this.family = family; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
    public void setBudgetInitial(BigDecimal budgetInitial) { this.budgetInitial = budgetInitial; }
    public void setBudgetRestant(BigDecimal budgetRestant) { this.budgetRestant = budgetRestant; }
    public void setBudgetEngage(BigDecimal budgetEngage) { this.budgetEngage = budgetEngage; }
    public void setDetails(List<DaDetails> details) { this.details = details; }
    public void setTransfersSource(List<BudgetTransfer> transfersSource) { this.transfersSource = transfersSource; }
    public void setTransfersCible(List<BudgetTransfer> transfersCible) { this.transfersCible = transfersCible; }
}
`

## Supplier.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "supplier")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oid_supplier")
    private Integer oidSupplier;

    private String nom;
    private String contact;
    private String email;
    private String phone;
    private String adresse;
    private String sector;
    private Integer rating; // 1-5
    private Integer averageLeadTime; // in days
    private Boolean isCertified;
    private String ice;

    @OneToMany(mappedBy = "fournisseur")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<DaDetails> details = new ArrayList<>();

    public Supplier() {}

    public Supplier(String nom, String contact, String adresse, String sector, Integer rating, Integer leadTime) {
        this.nom = nom;
        this.contact = contact;
        this.adresse = adresse;
        this.sector = sector;
        this.rating = rating;
        this.averageLeadTime = leadTime;
        this.isCertified = false;
    }

    // Getters
    public Integer getOidSupplier() { return oidSupplier; }
    public String getNom() { return nom; }
    public String getContact() { return contact; }
    public String getAdresse() { return adresse; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getSector() { return sector; }
    public Integer getRating() { return rating; }
    public Integer getAverageLeadTime() { return averageLeadTime; }
    public Boolean getIsCertified() { return isCertified; }
    public String getIce() { return ice; }
    public List<DaDetails> getDetails() { return details; }

    // Setters
    public void setOidSupplier(Integer oidSupplier) { this.oidSupplier = oidSupplier; }
    public void setNom(String nom) { this.nom = nom; }
    public void setContact(String contact) { this.contact = contact; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setSector(String sector) { this.sector = sector; }
    public void setRating(Integer rating) { this.rating = rating; }
    public void setAverageLeadTime(Integer averageLeadTime) { this.averageLeadTime = averageLeadTime; }
    public void setIsCertified(Boolean isCertified) { this.isCertified = isCertified; }
    public void setIce(String ice) { this.ice = ice; }
    public void setDetails(List<DaDetails> details) { this.details = details; }
}
`

## TransferRequest.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;

@Entity
@Table(name = "transfer_request")
public class TransferRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_warehouse_id")
    private Warehouse sourceWarehouse;

    @ManyToOne
    @JoinColumn(name = "destination_warehouse_id")
    private Warehouse destinationWarehouse;

    private String itemCode;
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private TransferStatus status;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Warehouse getSourceWarehouse() { return sourceWarehouse; }
    public void setSourceWarehouse(Warehouse sourceWarehouse) { this.sourceWarehouse = sourceWarehouse; }
    public Warehouse getDestinationWarehouse() { return destinationWarehouse; }
    public void setDestinationWarehouse(Warehouse destinationWarehouse) { this.destinationWarehouse = destinationWarehouse; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }
}

`

## TransferStatus.java
`java
package com.pfe.gestionsachat.model;

public enum TransferStatus {
    REQUESTED,
    IN_TRANSIT,
    DELIVERED
}

`

## TypeAction.java
`java
package com.pfe.gestionsachat.model;

public enum TypeAction {
    CREATION_DA,
    SOUMISSION,
    VALIDATION,
    REJET,
    AJUST_BUDGET_SF,
    VALID_BUDGET_FAMILLE,
    TRANSFERT_BUDGET,
    CREATION_PO,
    ALERTE_RETARD
}

`

## TypeAjustement.java
`java
package com.pfe.gestionsachat.model;

public enum TypeAjustement {
    SOUS_FAMILLE,
    FAMILLE
}

`

## UrgenceDemande.java
`java
package com.pfe.gestionsachat.model;

public enum UrgenceDemande {
    NORMALE,
    URGENTE,
    CRITIQUE
}

`

## User.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oid_user")
    @com.fasterxml.jackson.annotation.JsonProperty("oid_user")
    @com.fasterxml.jackson.annotation.JsonAlias({"id", "id_demandeur", "userId", "idDemandeur"})
    private Integer oidUser;

    private String nom;
    private String email;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String service;

    @ManyToOne
    @JoinColumn(name = "n1_id")
    @com.fasterxml.jackson.annotation.JsonProperty("n1")
    private User n1;

    private Boolean actif;

    @OneToMany(mappedBy = "user")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Action> actions = new ArrayList<>();

    public User() {}

    public User(String nom, String email, String password, Role role) {
        this.nom = nom;
        this.email = email;
        this.password = password;
        this.role = role;
        this.actif = true;
    }

    // Getters
    public Integer getOidUser() { return oidUser; }
    public String getNom() { return nom; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public String getService() { return service; }
    public User getN1() { return n1; }
    public Boolean getActif() { return actif; }
    public List<Action> getActions() { return actions; }

    @com.fasterxml.jackson.annotation.JsonProperty("n1_id")
    public Integer getN1Id() {
        return n1 != null ? n1.getOidUser() : null;
    }

    // Setters
    public void setOidUser(Integer oidUser) { this.oidUser = oidUser; }
    public void setNom(String nom) { this.nom = nom; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(Role role) { this.role = role; }
    public void setService(String service) { this.service = service; }
    public void setN1(User n1) { this.n1 = n1; }
    public void setActif(Boolean actif) { this.actif = actif; }
    public void setActions(List<Action> actions) { this.actions = actions; }
}
`

## ValidationDecision.java
`java
package com.pfe.gestionsachat.model;

public enum ValidationDecision {
    ACCEPTE,
    REJETE
}

`

## Warehouse.java
`java
package com.pfe.gestionsachat.model;

import jakarta.persistence.*;

@Entity
@Table(name = "warehouse")
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String location;

    @Enumerated(EnumType.STRING)
    private WarehouseType type;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public WarehouseType getType() { return type; }
    public void setType(WarehouseType type) { this.type = type; }
}

`

## WarehouseType.java
`java
package com.pfe.gestionsachat.model;

public enum WarehouseType {
    CENTRAL,
    REGIONAL,
    LOCAL
}

`

## AchatWorkflowOrchestrator.java
`java
public BudgetCheckResult(boolean s, BigDecimal mr, BigDecimal ba, String m);

@Transactional
public BudgetCheckResult verifierBudget(Integer daId, Integer userId);

@Transactional
public DaHeader processValidation(Integer daId, Integer userId, ValidationDecision decision, String motif);

@Transactional
public DaHeader solliciterAjustement(Integer daId, Integer userId, String type, String motif);

@Transactional
public DaHeader ajusterBudgetSousFamille(Integer daId, Integer dafId, Integer sourceId, Integer cibleId, BigDecimal montant);

@Transactional
public DaHeader ajusterBudgetFamille(Integer daId, Integer dgId, Integer cibleId, BigDecimal montant);

@Transactional
public PurchaseOrder manualCreatePO(Integer daId, Integer acheteurId);

`

## AuthService.java
`java
public User login(String email, String password);

public User getUserByEmail(String email);

`

## BudgetSuiviService.java
`java
public List<BudgetFamilleDto> getSuiviBudgetaire();

@Transactional
public ConsommerBudgetResponse consommerBudget(ConsommerBudgetRequest request);

public BudgetFamilleDto getFamilleDetail(@jakarta.annotation.Nonnull Integer idFamille);

public List<AlerteBudgetaireDto> getAlertes();

@Transactional
`

## CreditNoteService.java
`java
@Transactional
public CreditNote processCreditNote(CreditNoteRequest request);

`

## DaHeaderService.java
`java
@Transactional
public DaHeader createPurchaseRequest(@NonNull DaHeader request);

public DaHeader submitPurchaseRequest(@NonNull Integer id);

public List<DaHeader> getAllPurchaseRequests();

public DaHeader getPurchaseRequestById(Integer id);

public List<DaHeader> getPurchaseRequestsByStatus(StatutDA statut);

public List<DaHeader> getPurchaseRequestsByDemandeur(@NonNull Integer oidUser);

public DaHeader updatePurchaseRequest(@NonNull Integer id, @NonNull DaHeader requestDetails);

public void deletePurchaseRequest(@NonNull Integer id);

public List<DaHeader> searchPurchaseRequests(String keyword);

public long countByStatut(StatutDA statut);

`

## DemandeAchatInterneService.java
`java
@Transactional
public DemandeAchatInterne createDemande(DemandeAchatInterne demande, User demandeur);

@Transactional
public DemandeAchatInterne soumettre(@NonNull Long id, @NonNull User demandeur);

@Transactional
public DemandeAchatInterne validerN1(Long id, boolean valider, String commentaire, User n1);

@Transactional
public DemandeAchatInterne validerTechnicien(Long id, boolean valider, String commentaire, User tech);

@Transactional
public DemandeAchatInterne valoriserDemande(Long id, java.math.BigDecimal prixUnitaire, Integer supplierId);

@Transactional
public DemandeAchatInterne traiterAchat(Long id, User acheteur);

@Transactional
public DemandeAchatInterne ajustementBudget(Long id, TypeAjustement type, User acheteur);

@Transactional
public DemandeAchatInterne validerAjustement(Long id, boolean valider, String commentaire, User validateur);

@Transactional
public DemandeAchatInterne validerAMG(Long id, boolean valider, String commentaire, User amg);

@Transactional
public DemandeAchatInterne validerDAF(Long id, boolean valider, String commentaire, User daf);

@Transactional
public DemandeAchatInterne validerDG(Long id, boolean valider, String commentaire, User dg);

@Transactional
public PurchaseOrder creerPO(@NonNull Long id, @NonNull User acheteur);

public List<DemandeAchatInterne> getMesDemandes(User demandeur);

public List<DemandeAchatInterne> getDemandesAValider(User utilisateur);

`

## DemandeAjustementService.java
`java
@Transactional
public DemandeAjustement soumettreAjustementFamille(@NonNull Integer daId, @NonNull Integer familleCibleId, @NonNull BigDecimal montantDemande, String justification, @NonNull Long acheteurId);

public DemandeAjustement deciderDg(@NonNull Long id, @NonNull Long dgId, @NonNull String decision, String justification);

public DemandeAjustement soumettreAjustementSousFamille(@NonNull Integer daId, @NonNull Integer sourceSousFamilleId, @NonNull Integer cibleSousFamilleId, @NonNull BigDecimal montantDemande, String justification, @NonNull Long acheteurId);

public DemandeAjustement deciderDaf(@NonNull Long id, @NonNull Long dafId, @NonNull String decision, BigDecimal montantFinal, String justification);

public DemandeAjustement confirmerAcheteur(@NonNull Long id, @NonNull Long acheteurId);

public DemandeAjustement finaliserAmg(@NonNull Long id);

public DaHeader legacyAjusterBudgetSousFamille(Integer daId, Integer dafId, Integer subSourceId, Integer subCibleId, BigDecimal montant);

public DaHeader legacyAjusterBudgetFamille(Integer daId, Integer dgId, Integer subCibleId, BigDecimal montant);

`

## DemandeInterneService.java
`java
@Transactional
public DemandeAchatInterne soumettre(DemandeAchatInterne da, User demandeur);

`

## FamilyService.java
`java
public Family createFamily(@NonNull Family family);

public List<Family> getAllFamilies();

public Family getFamilyById(@NonNull Integer id);

public Family updateFamily(@NonNull Integer id, @NonNull Family familyDetails);

public void deleteFamily(@NonNull Integer id);

`

## GrcService.java
`java
@Transactional
public GrcHeader createGrc(GrcHeader grc);

@Transactional
public GrcHeader validateGrc(Long grcId, User comptable);

@Transactional
public GrcHeader validateGrc(Long grcId);

@Transactional
public GrcHeader approveGrc(Long grcId);

`

## GrnService.java
`java
@Transactional
public GrnHeader createGrn(GrnHeader grn);

@Transactional
public GrnHeader completeGrnEntry(Long grnId, User magasinier);

@Transactional
public GrnHeader validateGrn(Long grnId);

`

## MatchingService.java
`java
@Transactional
public Invoice matchInvoice(@org.springframework.lang.NonNull Long invoiceId);

@Transactional
public Invoice approveInvoice(@org.springframework.lang.NonNull Long invoiceId);

`

## NotificationService.java
`java
public void notifyUser(User user, String title, String message);

public void notifyTopic(String topic, String message);

`

## PdfExportService.java
`java
public byte[] generatePurchaseOrderPdf(PurchaseOrder po);

public byte[] generateGrnPdf(com.pfe.gestionsachat.model.GrnHeader grn);

public byte[] generateGrcPdf(com.pfe.gestionsachat.model.GrcHeader grc);

public byte[] generateInvoicePdf(com.pfe.gestionsachat.model.Invoice invoice);

`

## PurchaseOrderService.java
`java
public List<PurchaseOrder> getAllPurchaseOrders();

public java.util.Map<String, Integer> getPoBalance(Integer poId);

public PurchaseOrder getPurchaseOrderById(Integer id);

public PurchaseOrder getPurchaseOrderByDa(Integer oidDa);

public List<PurchaseOrder> getPurchaseOrdersByStatus(POStatus statut);

@Transactional
public PurchaseOrder generateFromInternal(DemandeAchatInterne demande);

@Transactional
public PurchaseOrder generateFromClassic(DaHeader da);

@Transactional
public PurchaseOrder submitForApproval(Integer poId, User acheteur);

@Transactional
public PurchaseOrder approvePO(Integer poId, User responsable, String commentaire);

@Transactional
public PurchaseOrder rejectPO(Integer poId, User responsable, String motif);

@Transactional
public PurchaseOrder shortClose(Integer poId, User responsable, String motif);

`

## StockAlertService.java
`java
public void checkStockLevels();

`

## SubFamilyService.java
`java
public SubFamily createSubFamily(@NonNull SubFamily subFamily);

public List<SubFamily> getAllSubFamilies();

public SubFamily getSubFamilyById(@NonNull Integer id);

public List<SubFamily> getSubFamiliesByFamily(@NonNull Integer familyId);

public List<SubFamily> getSubFamiliesByFamilyName(String familyName);

public SubFamily updateSubFamily(@NonNull Integer id, @NonNull SubFamily subFamilyDetails);

public void deleteSubFamily(@NonNull Integer id);

`

## SupplierService.java
`java
public Supplier createSupplier(@NonNull Supplier supplier);

public List<Supplier> getAllSuppliers();

public Supplier getSupplierById(@NonNull Integer id);

public Supplier updateSupplier(@NonNull Integer id, @NonNull Supplier supplierDetails);

public void deleteSupplier(@NonNull Integer id);

`

## UserService.java
`java
public User createUser(@NonNull User user);

public List<User> getAllUsers();

public User getUserById(@NonNull Integer id);

public User getUserByEmail(@NonNull String email);

public List<User> getUsersByRole(@NonNull Role role);

public User updateUser(@NonNull Integer id, @NonNull User userDetails);

public User changePassword(@NonNull Integer id, @NonNull String newPassword);

public void deleteUser(@NonNull Integer id);

public List<User> searchUsers(String keyword);

public User authenticate(@NonNull String email, @NonNull String password);

`

## WarehouseService.java
`java
@Transactional
public void addStock(String itemCode, String itemName, Integer quantity, String referenceDoc);

@Transactional
public void addStock(String itemCode, Integer quantity, String referenceDoc);

@Transactional
public void removeStock(String itemCode, Integer quantity, String referenceDoc);

public boolean verifierStock(String designation, Integer quantite);

@Transactional
public void affecterStock(String designation, Integer quantite, String reference);

`

## WorkflowMonitorService.java
`java
public void monitorGhostWorkflows();

`

## AchatWorkflowController.java
`java
@RequestMapping("/api/workflow")

@PostMapping("/validate")
public ResponseEntity<DaHeader> validate(;

@PostMapping("/check-budget")
public ResponseEntity<AchatWorkflowOrchestrator.BudgetCheckResult> checkBudget(;

@PostMapping("/adjust-subfamily")
public ResponseEntity<DaHeader> adjustSubFamily(;

@PostMapping("/adjust-family")
public ResponseEntity<DaHeader> adjustFamily(;

@PostMapping("/create-po")
public ResponseEntity<PurchaseOrder> createPO(;

@PostMapping("/request-adjustment")
public ResponseEntity<DaHeader> requestAdjustment(;

`

## AdminController.java
`java
@RequestMapping("/api/admin")

@GetMapping("/users")
public ResponseEntity<List<User>> getUsers();
// Appelle: return ResponseEntity.ok(userService.getAllUsers());

@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody @NonNull User user);
// Appelle: return ResponseEntity.ok(userService.createUser(user));

@GetMapping("/dashboard")
public ResponseEntity<Map<String, Object>> getDashboard();
// Appelle: stats.put("totalUsers", userService.getAllUsers().size());

`

## AuthController.java
`java
@RequestMapping(value = "/api/auth", produces = "application/json")

@PostMapping("/login")
public ResponseEntity<Map<String, Object>> login(;

`

## BudgetController.java
`java
@RequestMapping("/api/budget")

@GetMapping("/suivi")
@PostMapping("/consommer")
@GetMapping("/famille/{id}")
@GetMapping("/alertes")
`

## CreditNoteController.java
`java
@RequestMapping("/api/creditnote")

@PostMapping
public ResponseEntity<CreditNote> createCreditNote(@RequestBody CreditNoteRequest request);
// Appelle: return ResponseEntity.ok(creditNoteService.processCreditNote(request));

`

## DaHeaderController.java
`java
@RequestMapping({"/api/da-headers", "/api/purchase-requests", "/api/purchase-request", "/api/requests", "/api/request", "/api/da"})

@PostMapping
public ResponseEntity<DaHeader> createPurchaseRequest(@RequestBody @org.springframework.lang.NonNull DaHeader request);
// Appelle: return ResponseEntity.ok(daHeaderService.createPurchaseRequest(request));

@PostMapping("/{id}/submit")
public ResponseEntity<DaHeader> submitPurchaseRequest(@PathVariable @org.springframework.lang.NonNull Integer id);
// Appelle: return ResponseEntity.ok(daHeaderService.submitPurchaseRequest(id));

@GetMapping
public ResponseEntity<List<DaHeader>> getAllPurchaseRequests();
// Appelle: return ResponseEntity.ok(daHeaderService.getAllPurchaseRequests());

@GetMapping("/{id}")
public ResponseEntity<DaHeader> getPurchaseRequestById(@PathVariable @org.springframework.lang.NonNull Integer id);
// Appelle: return ResponseEntity.ok(daHeaderService.getPurchaseRequestById(id));

@GetMapping("/status/{statut}")
public ResponseEntity<List<DaHeader>> getPurchaseRequestsByStatus(@PathVariable @org.springframework.lang.NonNull StatutDA statut);
// Appelle: return ResponseEntity.ok(daHeaderService.getPurchaseRequestsByStatus(statut));

@GetMapping("/demandeur/{userId}")
public ResponseEntity<List<DaHeader>> getPurchaseRequestsByDemandeur(@PathVariable @org.springframework.lang.NonNull Integer userId);
// Appelle: return ResponseEntity.ok(daHeaderService.getPurchaseRequestsByDemandeur(userId));

@PutMapping("/{id}")
public ResponseEntity<DaHeader> updatePurchaseRequest(@PathVariable @org.springframework.lang.NonNull Integer id, @RequestBody @org.springframework.lang.NonNull DaHeader requestDetails);
// Appelle: return ResponseEntity.ok(daHeaderService.updatePurchaseRequest(id, requestDetails));

@DeleteMapping("/{id}")
public ResponseEntity<Void> deletePurchaseRequest(@PathVariable @org.springframework.lang.NonNull Integer id);
// Appelle: daHeaderService.deletePurchaseRequest(id);

`

## DemandeAchatInterneController.java
`java
@RequestMapping(value = "/api/demandes", produces = "application/json")

@GetMapping
public ResponseEntity<List<DemandeAchatInterne>> getAll();

@PostMapping
public ResponseEntity<DemandeAchatInterne> create(@RequestBody @NonNull DemandeAchatInterne demande, @RequestParam @NonNull Integer userId);
// Appelle: return ResponseEntity.ok(demandeService.createDemande(demande, user));

@GetMapping("/{id}")
public ResponseEntity<DemandeAchatInterne> getById(@PathVariable Long id);

@PostMapping("/{id}/soumettre")
public ResponseEntity<DemandeAchatInterne> soumettre(@PathVariable @NonNull Long id, @RequestParam @NonNull Integer userId);
// Appelle: return ResponseEntity.ok(demandeService.soumettre(id, user));

@PutMapping("/{id}/valider-n1")
public ResponseEntity<DemandeAchatInterne> validerN1(@PathVariable @NonNull Long id, @RequestBody @NonNull Map<String, Object> payload, @RequestParam @NonNull Integer userId);
// Appelle: return ResponseEntity.ok(demandeService.validerN1(id, valider, commentaire, user));

@PutMapping("/{id}/valider-technicien")
public ResponseEntity<DemandeAchatInterne> validerTechnicien(@PathVariable @NonNull Long id, @RequestBody @NonNull Map<String, Object> payload, @RequestParam @NonNull Integer userId);
// Appelle: return ResponseEntity.ok(demandeService.validerTechnicien(id, valider, commentaire, user));

@PutMapping("/{id}/valoriser-achat")
public ResponseEntity<DemandeAchatInterne> valoriserDemande(;
// Appelle: return ResponseEntity.ok(demandeService.valoriserDemande(id, prixUnitaire, supplierId));

@PutMapping("/{id}/traiter-achat")
public ResponseEntity<DemandeAchatInterne> traiterAchat(@PathVariable @NonNull Long id, @RequestParam @NonNull Integer userId);
// Appelle: return ResponseEntity.ok(demandeService.traiterAchat(id, user));

@PutMapping("/{id}/valider-amg")
public ResponseEntity<DemandeAchatInterne> validerAMG(@PathVariable @NonNull Long id, @RequestBody @NonNull Map<String, Object> payload, @RequestParam @NonNull Integer userId);
// Appelle: return ResponseEntity.ok(demandeService.validerAMG(id, valider, commentaire, user));

@PutMapping("/{id}/valider-daf")
public ResponseEntity<DemandeAchatInterne> validerDAF(@PathVariable @NonNull Long id, @RequestBody @NonNull Map<String, Object> payload, @RequestParam @NonNull Integer userId);
// Appelle: return ResponseEntity.ok(demandeService.validerDAF(id, valider, commentaire, user));

@PutMapping("/{id}/valider-dg")
public ResponseEntity<DemandeAchatInterne> validerDG(@PathVariable @NonNull Long id, @RequestBody @NonNull Map<String, Object> payload, @RequestParam @NonNull Integer userId);
// Appelle: return ResponseEntity.ok(demandeService.validerDG(id, valider, commentaire, user));

@PostMapping("/{id}/ajustement")
public ResponseEntity<DemandeAchatInterne> ajustement(@PathVariable @NonNull Long id, @RequestParam @NonNull com.pfe.gestionsachat.model.TypeAjustement type, @RequestParam @NonNull Integer userId);
// Appelle: return ResponseEntity.ok(demandeService.ajustementBudget(id, type, user));

@PostMapping("/{id}/creer-po")
public ResponseEntity<com.pfe.gestionsachat.model.PurchaseOrder> creerPO(@PathVariable @NonNull Long id, @RequestParam @NonNull Integer userId);
// Appelle: return ResponseEntity.ok(demandeService.creerPO(id, user));

@GetMapping("/mes-demandes")
public ResponseEntity<List<DemandeAchatInterne>> getMesDemandes(@RequestParam @NonNull Integer userId);
// Appelle: return ResponseEntity.ok(demandeService.getMesDemandes(user));

@GetMapping("/a-valider")
public ResponseEntity<List<DemandeAchatInterne>> getAValider(@RequestParam @NonNull Integer userId);
// Appelle: return ResponseEntity.ok(demandeService.getDemandesAValider(user));

@GetMapping("/{id}/offres")
public ResponseEntity<List<OffreFournisseur>> getOffres(@PathVariable Long id);

`

## DemandeAjustementController.java
`java
@RequestMapping("/api/ajustement")

@PostMapping("/famille/soumettre")
public ResponseEntity<?> soumettreFamille(@RequestBody Map<String, Object> payload);
// Appelle: DemandeAjustement daAjust = demandeAjustementService.soumettreAjustementFamille(daId, familleCibleId, montantDemande, justification, acheteurId);

@PostMapping("/{id}/dg/decider")
public ResponseEntity<?> deciderDg(@PathVariable Long id, @RequestBody Map<String, Object> payload);
// Appelle: DemandeAjustement daAjust = demandeAjustementService.deciderDg(id, dgId, decision, justification);

@PostMapping("/sous-famille/soumettre")
public ResponseEntity<?> soumettreSousFamille(@RequestBody Map<String, Object> payload);
// Appelle: DemandeAjustement daAjust = demandeAjustementService.soumettreAjustementSousFamille(daId, sourceSousFamilleId, cibleSousFamilleId, montantDemande, justification, acheteurId);

@PostMapping("/{id}/daf/decider")
public ResponseEntity<?> deciderDaf(@PathVariable Long id, @RequestBody Map<String, Object> payload);
// Appelle: DemandeAjustement daAjust = demandeAjustementService.deciderDaf(id, dafId, decision, montantFinal, justification);

@PostMapping("/{id}/acheteur/confirmer")
public ResponseEntity<?> confirmerAcheteur(@PathVariable Long id, @RequestBody Map<String, Object> payload);
// Appelle: DemandeAjustement daAjust = demandeAjustementService.confirmerAcheteur(id, acheteurId);

@PostMapping("/{id}/amg/finaliser")
public ResponseEntity<?> finaliserAmg(@PathVariable Long id);
// Appelle: DemandeAjustement daAjust = demandeAjustementService.finaliserAmg(id);

@GetMapping("/{id}")
public ResponseEntity<?> getDemandeAjustement(@PathVariable Long id);

`

## FamilyController.java
`java
@RequestMapping({"/api/families", "/api/family", "/api/categories", "/api/category", "/api/familles"})

@PostMapping
public ResponseEntity<Family> createFamily(@RequestBody @org.springframework.lang.NonNull Family family);
// Appelle: return ResponseEntity.ok(familyService.createFamily(family));

@GetMapping(produces = "application/json;charset=UTF-8")
public ResponseEntity<?> getAllFamilies(@RequestParam(required = false, defaultValue = "DEMANDEUR") String role);
// Appelle: List<Family> families = familyService.getAllFamilies();

@GetMapping("/{id}")
public ResponseEntity<Family> getFamilyById(@PathVariable @org.springframework.lang.NonNull Integer id);
// Appelle: return ResponseEntity.ok(familyService.getFamilyById(id));

@PutMapping("/{id}")
public ResponseEntity<Family> updateFamily(@PathVariable @org.springframework.lang.NonNull Integer id, @RequestBody @org.springframework.lang.NonNull Family familyDetails);
// Appelle: return ResponseEntity.ok(familyService.updateFamily(id, familyDetails));

@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteFamily(@PathVariable @org.springframework.lang.NonNull Integer id);
// Appelle: familyService.deleteFamily(id);

`

## GrcController.java
`java
@RequestMapping("/api/grc")

@PostMapping
public ResponseEntity<GrcHeader> createGrc(@RequestBody GrcHeader grc);
// Appelle: return ResponseEntity.ok(grcService.createGrc(grc));

@PutMapping("/{id}/valider")
public ResponseEntity<GrcHeader> validateGrc(@PathVariable Long id);
// Appelle: return ResponseEntity.ok(grcService.validateGrc(Objects.requireNonNull(id)));

@PutMapping("/{id}/approuver")
public ResponseEntity<GrcHeader> approveGrc(@PathVariable Long id);
// Appelle: return ResponseEntity.ok(grcService.approveGrc(Objects.requireNonNull(id)));

@GetMapping("/{id}/download")
`

## GrnController.java
`java
@RequestMapping("/api/grn")

@PostMapping
public ResponseEntity<GrnHeader> createGrn(@RequestBody GrnHeader grn);
// Appelle: return ResponseEntity.ok(grnService.createGrn(grn));

@PutMapping("/{id}/valider")
public ResponseEntity<GrnHeader> validateGrn(@PathVariable Long id);
// Appelle: return ResponseEntity.ok(grnService.validateGrn(Objects.requireNonNull(id)));

@GetMapping("/status/{status}")
public ResponseEntity<List<GrnHeader>> getByStatus(@PathVariable GrnStatus status);

@GetMapping("/{id}/download")
`

## InvoiceController.java
`java
@RequestMapping("/api/invoice")

@GetMapping
public ResponseEntity<List<Invoice>> getAll();

@GetMapping("/{id}")
public ResponseEntity<Invoice> getById(@PathVariable Long id);

@PostMapping
public ResponseEntity<Invoice> createInvoice(@RequestBody Invoice invoice);

@PostMapping("/{id}/match")
public ResponseEntity<Invoice> matchInvoice(@PathVariable Long id);
// Appelle: return ResponseEntity.ok(matchingService.matchInvoice(java.util.Objects.requireNonNull(id)));

@PostMapping("/{id}/approve")
public ResponseEntity<Invoice> approveInvoice(@PathVariable Long id);
// Appelle: return ResponseEntity.ok(matchingService.approveInvoice(java.util.Objects.requireNonNull(id)));

@GetMapping("/{id}/download")
public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id);
// Appelle: byte[] pdfBytes = pdfExportService.generateInvoicePdf(invoice);

`

## PurchaseOrderController.java
`java
@RequestMapping("/api/purchase-orders")

@GetMapping
public ResponseEntity<List<PurchaseOrder>> getAllPurchaseOrders();
// Appelle: return ResponseEntity.ok(purchaseOrderService.getAllPurchaseOrders());

@GetMapping("/{id}")
public ResponseEntity<PurchaseOrder> getPurchaseOrderById(@PathVariable @org.springframework.lang.NonNull Integer id);
// Appelle: return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderById(id));

@GetMapping("/da/{oidDa}")
public ResponseEntity<PurchaseOrder> getPurchaseOrderByDa(@PathVariable @org.springframework.lang.NonNull Integer oidDa);
// Appelle: return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderByDa(oidDa));

@GetMapping("/status/{statut}")
public ResponseEntity<List<PurchaseOrder>> getPurchaseOrdersByStatus(;
// Appelle: return ResponseEntity.ok(purchaseOrderService.getPurchaseOrdersByStatus(statut));

@GetMapping("/{id}/balance")
public ResponseEntity<java.util.Map<String, Integer>> getPoBalance(@PathVariable Integer id);
// Appelle: return ResponseEntity.ok(purchaseOrderService.getPoBalance(id));

@PutMapping("/{id}/approve")
public ResponseEntity<PurchaseOrder> approvePO(;
// Appelle: return ResponseEntity.ok(purchaseOrderService.approvePO(id, responsable, commentaire));

@PutMapping("/{id}/reject")
public ResponseEntity<PurchaseOrder> rejectPO(;
// Appelle: return ResponseEntity.ok(purchaseOrderService.rejectPO(id, responsable, motif));

@PutMapping("/{id}/short-close")
public ResponseEntity<PurchaseOrder> shortClose(;
// Appelle: return ResponseEntity.ok(purchaseOrderService.shortClose(id, responsable, motif));

@GetMapping("/{id}/download")
public ResponseEntity<byte[]> downloadPo(@PathVariable Integer id);
// Appelle: PurchaseOrder po = purchaseOrderService.getPurchaseOrderById(id);

@GetMapping("/da/{oidDa}/download")
public ResponseEntity<byte[]> downloadPoByDa(@PathVariable Integer oidDa);
// Appelle: PurchaseOrder po = purchaseOrderService.getPurchaseOrderByDa(oidDa);

`

## SubFamilyController.java
`java
@RequestMapping({"/api/sub-families", "/api/sub-family", "/api/subcategories", "/api/subcategory", "/api/sous-familles"})

@GetMapping("/test-debug/**")
public ResponseEntity<String> debugRequest(jakarta.servlet.http.HttpServletRequest request);

@PostMapping
public ResponseEntity<SubFamily> createSubFamily(@RequestBody @org.springframework.lang.NonNull SubFamily subFamily);
// Appelle: return ResponseEntity.ok(subFamilyService.createSubFamily(subFamily));

@GetMapping(produces = "application/json;charset=UTF-8")
@GetMapping(value = "/{id}", produces = "application/json;charset=UTF-8")
public ResponseEntity<Object> getSubFamilyByIdOrFamilyChildren(@PathVariable String id);
// Appelle: List<SubFamily> subFamilies = subFamilyService.getSubFamiliesByFamily(numericId);

@GetMapping(value = {"/family/{familyIdOrName}", "/famille/{familyIdOrName}"}, produces = "application/json;charset=UTF-8")
public ResponseEntity<List<SubFamily>> getSubFamiliesByFamily(@PathVariable String familyIdOrName);
// Appelle: return ResponseEntity.ok(subFamilyService.getSubFamiliesByFamily(id)); return ResponseEntity.ok(subFamilyService.getSubFamiliesByFamilyName(familyIdOrName));

@GetMapping(value = "/search", produces = "application/json;charset=UTF-8")
public ResponseEntity<List<SubFamily>> searchSubFamilies(;
// Appelle: return ResponseEntity.ok(subFamilyService.getSubFamiliesByFamily(id));

@PutMapping("/{id}")
public ResponseEntity<SubFamily> updateSubFamily(@PathVariable @org.springframework.lang.NonNull Integer id, @RequestBody @org.springframework.lang.NonNull SubFamily subFamilyDetails);
// Appelle: return ResponseEntity.ok(subFamilyService.updateSubFamily(id, subFamilyDetails));

@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteSubFamily(@PathVariable @org.springframework.lang.NonNull Integer id);
// Appelle: subFamilyService.deleteSubFamily(id);

`

## SupplierController.java
`java
@RequestMapping("/api/suppliers")

@PostMapping
public ResponseEntity<Supplier> createSupplier(@RequestBody @org.springframework.lang.NonNull Supplier supplier);
// Appelle: return ResponseEntity.ok(supplierService.createSupplier(supplier));

@GetMapping
public ResponseEntity<List<Supplier>> getAllSuppliers();
// Appelle: return ResponseEntity.ok(supplierService.getAllSuppliers());

@GetMapping("/{id}")
public ResponseEntity<Supplier> getSupplierById(@PathVariable @org.springframework.lang.NonNull Integer id);
// Appelle: return ResponseEntity.ok(supplierService.getSupplierById(id));

@PutMapping("/{id}")
public ResponseEntity<Supplier> updateSupplier(@PathVariable @org.springframework.lang.NonNull Integer id, @RequestBody @org.springframework.lang.NonNull Supplier supplierDetails);
// Appelle: return ResponseEntity.ok(supplierService.updateSupplier(id, supplierDetails));

@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteSupplier(@PathVariable @org.springframework.lang.NonNull Integer id);
// Appelle: supplierService.deleteSupplier(id);

`

## UserController.java
`java
@RequestMapping("/api/users")

@PostMapping
public ResponseEntity<User> createUser(@RequestBody @org.springframework.lang.NonNull User user);
// Appelle: return ResponseEntity.ok(userService.createUser(user));

@GetMapping
public ResponseEntity<List<User>> getAllUsers();
// Appelle: return ResponseEntity.ok(userService.getAllUsers());

@GetMapping("/{id}")
public ResponseEntity<User> getUserById(@PathVariable @org.springframework.lang.NonNull Integer id);
// Appelle: return ResponseEntity.ok(userService.getUserById(id));

@GetMapping("/role/{role}")
public ResponseEntity<List<User>> getUsersByRole(@PathVariable @org.springframework.lang.NonNull Role role);
// Appelle: return ResponseEntity.ok(userService.getUsersByRole(role));

@PutMapping("/{id}")
public ResponseEntity<User> updateUser(@PathVariable @org.springframework.lang.NonNull Integer id, @RequestBody @org.springframework.lang.NonNull User userDetails);
// Appelle: return ResponseEntity.ok(userService.updateUser(id, userDetails));

@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable @org.springframework.lang.NonNull Integer id);
// Appelle: userService.deleteUser(id);

`

## WarehouseController.java
`java
@RequestMapping("/api/warehouse")

@GetMapping
public List<Warehouse> getAllWarehouses();

@GetMapping("/stock")
public List<StockItem> getStockItems();

`


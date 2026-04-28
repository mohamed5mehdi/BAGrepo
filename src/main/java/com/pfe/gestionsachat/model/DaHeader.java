package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "DaHeader")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class DaHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oid_da")
    @com.fasterxml.jackson.annotation.JsonProperty("oid_da")
    private Integer oidDa;

    @ManyToOne
    @JoinColumn(name = "id_demandeur")
    private User demandeur;

    @Column(name = "date_creation")
    private LocalDate dateCreation;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
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
        // Updated logic to match new states if needed
        this.statut = StatutDA.EN_ATTENTE_N1;
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
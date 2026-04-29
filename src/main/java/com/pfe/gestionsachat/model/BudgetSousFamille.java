package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "budget_sous_famille")
public class BudgetSousFamille {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String libelle;

    @ManyToOne
    @JoinColumn(name = "budget_famille_id")
    private BudgetFamille budgetFamille;

    private BigDecimal montantAlloue;

    private BigDecimal montantConsomme = BigDecimal.ZERO;

    private BigDecimal montantDisponible;

    public BudgetSousFamille() {}

    public BudgetSousFamille(String libelle, BigDecimal montantAlloue, BudgetFamille budgetFamille) {
        this.libelle = libelle;
        this.montantAlloue = montantAlloue;
        this.montantDisponible = montantAlloue;
        this.budgetFamille = budgetFamille;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public BudgetFamille getBudgetFamille() { return budgetFamille; }
    public void setBudgetFamille(BudgetFamille budgetFamille) { this.budgetFamille = budgetFamille; }

    public BigDecimal getMontantAlloue() { return montantAlloue; }
    public void setMontantAlloue(BigDecimal montantAlloue) { this.montantAlloue = montantAlloue; }

    public BigDecimal getMontantConsomme() { return montantConsomme; }
    public void setMontantConsomme(BigDecimal montantConsomme) { this.montantConsomme = montantConsomme; }

    public BigDecimal getMontantDisponible() { return montantDisponible; }
    public void setMontantDisponible(BigDecimal montantDisponible) { this.montantDisponible = montantDisponible; }
}

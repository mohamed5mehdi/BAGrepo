package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "budget_famille")
public class BudgetFamille {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String libelle;

    private BigDecimal montantAlloue;

    private BigDecimal montantConsomme = BigDecimal.ZERO;

    private BigDecimal montantDisponible;

    @OneToMany(mappedBy = "budgetFamille", cascade = CascadeType.ALL)
    private List<BudgetSousFamille> sousFamilles = new ArrayList<>();

    public BudgetFamille() {}

    public BudgetFamille(String libelle, BigDecimal montantAlloue) {
        this.libelle = libelle;
        this.montantAlloue = montantAlloue;
        this.montantDisponible = montantAlloue;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public BigDecimal getMontantAlloue() { return montantAlloue; }
    public void setMontantAlloue(BigDecimal montantAlloue) { this.montantAlloue = montantAlloue; }

    public BigDecimal getMontantConsomme() { return montantConsomme; }
    public void setMontantConsomme(BigDecimal montantConsomme) { this.montantConsomme = montantConsomme; }

    public BigDecimal getMontantDisponible() { return montantDisponible; }
    public void setMontantDisponible(BigDecimal montantDisponible) { this.montantDisponible = montantDisponible; }

    public List<BudgetSousFamille> getSousFamilles() { return sousFamilles; }
    public void setSousFamilles(List<BudgetSousFamille> sousFamilles) { this.sousFamilles = sousFamilles; }
}

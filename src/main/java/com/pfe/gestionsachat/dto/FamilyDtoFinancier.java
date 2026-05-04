package com.pfe.gestionsachat.dto;

import java.math.BigDecimal;

public class FamilyDtoFinancier {
    private Integer id;
    private String name;
    private BigDecimal budgetTotal;
    private BigDecimal budgetRestant;
    private BigDecimal budgetEngage;
    private BigDecimal budgetDisponible;

    public FamilyDtoFinancier(Integer id, String name, BigDecimal budgetTotal, BigDecimal budgetRestant, BigDecimal budgetEngage, BigDecimal budgetDisponible) {
        this.id = id;
        this.name = name;
        this.budgetTotal = budgetTotal;
        this.budgetRestant = budgetRestant;
        this.budgetEngage = budgetEngage;
        this.budgetDisponible = budgetDisponible;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getBudgetTotal() { return budgetTotal; }
    public void setBudgetTotal(BigDecimal budgetTotal) { this.budgetTotal = budgetTotal; }

    public BigDecimal getBudgetRestant() { return budgetRestant; }
    public void setBudgetRestant(BigDecimal budgetRestant) { this.budgetRestant = budgetRestant; }

    public BigDecimal getBudgetEngage() { return budgetEngage; }
    public void setBudgetEngage(BigDecimal budgetEngage) { this.budgetEngage = budgetEngage; }

    public BigDecimal getBudgetDisponible() { return budgetDisponible; }
    public void setBudgetDisponible(BigDecimal budgetDisponible) { this.budgetDisponible = budgetDisponible; }
}

package com.pfe.gestionsachat.dto;

public class FamilyDtoPublic {
    private Integer id;
    private String name;
    private java.math.BigDecimal budgetRestant;

    public FamilyDtoPublic(Integer id, String name, java.math.BigDecimal budgetRestant) {
        this.id = id;
        this.name = name;
        this.budgetRestant = budgetRestant;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public java.math.BigDecimal getBudget_restant() { return budgetRestant; }
    public void setBudget_restant(java.math.BigDecimal budgetRestant) { this.budgetRestant = budgetRestant; }
}

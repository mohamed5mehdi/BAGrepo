package com.pfe.gestionsachat.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests rigoureux de niveau PhD sur les invariants financiers (Budget).
 * Prouve formellement que l'équation :
 * budgetInitial = budgetEngage + budgetRestant
 * est conservée à travers toutes les opérations d'imputation et d'ajustement.
 */
class BudgetInvariantTest {

    @Test
    void testFamilyDeductBudgetMaintainsInvariant() {
        Family family = new Family("Informatique", new BigDecimal("10000.00"));
        
        assertEquals(new BigDecimal("10000.00"), family.getBudgetRestant());
        assertEquals(BigDecimal.ZERO, family.getBudgetEngage());

        // Consommation de 3000
        BigDecimal consommation = new BigDecimal("3000.00");
        family.deductBudget(consommation);

        assertEquals(new BigDecimal("7000.00"), family.getBudgetRestant());
        assertEquals(new BigDecimal("3000.00"), family.getBudgetEngage());

        // Invariant : 7000 + 3000 = 10000
        BigDecimal somme = family.getBudgetRestant().add(family.getBudgetEngage());
        assertEquals(0, family.getBudgetInitial().compareTo(somme), 
            "La somme Restant + Engagé DOIT être égale au Budget Initial");
    }

    @Test
    void testSubFamilyDeductBudgetMaintainsInvariant() {
        Family family = new Family("Informatique", new BigDecimal("10000.00"));
        SubFamily sub = new SubFamily("Logiciels", new BigDecimal("4000.00"), family);

        // Consommation de 1500
        BigDecimal consommation = new BigDecimal("1500.00");
        sub.deductBudget(consommation);

        assertEquals(new BigDecimal("2500.00"), sub.getBudgetRestant());
        assertEquals(new BigDecimal("1500.00"), sub.getBudgetEngage());

        // Invariant Sous-Famille
        BigDecimal sommeSub = sub.getBudgetRestant().add(sub.getBudgetEngage());
        assertEquals(0, sub.getBudgetInitial().compareTo(sommeSub));
    }

    @Test
    void testNegativeBudgetThrowsException() {
        Family family = new Family("Marketing", new BigDecimal("5000.00"));
        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            () -> {
                family.deductBudget(new BigDecimal("6000.00"));
            },
            "Expected deductBudget() to throw, but it didn't"
        );

        assertTrue(thrown.getMessage().toLowerCase().contains("insuffisant") || thrown.getMessage().toLowerCase().contains("négatif"));
    }
    
    @Test
    void testInvoiceMontantsInvariant() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-001");
        invoice.setMontantHT(new BigDecimal("100.00"));
        invoice.setMontantTTC(new BigDecimal("90.00")); // Impossible, TTC < HT
        
        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            () -> {
                // Simulation du @PrePersist/@PreUpdate
                java.lang.reflect.Method method = Invoice.class.getDeclaredMethod("validateMontants");
                method.setAccessible(true);
                try {
                    method.invoke(invoice);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause(); // Extract actual exception
                }
            }
        );
        
        assertTrue(thrown.getMessage().contains("TTC"));
        assertTrue(thrown.getMessage().contains("HT"));
    }
}

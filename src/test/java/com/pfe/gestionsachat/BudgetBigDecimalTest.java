package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.Family;
import com.pfe.gestionsachat.model.SubFamily;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BudgetBigDecimalTest {

    @Test
    void testBudgetCalculations() {
        Family family = new Family("IT", BigDecimal.valueOf(1000.00));
        SubFamily sf = new SubFamily("Hardware", BigDecimal.valueOf(500.00), family);
        
        // Precision test: 500.00 - 123.45 = 376.55
        BigDecimal purchaseAmount = BigDecimal.valueOf(123.45);
        assertTrue(sf.hasEnoughBudget(purchaseAmount));
        
        // Simulating the reduction logic that should be in the service
        BigDecimal newBudget = sf.getBudgetRestant().subtract(purchaseAmount);
        sf.setBudgetRestant(newBudget);
        
        assertEquals(0, BigDecimal.valueOf(376.55).compareTo(sf.getBudgetRestant()));
        
        // Test precision with many decimals
        BigDecimal smallAmount = new BigDecimal("0.00000001");
        BigDecimal before = sf.getBudgetRestant();
        sf.setBudgetRestant(before.subtract(smallAmount));
        assertNotEquals(0, before.compareTo(sf.getBudgetRestant()));
    }
}

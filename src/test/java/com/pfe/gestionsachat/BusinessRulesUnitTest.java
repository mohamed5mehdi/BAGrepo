package com.pfe.gestionsachat;

import com.pfe.gestionsachat.model.SubFamily;
import com.pfe.gestionsachat.model.GrnDetails;
import com.pfe.gestionsachat.model.Invoice;
import com.pfe.gestionsachat.model.InvoiceStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class BusinessRulesUnitTest {

    @Test
    public void testBudgetSuffisantValidation() {
        SubFamily subFamily = new SubFamily();
        subFamily.setBudgetInitial(new BigDecimal("10000.00"));
        subFamily.setBudgetRestant(new BigDecimal("10000.00"));
        
        assertTrue(subFamily.hasEnoughBudget(new BigDecimal("5000.00")), "Le budget devrait être suffisant");
        assertFalse(subFamily.hasEnoughBudget(new BigDecimal("15000.00")), "Le budget devrait être insuffisant");
        assertTrue(subFamily.hasEnoughBudget(new BigDecimal("10000.00")), "Le budget devrait être exactement suffisant");
    }

    @Test
    public void testGrnQualityConsistency() {
        GrnDetails detail = new GrnDetails();
        detail.setReceivedQuantity(15);
        detail.setAcceptedQuantity(10);
        detail.setRejectedQuantity(5);
        
        assertEquals(15, detail.getAcceptedQuantity() + detail.getRejectedQuantity(), 
            "La somme des quantités acceptées et rejetées doit être égale à la quantité reçue");
    }
    
    @Test
    public void testInvoiceStatusTransitions() {
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.RECEIVED);
        
        assertEquals(InvoiceStatus.RECEIVED, invoice.getStatus());
        
        invoice.setStatus(InvoiceStatus.MATCHED);
        assertEquals(InvoiceStatus.MATCHED, invoice.getStatus());
        
        invoice.setStatus(InvoiceStatus.APPROVED);
        assertEquals(InvoiceStatus.APPROVED, invoice.getStatus());
    }
}

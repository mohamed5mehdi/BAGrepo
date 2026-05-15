package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    java.util.List<Invoice> findByPurchaseOrder_IdPo(Integer idPo);

    /**
     * Guard unicité — évite les doublons de facture en cas de retry client.
     * Utilisé dans GrcService.generateInvoice() avant toute insertion.
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}

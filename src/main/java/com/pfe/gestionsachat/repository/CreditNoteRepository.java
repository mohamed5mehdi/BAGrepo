package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {

    /**
     * Vérification d'idempotence : évite la création de deux notes de crédit avec le même numéro.
     * Utilisé dans CreditNoteService.processCreditNote() avant toute opération financière.
     */
    boolean existsByCreditNoteNumber(String creditNoteNumber);
}

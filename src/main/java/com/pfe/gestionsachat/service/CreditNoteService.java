package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.CreditNote;
import com.pfe.gestionsachat.model.CreditNoteStatus;
import com.pfe.gestionsachat.repository.CreditNoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditNoteService {
    @Autowired
    private CreditNoteRepository creditNoteRepository;

    @Transactional
    public CreditNote createCreditNote(CreditNote creditNote) {
        creditNote.setStatus(CreditNoteStatus.PENDING);
        return creditNoteRepository.save(creditNote);
    }
}

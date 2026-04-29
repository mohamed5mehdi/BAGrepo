package com.pfe.gestionsachat.service;

import com.pfe.gestionsachat.model.BudgetFamille;
import com.pfe.gestionsachat.model.BudgetSousFamille;
import com.pfe.gestionsachat.repository.BudgetFamilleRepository;
import com.pfe.gestionsachat.repository.BudgetSousFamilleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class BudgetService {

    @Autowired
    private BudgetFamilleRepository budgetFamilleRepository;

    @Autowired
    private BudgetSousFamilleRepository budgetSousFamilleRepository;

    public boolean verifierBudgetSousFamille(Long sousFamilleId, BigDecimal montant) {
        BudgetSousFamille bsf = budgetSousFamilleRepository.findById(sousFamilleId).orElseThrow();
        return bsf.getMontantDisponible().compareTo(montant) >= 0;
    }

    @Transactional
    public void consommerBudget(Long sousFamilleId, BigDecimal montant) {
        BudgetSousFamille bsf = budgetSousFamilleRepository.findById(sousFamilleId).orElseThrow();
        BudgetFamille bf = bsf.getBudgetFamille();

        bsf.setMontantConsomme(bsf.getMontantConsomme().add(montant));
        bsf.setMontantDisponible(bsf.getMontantAlloue().subtract(bsf.getMontantConsomme()));
        budgetSousFamilleRepository.save(bsf);

        bf.setMontantConsomme(bf.getMontantConsomme().add(montant));
        bf.setMontantDisponible(bf.getMontantAlloue().subtract(bf.getMontantConsomme()));
        budgetFamilleRepository.save(bf);
    }
    
    @Transactional
    public void ajusterBudgetSousFamille(Long sousFamilleId, BigDecimal nouveauMontant) {
        BudgetSousFamille bsf = budgetSousFamilleRepository.findById(sousFamilleId).orElseThrow();
        bsf.setMontantAlloue(nouveauMontant);
        bsf.setMontantDisponible(bsf.getMontantAlloue().subtract(bsf.getMontantConsomme()));
        budgetSousFamilleRepository.save(bsf);
    }

    @Transactional
    public void ajusterBudgetFamille(Long familleId, BigDecimal nouveauMontant) {
        BudgetFamille bf = budgetFamilleRepository.findById(familleId).orElseThrow();
        bf.setMontantAlloue(nouveauMontant);
        bf.setMontantDisponible(bf.getMontantAlloue().subtract(bf.getMontantConsomme()));
        budgetFamilleRepository.save(bf);
    }
}

package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.BudgetSousFamille;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BudgetSousFamilleRepository extends JpaRepository<BudgetSousFamille, Long> {
}

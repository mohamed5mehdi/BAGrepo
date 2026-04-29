package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.BudgetFamille;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BudgetFamilleRepository extends JpaRepository<BudgetFamille, Long> {
}

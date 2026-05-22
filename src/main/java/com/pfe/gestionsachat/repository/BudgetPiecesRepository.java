package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.BudgetPieces;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface BudgetPiecesRepository extends JpaRepository<BudgetPieces, Integer> {

    Optional<BudgetPieces> findByExercice(String exercice);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BudgetPieces b WHERE b.exercice = :exercice")
    Optional<BudgetPieces> findByExerciceWithLock(@Param("exercice") String exercice);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BudgetPieces b WHERE b.id = :id")
    Optional<BudgetPieces> findByIdWithLock(@Param("id") Integer id);
}

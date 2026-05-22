package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.TransferLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferLineRepository extends JpaRepository<TransferLine, Long> {
    // Les opérations sur les lignes passent par le cascade ALL de TransferHeader.
    // Ce repository est disponible pour les requêtes directes si besoin (rapports, etc.).
}

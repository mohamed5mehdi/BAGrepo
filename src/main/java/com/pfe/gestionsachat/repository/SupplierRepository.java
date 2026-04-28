package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
    Optional<Supplier> findByContact(String contact);
    Optional<Supplier> findByNom(String nom);
}
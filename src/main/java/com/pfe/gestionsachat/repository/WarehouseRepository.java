package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    /**
     * Recherche deterministe par nom — remplace findAll().get(0) dans DataInitializer.
     * MAJEUR-03 : findAll() sans ORDER BY est non deterministe.
     */
    Optional<Warehouse> findByName(String name);
}

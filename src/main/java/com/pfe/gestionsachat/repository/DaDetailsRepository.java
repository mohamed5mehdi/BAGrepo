package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.DaDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DaDetailsRepository extends JpaRepository<DaDetails, Integer> {

    List<DaDetails> findByDaHeader_OidDa(Integer oidDa);

    List<DaDetails> findByQuantiteGreaterThan(Integer quantite);
}
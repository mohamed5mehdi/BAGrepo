package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.Justification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JustificationRepository extends JpaRepository<Justification, Integer> {
    List<Justification> findByDaHeader_OidDa(Integer oidDa);
}

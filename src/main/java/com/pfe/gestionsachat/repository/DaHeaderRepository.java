package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.DaHeader;
import com.pfe.gestionsachat.model.StatutDA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DaHeaderRepository extends JpaRepository<DaHeader, Integer> {

    List<DaHeader> findByStatut(StatutDA statut);

    @Query("SELECT d FROM DaHeader d WHERE d.demandeur.oidUser = :oidUser")
    List<DaHeader> findByDemandeur_OidUser(@Param("oidUser") Integer oidUser);

    @Query("SELECT d FROM DaHeader d WHERE d.statut = :statut AND d.demandeur.oidUser = :oidUser")
    List<DaHeader> findByStatutAndDemandeur_OidUser(@Param("statut") StatutDA statut, @Param("oidUser") Integer oidUser);

    @Query("SELECT d FROM DaHeader d WHERE d.statut = :statut ORDER BY d.dateCreation DESC")
    List<DaHeader> findPendingRequests(@Param("statut") StatutDA statut);

    long countByStatut(StatutDA statut);

    @Query("SELECT d FROM DaHeader d WHERE d.objet LIKE %:keyword%")
    List<DaHeader> searchByKeyword(@Param("keyword") String keyword);
}
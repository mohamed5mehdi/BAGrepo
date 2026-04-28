package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.SubFamily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubFamilyRepository extends JpaRepository<SubFamily, Integer> {

    Optional<SubFamily> findByLibelle(String libelle);

    @Query("SELECT s FROM SubFamily s WHERE s.family.idFamily = :familyId")
    List<SubFamily> findByFamilyId(@Param("familyId") Integer familyId);

    @Query("SELECT s FROM SubFamily s WHERE s.family.libelle = :libelle")
    List<SubFamily> findByFamilyLibelle(@Param("libelle") String libelle);
}
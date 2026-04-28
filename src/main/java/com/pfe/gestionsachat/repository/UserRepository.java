package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    Optional<User> findByNom(String nom);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByActif(Boolean actif);

    @Query("SELECT u FROM User u WHERE u.nom LIKE %:keyword% OR u.email LIKE %:keyword%")
    List<User> searchByKeyword(@Param("keyword") String keyword);
}
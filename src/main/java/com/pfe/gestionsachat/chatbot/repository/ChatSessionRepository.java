package com.pfe.gestionsachat.chatbot.repository;

import com.pfe.gestionsachat.chatbot.model.ChatSession;
import com.pfe.gestionsachat.chatbot.model.ChatSessionStatut;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    List<ChatSession> findByUserId(Integer userId);

    Optional<ChatSession> findFirstByUserIdAndStatutOrderByDateCreationDesc(Integer userId, ChatSessionStatut statut);

    /**
     * Verrou pessimiste en écriture — critique pour éviter les race conditions
     * lors des mises à jour concurrentes de slotsJson.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ChatSession s WHERE s.id = :id")
    Optional<ChatSession> findByIdWithLock(@Param("id") String id);

    List<ChatSession> findByStatutAndDateDernierMsgBefore(ChatSessionStatut statut, LocalDateTime date);
    
    @Modifying
    @Transactional
    @Query("UPDATE ChatSession c SET c.statut = :statut WHERE c.statut = com.pfe.gestionsachat.chatbot.model.ChatSessionStatut.ACTIVE AND c.dateDernierMsg < :limite")
    void abandonnerSessionsInactives(
        @Param("statut") ChatSessionStatut statut,
        @Param("limite") LocalDateTime limite
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM ChatSession c WHERE c.statut = :statut AND c.dateCreation < :date")
    void purgeSessionsPhysiquement(
        @Param("statut") ChatSessionStatut statut,
        @Param("date") LocalDateTime date
    );
}

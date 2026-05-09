package com.pfe.gestionsachat.chatbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "chat_session")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChatSession {

    private static final Logger log = LoggerFactory.getLogger(ChatSession.class);

    @Id
    private String id;

    @Column(nullable = false)
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatSessionStatut statut;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    private LocalDateTime dateDernierMsg;

    @Column(columnDefinition = "TEXT")
    private String slotsJson;

    /** ID de la DA créée après confirmation — nullable tant que non confirmée */
    private Long daCreeeId;

    // ─── Constructeur vide pour JPA ──────────────────────────────────────────
    public ChatSession() {
    }

    // ─── Constructeur métier ─────────────────────────────────────────────────
    public ChatSession(Integer userId) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.statut = ChatSessionStatut.ACTIVE;
        this.dateCreation = LocalDateTime.now();
        this.dateDernierMsg = LocalDateTime.now();
    }

    /** Garde-fou JPA : garantit des dates non-null avant toute persistance */
    @PrePersist
    @PreUpdate
    private void sanitizeDates() {
        if (this.dateCreation == null) {
            log.warn("[ChatSession] dateCreation null détecté sur session {}, correction auto", this.id);
            this.dateCreation = LocalDateTime.now();
        }
        if (this.dateDernierMsg == null) {
            log.warn("[ChatSession] dateDernierMsg null détecté sur session {}, correction auto", this.id);
            this.dateDernierMsg = LocalDateTime.now();
        }
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public ChatSessionStatut getStatut() { return statut; }
    public void setStatut(ChatSessionStatut statut) { this.statut = statut; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateDernierMsg() { return dateDernierMsg; }
    public void setDateDernierMsg(LocalDateTime dateDernierMsg) { this.dateDernierMsg = dateDernierMsg; }

    public String getSlotsJson() { return slotsJson; }
    public void setSlotsJson(String slotsJson) { this.slotsJson = slotsJson; }

    public Long getDaCreeeId() { return daCreeeId; }
    public void setDaCreeeId(Long daCreeeId) { this.daCreeeId = daCreeeId; }
}

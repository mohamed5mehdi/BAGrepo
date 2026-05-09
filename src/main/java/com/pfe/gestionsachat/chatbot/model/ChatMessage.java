package com.pfe.gestionsachat.chatbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Référence à la session par String — pas de @ManyToOne pour rester simple */
    @Column(nullable = false)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatMessageRole role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenu;

    private LocalDateTime dateEnvoi;

    /** Snapshot JSON des slots au moment de l'envoi — nullable, usage audit */
    @Column(columnDefinition = "TEXT")
    private String slotsSnapshot;

    // ─── Constructeur vide pour JPA ──────────────────────────────────────────
    public ChatMessage() {
    }

    // ─── Constructeur métier ─────────────────────────────────────────────────
    public ChatMessage(String sessionId, ChatMessageRole role, String contenu) {
        this.sessionId = sessionId;
        this.role = role;
        this.contenu = contenu;
        this.dateEnvoi = LocalDateTime.now();
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public ChatMessageRole getRole() { return role; }
    public void setRole(ChatMessageRole role) { this.role = role; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public String getSlotsSnapshot() { return slotsSnapshot; }
    public void setSlotsSnapshot(String slotsSnapshot) { this.slotsSnapshot = slotsSnapshot; }
}

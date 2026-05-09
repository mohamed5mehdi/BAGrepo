package com.pfe.gestionsachat.chatbot.dto;

import com.pfe.gestionsachat.chatbot.model.SlotState;

/**
 * DTO retourné par le ChatbotController après chaque interaction.
 * Encapsule l'état conversationnel courant sans exposer les détails JPA.
 */
public class ChatResponse {

    private String sessionId;
    private String botMessage;
    private SlotState slots;
    private boolean isComplet;
    private boolean isConfirmed;
    /** Nullable — renseigné uniquement après confirmerEtSoumettre() */
    private Long daCreeeId;

    // ─── Constructeur vide ────────────────────────────────────────────────────
    public ChatResponse() {}

    // ─── Constructeur métier ──────────────────────────────────────────────────
    public ChatResponse(String sessionId, String botMessage, SlotState slots,
                        boolean isComplet, boolean isConfirmed, Long daCreeeId) {
        this.sessionId = sessionId;
        this.botMessage = botMessage;
        this.slots = slots;
        this.isComplet = isComplet;
        this.isConfirmed = isConfirmed;
        this.daCreeeId = daCreeeId;
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getBotMessage() { return botMessage; }
    public void setBotMessage(String botMessage) { this.botMessage = botMessage; }

    public SlotState getSlots() { return slots; }
    public void setSlots(SlotState slots) { this.slots = slots; }

    public boolean isComplet() { return isComplet; }
    public void setComplet(boolean complet) { isComplet = complet; }

    public boolean isConfirmed() { return isConfirmed; }
    public void setConfirmed(boolean confirmed) { isConfirmed = confirmed; }

    public Long getDaCreeeId() { return daCreeeId; }
    public void setDaCreeeId(Long daCreeeId) { this.daCreeeId = daCreeeId; }
}

package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;

    private String entite;

    private Long entiteId;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id")
    private User utilisateur;

    private LocalDateTime dateAction;

    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String valeurAvant;

    @Column(columnDefinition = "TEXT")
    private String valeurApres;

    public AuditLog() {
        this.dateAction = LocalDateTime.now();
    }

    public AuditLog(String action, String entite, Long entiteId, User utilisateur, String valeurAvant, String valeurApres) {
        this();
        this.action = action;
        this.entite = entite;
        this.entiteId = entiteId;
        this.utilisateur = utilisateur;
        this.valeurAvant = valeurAvant;
        this.valeurApres = valeurApres;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntite() { return entite; }
    public void setEntite(String entite) { this.entite = entite; }

    public Long getEntiteId() { return entiteId; }
    public void setEntiteId(Long entiteId) { this.entiteId = entiteId; }

    public User getUtilisateur() { return utilisateur; }
    public void setUtilisateur(User utilisateur) { this.utilisateur = utilisateur; }

    public LocalDateTime getDateAction() { return dateAction; }
    public void setDateAction(LocalDateTime dateAction) { this.dateAction = dateAction; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getValeurAvant() { return valeurAvant; }
    public void setValeurAvant(String valeurAvant) { this.valeurAvant = valeurAvant; }

    public String getValeurApres() { return valeurApres; }
    public void setValeurApres(String valeurApres) { this.valeurApres = valeurApres; }
}

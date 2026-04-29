package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "status_history")
public class StatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entiteType;

    private Long entiteId;

    private String statutAvant;

    private String statutApres;

    @ManyToOne
    @JoinColumn(name = "modifie_par_id")
    private User modifiePar;

    private LocalDateTime dateModification;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    public StatusHistory() {
        this.dateModification = LocalDateTime.now();
    }

    public StatusHistory(String entiteType, Long entiteId, String statutAvant, String statutApres, User modifiePar, String commentaire) {
        this();
        this.entiteType = entiteType;
        this.entiteId = entiteId;
        this.statutAvant = statutAvant;
        this.statutApres = statutApres;
        this.modifiePar = modifiePar;
        this.commentaire = commentaire;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEntiteType() { return entiteType; }
    public void setEntiteType(String entiteType) { this.entiteType = entiteType; }

    public Long getEntiteId() { return entiteId; }
    public void setEntiteId(Long entiteId) { this.entiteId = entiteId; }

    public String getStatutAvant() { return statutAvant; }
    public void setStatutAvant(String statutAvant) { this.statutAvant = statutAvant; }

    public String getStatutApres() { return statutApres; }
    public void setStatutApres(String statutApres) { this.statutApres = statutApres; }

    public User getModifiePar() { return modifiePar; }
    public void setModifiePar(User modifiePar) { this.modifiePar = modifiePar; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
}

package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "justification")
public class Justification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_justif")
    private Integer idJustif;

    @ManyToOne
    @JoinColumn(name = "id_da")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private DaHeader daHeader;

    @Column(columnDefinition = "TEXT")
    private String texte;

    @Column(name = "date_justif")
    private LocalDate dateJustif;

    public Justification() {
        this.dateJustif = LocalDate.now();
    }

    public Justification(DaHeader daHeader, String texte) {
        this();
        this.daHeader = daHeader;
        this.texte = texte;
    }

    // Getters
    public Integer getIdJustif() { return idJustif; }
    public DaHeader getDaHeader() { return daHeader; }
    public String getTexte() { return texte; }
    public LocalDate getDateJustif() { return dateJustif; }

    // Setters
    public void setIdJustif(Integer idJustif) { this.idJustif = idJustif; }
    public void setDaHeader(DaHeader daHeader) { this.daHeader = daHeader; }
    public void setTexte(String texte) { this.texte = texte; }
    public void setDateJustif(LocalDate dateJustif) { this.dateJustif = dateJustif; }
}

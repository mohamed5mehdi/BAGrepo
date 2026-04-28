package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "supplier")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oid_supplier")
    private Integer oidSupplier;

    private String nom;
    private String contact;
    private String adresse;

    @OneToMany(mappedBy = "fournisseur")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<DaDetails> details = new ArrayList<>();

    public Supplier() {}

    public Supplier(String nom, String contact, String adresse) {
        this.nom = nom;
        this.contact = contact;
        this.adresse = adresse;
    }

    // Getters
    public Integer getOidSupplier() { return oidSupplier; }
    public String getNom() { return nom; }
    public String getContact() { return contact; }
    public String getAdresse() { return adresse; }
    public List<DaDetails> getDetails() { return details; }

    // Setters
    public void setOidSupplier(Integer oidSupplier) { this.oidSupplier = oidSupplier; }
    public void setNom(String nom) { this.nom = nom; }
    public void setContact(String contact) { this.contact = contact; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public void setDetails(List<DaDetails> details) { this.details = details; }
}
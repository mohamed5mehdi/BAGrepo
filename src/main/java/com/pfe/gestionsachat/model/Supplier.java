package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "supplier")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oid_supplier")
    private Integer oidSupplier;

    private String nom;
    private String contact;
    private String email;
    private String phone;
    private String adresse;
    private String sector;
    private Integer rating; // 1-5
    private Integer averageLeadTime; // in days
    private Boolean isCertified;
    private String ice;

    @OneToMany(mappedBy = "fournisseur")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<DaDetails> details = new ArrayList<>();

    public Supplier() {}

    public Supplier(Integer oidSupplier) {
        this.oidSupplier = oidSupplier;
    }

    public Supplier(String nom, String contact, String adresse, String sector, Integer rating, Integer leadTime) {
        this.nom = nom;
        this.contact = contact;
        this.adresse = adresse;
        this.sector = sector;
        this.rating = rating;
        this.averageLeadTime = leadTime;
        this.isCertified = false;
    }

    // Getters
    public Integer getOidSupplier() { return oidSupplier; }
    public String getNom() { return nom; }
    public String getContact() { return contact; }
    public String getAdresse() { return adresse; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getSector() { return sector; }
    public Integer getRating() { return rating; }
    public Integer getAverageLeadTime() { return averageLeadTime; }
    public Boolean getIsCertified() { return isCertified; }
    public String getIce() { return ice; }
    public List<DaDetails> getDetails() { return details; }

    // Setters
    public void setOidSupplier(Integer oidSupplier) { this.oidSupplier = oidSupplier; }
    public void setNom(String nom) { this.nom = nom; }
    public void setContact(String contact) { this.contact = contact; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setSector(String sector) { this.sector = sector; }
    public void setRating(Integer rating) { this.rating = rating; }
    public void setAverageLeadTime(Integer averageLeadTime) { this.averageLeadTime = averageLeadTime; }
    public void setIsCertified(Boolean isCertified) { this.isCertified = isCertified; }
    public void setIce(String ice) { this.ice = ice; }
    public void setDetails(List<DaDetails> details) { this.details = details; }
}
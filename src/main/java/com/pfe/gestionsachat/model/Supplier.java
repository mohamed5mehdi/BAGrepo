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



    /**
     * BUG-19 FIX : Validation de la plage du rating.
     * Le rating doit toujours être entre 1 et 5 s'il est renseigné.
     */
    @PrePersist
    @PreUpdate
    private void validateRating() {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalStateException("Supplier [" + nom + "] : le rating doit être compris entre 1 et 5.");
        }
    }

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

}
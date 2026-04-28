package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oid_user")
    @com.fasterxml.jackson.annotation.JsonProperty("oid_user")
    private Integer oidUser;

    private String nom;
    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String service;

    @ManyToOne
    @JoinColumn(name = "n1_id")
    @com.fasterxml.jackson.annotation.JsonProperty("n1")
    private User n1;

    private Boolean actif;

    @OneToMany(mappedBy = "user")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Action> actions = new ArrayList<>();

    public User() {}

    public User(String nom, String email, String password, Role role) {
        this.nom = nom;
        this.email = email;
        this.password = password;
        this.role = role;
        this.actif = true;
    }

    // Getters
    public Integer getOidUser() { return oidUser; }
    public String getNom() { return nom; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public String getService() { return service; }
    public User getN1() { return n1; }
    public Boolean getActif() { return actif; }
    public List<Action> getActions() { return actions; }

    @com.fasterxml.jackson.annotation.JsonProperty("n1_id")
    public Integer getN1Id() {
        return n1 != null ? n1.getOidUser() : null;
    }

    // Setters
    public void setOidUser(Integer oidUser) { this.oidUser = oidUser; }
    public void setNom(String nom) { this.nom = nom; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(Role role) { this.role = role; }
    public void setService(String service) { this.service = service; }
    public void setN1(User n1) { this.n1 = n1; }
    public void setActif(Boolean actif) { this.actif = actif; }
    public void setActions(List<Action> actions) { this.actions = actions; }
}
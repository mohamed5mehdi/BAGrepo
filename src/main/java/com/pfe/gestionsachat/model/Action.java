package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "action")
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oid_action")
    private Integer oidAction;

    @ManyToOne
    @JoinColumn(name = "oid_user")
    private User user;

    @ManyToOne
    @JoinColumn(name = "id_da")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private DaHeader daHeader;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_action")
    private TypeAction typeAction;

    @Column(name = "date_action")
    private LocalDate dateAction;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    public Action() {
        this.dateAction = LocalDate.now();
    }

    public Action(User user, DaHeader daHeader, TypeAction typeAction) {
        this();
        this.user = user;
        this.daHeader = daHeader;
        this.typeAction = typeAction;
    }

    // Getters
    public Integer getOidAction() { return oidAction; }
    public User getUser() { return user; }
    public DaHeader getDaHeader() { return daHeader; }
    public TypeAction getTypeAction() { return typeAction; }
    public LocalDate getDateAction() { return dateAction; }
    public String getMetadata() { return metadata; }

    // Setters
    public void setOidAction(Integer oidAction) { this.oidAction = oidAction; }
    public void setUser(User user) { this.user = user; }
    public void setDaHeader(DaHeader daHeader) { this.daHeader = daHeader; }
    public void setTypeAction(TypeAction typeAction) { this.typeAction = typeAction; }
    public void setDateAction(LocalDate dateAction) { this.dateAction = dateAction; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}

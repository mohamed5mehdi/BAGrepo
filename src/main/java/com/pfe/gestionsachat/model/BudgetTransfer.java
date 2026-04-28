package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "budget_transfer")
public class BudgetTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transfert")
    private Integer idTransfert;

    @ManyToOne
    @JoinColumn(name = "id_da")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private DaHeader daHeader;

    @ManyToOne
    @JoinColumn(name = "id_sub_source")
    private SubFamily subSource;

    @ManyToOne
    @JoinColumn(name = "id_sub_cible")
    private SubFamily subCible;

    private BigDecimal montant;

    @Column(name = "date_transfert")
    private LocalDate dateTransfert;

    @ManyToOne
    @JoinColumn(name = "id_daf")
    private User daf;

    public BudgetTransfer() {
        this.dateTransfert = LocalDate.now();
    }

    public BudgetTransfer(DaHeader daHeader, SubFamily subSource, SubFamily subCible,
                          BigDecimal montant, User daf) {
        this();
        this.daHeader = daHeader;
        this.subSource = subSource;
        this.subCible = subCible;
        this.montant = montant;
        this.daf = daf;
    }

    // Getters
    public Integer getIdTransfert() { return idTransfert; }
    public DaHeader getDaHeader() { return daHeader; }
    public SubFamily getSubSource() { return subSource; }
    public SubFamily getSubCible() { return subCible; }
    public BigDecimal getMontant() { return montant; }
    public LocalDate getDateTransfert() { return dateTransfert; }
    public User getDaf() { return daf; }

    // Setters
    public void setIdTransfert(Integer idTransfert) { this.idTransfert = idTransfert; }
    public void setDaHeader(DaHeader daHeader) { this.daHeader = daHeader; }
    public void setSubSource(SubFamily subSource) { this.subSource = subSource; }
    public void setSubCible(SubFamily subCible) { this.subCible = subCible; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public void setDateTransfert(LocalDate dateTransfert) { this.dateTransfert = dateTransfert; }
    public void setDaf(User daf) { this.daf = daf; }
}

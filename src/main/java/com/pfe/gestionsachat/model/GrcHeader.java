package com.pfe.gestionsachat.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "grc_header")
public class GrcHeader {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "grn_header_id")
    private GrnHeader grnHeader;

    private LocalDate costingDate = LocalDate.now();

    @ManyToOne
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Enumerated(EnumType.STRING)
    private GrcStatus status;

    private java.math.BigDecimal totalAmount;
    private String devise;

    @OneToMany(mappedBy = "grcHeader", cascade = CascadeType.ALL)
    private List<GrcDetails> details;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public GrnHeader getGrnHeader() { return grnHeader; }
    public void setGrnHeader(GrnHeader grnHeader) { this.grnHeader = grnHeader; }
    public LocalDate getCostingDate() { return costingDate; }
    public void setCostingDate(LocalDate costingDate) { this.costingDate = costingDate; }
    public User getProcessedBy() { return processedBy; }
    public void setProcessedBy(User processedBy) { this.processedBy = processedBy; }
    public GrcStatus getStatus() { return status; }
    public void setStatus(GrcStatus status) { this.status = status; }
    public java.math.BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(java.math.BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getDevise() { return devise; }
    public void setDevise(String devise) { this.devise = devise; }
    public List<GrcDetails> getDetails() { return details; }
    public void setDetails(List<GrcDetails> details) { this.details = details; }
}

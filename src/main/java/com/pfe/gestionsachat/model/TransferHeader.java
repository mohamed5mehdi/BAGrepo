package com.pfe.gestionsachat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * TransferHeader — Entête du flux LTO/LTI (Transfert Inter-Sites).
 *
 * Machine à états :
 *   PENDING → IN_TRANSIT (shipTransfer par MAGASINIER source)
 *   IN_TRANSIT → RECEIVED (receiveTransfer par MAGASINIER_DEST)
 *   PENDING → CANCELLED (cancelTransfer par EMPLOYE auteur ou ADMINISTRATEUR)
 */
@Entity
@Table(name = "transfer_header")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TransferHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numéro LTO (Local Transfer Order) — généré à l'expédition (IN_TRANSIT). */
    private String ltoNumber;

    /** Numéro LTI (Local Transfer Intake) — généré à la réception (RECEIVED). */
    private String ltiNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "actions"})
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_source_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Warehouse warehouseSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_dest_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Warehouse warehouseDest;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** Horodatage de l'expédition (passage IN_TRANSIT). Null avant expédition. */
    private LocalDateTime shippedAt;

    /** Horodatage de la réception (passage RECEIVED). Null avant réception. */
    private LocalDateTime receivedAt;

    @OneToMany(mappedBy = "header", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<TransferLine> lines = new ArrayList<>();

    /**
     * BUG-20 FIX : Validation de la cohérence entre le statut et les timestamps.
     * Un transfert expédié DOIT avoir une date d'expédition.
     * Un transfert reçu DOIT avoir une date de réception.
     */
    @PrePersist
    @PreUpdate
    private void validateTimestamps() {
        if (status == TransferStatus.IN_TRANSIT && shippedAt == null) {
            throw new IllegalStateException("TransferHeader [" + id + "] : statut IN_TRANSIT mais shippedAt est null.");
        }
        if (status == TransferStatus.RECEIVED && receivedAt == null) {
            throw new IllegalStateException("TransferHeader [" + id + "] : statut RECEIVED mais receivedAt est null.");
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public String getLtoNumber() { return ltoNumber; }
    public String getLtiNumber() { return ltiNumber; }
    public TransferStatus getStatus() { return status; }
    public User getRequestedBy() { return requestedBy; }
    public Warehouse getWarehouseSource() { return warehouseSource; }
    public Warehouse getWarehouseDest() { return warehouseDest; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public List<TransferLine> getLines() { return lines; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setId(Long id) { this.id = id; }
    public void setLtoNumber(String ltoNumber) { this.ltoNumber = ltoNumber; }
    public void setLtiNumber(String ltiNumber) { this.ltiNumber = ltiNumber; }
    public void setStatus(TransferStatus status) { this.status = status; }
    public void setRequestedBy(User requestedBy) { this.requestedBy = requestedBy; }
    public void setWarehouseSource(Warehouse warehouseSource) { this.warehouseSource = warehouseSource; }
    public void setWarehouseDest(Warehouse warehouseDest) { this.warehouseDest = warehouseDest; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public void setLines(List<TransferLine> lines) { this.lines = lines; }
}

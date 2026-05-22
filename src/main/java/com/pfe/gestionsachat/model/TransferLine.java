package com.pfe.gestionsachat.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

/**
 * TransferLine — Ligne d'un TransferHeader.
 *
 * Chaque ligne référence un StockItem source et la quantité demandée.
 * warehouseSourceSnapshotId est un snapshot immuable du warehouse source
 * au moment de la soumission (RISQUE-27 : évite la perte de traçabilité
 * si le StockItem change de warehouse par admin).
 */
@Entity
@Table(name = "transfer_line")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TransferLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "header_id", nullable = false)
    @JsonBackReference
    private TransferHeader header;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stock_item_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private StockItem stockItem;

    @Column(nullable = false)
    private Integer quantityRequested;

    /**
     * RISQUE-27 : Snapshot de l'id du warehouse source au moment de la soumission.
     * Immuable une fois persisté.
     */
    @Column(name = "warehouse_source_snapshot_id")
    private Long warehouseSourceSnapshotId;

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public TransferHeader getHeader() { return header; }
    public StockItem getStockItem() { return stockItem; }
    public Integer getQuantityRequested() { return quantityRequested; }
    public Long getWarehouseSourceSnapshotId() { return warehouseSourceSnapshotId; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setId(Long id) { this.id = id; }
    public void setHeader(TransferHeader header) { this.header = header; }
    public void setStockItem(StockItem stockItem) { this.stockItem = stockItem; }
    public void setQuantityRequested(Integer quantityRequested) { this.quantityRequested = quantityRequested; }
    public void setWarehouseSourceSnapshotId(Long warehouseSourceSnapshotId) { this.warehouseSourceSnapshotId = warehouseSourceSnapshotId; }
}

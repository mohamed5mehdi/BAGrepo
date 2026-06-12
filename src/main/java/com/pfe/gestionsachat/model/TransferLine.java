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

    /**
     * BUG-16 FIX : FetchType.LAZY au lieu de EAGER.
     *
     * Avant : EAGER chargeait StockItem dans la transaction de lecture de TransferLine.
     * Le @Version de StockItem n'était alors pas actif pour la protection concurrente,
     * car la modification du stock survenait dans une transaction différente (service layer).
     * Deux transferts concurrents pouvaient tous les deux charger le même StockItem (version N),
     * puis écraser mutuellement leurs modifications → stock négatif possible.
     *
     * Après : LAZY force chaque transaction de modification à recharger StockItem dans son propre
     * contexte de persistance — le @Version est actif, ObjectOptimisticLockingFailureException
     * est levée si une écriture concurrente est détectée.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_item_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private StockItem stockItem;


    @Column(nullable = false)
    private Integer quantityRequested;

    @Column(name = "quantity_shipped")
    private Integer quantityShipped;

    @Column(name = "quantity_received")
    private Integer quantityReceived;

    /**
     * RISQUE-27 : Snapshot de l'id du warehouse source au moment de la soumission.
     * Immuable une fois persisté.
     */
    @Column(name = "warehouse_source_snapshot_id")
    private Long warehouseSourceSnapshotId;

    /**
     * BUG-FORENSIQUE-05 : Validation de positivité des quantités.
     * Des quantités négatives dans un transfert fausseraient les stocks (un transfert reçu négatif
     * réduirait le stock cible au lieu de l'augmenter).
     */
    @PrePersist
    @PreUpdate
    private void validateQuantities() {
        if (quantityRequested != null && quantityRequested <= 0) {
            throw new IllegalStateException("TransferLine : quantityRequested (" + quantityRequested + ") doit être strictement positive.");
        }
        if (quantityShipped != null && quantityShipped < 0) {
            throw new IllegalStateException("TransferLine : quantityShipped (" + quantityShipped + ") ne peut pas être négatif.");
        }
        if (quantityReceived != null && quantityReceived < 0) {
            throw new IllegalStateException("TransferLine : quantityReceived (" + quantityReceived + ") ne peut pas être négatif.");
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public TransferHeader getHeader() { return header; }
    public StockItem getStockItem() { return stockItem; }
    public Integer getQuantityRequested() { return quantityRequested; }
    public Integer getQuantityShipped() { return quantityShipped; }
    public Integer getQuantityReceived() { return quantityReceived; }
    public Long getWarehouseSourceSnapshotId() { return warehouseSourceSnapshotId; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setId(Long id) { this.id = id; }
    public void setHeader(TransferHeader header) { this.header = header; }
    public void setStockItem(StockItem stockItem) { this.stockItem = stockItem; }
    public void setQuantityRequested(Integer quantityRequested) { this.quantityRequested = quantityRequested; }
    public void setQuantityShipped(Integer quantityShipped) { this.quantityShipped = quantityShipped; }
    public void setQuantityReceived(Integer quantityReceived) { this.quantityReceived = quantityReceived; }
    public void setWarehouseSourceSnapshotId(Long warehouseSourceSnapshotId) { this.warehouseSourceSnapshotId = warehouseSourceSnapshotId; }
}

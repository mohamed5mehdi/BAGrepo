package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.TransferHeader;
import com.pfe.gestionsachat.model.TransferStatus;
import com.pfe.gestionsachat.model.User;
import com.pfe.gestionsachat.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransferHeaderRepository extends JpaRepository<TransferHeader, Long> {

    /** Transferts soumis par un employé donné (vue DemandeurPage). */
    List<TransferHeader> findByRequestedBy(User user);

    /**
     * File d'attente du MAGASINIER source — transferts PENDING à expédier depuis son warehouse.
     * RISQUE-14 : si user.warehouse == null, la query retourne 0 résultats silencieusement.
     * Guard explicite dans TransferService.getSourceQueue().
     */
    List<TransferHeader> findByWarehouseSourceAndStatus(Warehouse src, TransferStatus status);

    /** Historique d'expédition pour le magasinier source (IN_TRANSIT, RECEIVED). */
    List<TransferHeader> findByWarehouseSourceAndStatusIn(Warehouse src, List<TransferStatus> statuses);

    /**
     * File d'attente du MAGASINIER_DEST — transferts IN_TRANSIT à réceptionner vers son warehouse.
     * RISQUE-14 : même guard requis dans TransferService.getDestQueue().
     */
    List<TransferHeader> findByWarehouseDestAndStatus(Warehouse dest, TransferStatus status);

    /**
     * RISQUE-08 : alerte admin sur les transferts bloqués en transit.
     * Utilisé par un endpoint futur GET /api/transfers?status=IN_TRANSIT&olderThanHours=48.
     */
    List<TransferHeader> findByStatusAndCreatedAtBefore(TransferStatus status, LocalDateTime before);

    /**
     * Tous les transferts impliquant un warehouse donné (source ou destination).
     * Utile pour l'historique complet d'un entrepôt.
     */
    @Query("SELECT h FROM TransferHeader h WHERE h.warehouseSource = :wh OR h.warehouseDest = :wh ORDER BY h.createdAt DESC")
    List<TransferHeader> findAllByWarehouse(@Param("wh") Warehouse warehouse);

    /**
     * MAJEUR-01 : charge le header + ses lignes + les stockItems en une seule requête.
     * Élimine les N+1 queries du chargement LAZY de lines/stockItems.
     * Rend le tri anti-deadlock par stockItem.id déterministe (ids chargés, pas proxies).
     * Utilisé dans shipTransfer et receiveTransfer.
     */
    @Query("SELECT h FROM TransferHeader h JOIN FETCH h.lines l JOIN FETCH l.stockItem WHERE h.id = :id")
    Optional<TransferHeader> findByIdWithLinesAndItems(@Param("id") Long id);
}

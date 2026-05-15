package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.POStatus;
import com.pfe.gestionsachat.model.PurchaseOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {

    PurchaseOrder findByDaHeader_OidDa(Integer oidDa);

    List<PurchaseOrder> findByStatut(POStatus statut);

    @Query("SELECT p FROM PurchaseOrder p WHERE p.dateCreation BETWEEN :startDate AND :endDate")
    List<PurchaseOrder> findByDateCreationBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT p FROM PurchaseOrder p WHERE p.montantTotal > :amount")
    List<PurchaseOrder> findByMontantTotalGreaterThan(@Param("amount") Double amount);

    /**
     * POs APPROVED sans aucun GRN créé depuis plus de X jours — Ghost POs.
     * FIX #7 : paramètre enum typé — résistant aux renommages futurs.
     */
    @Query("SELECT p FROM PurchaseOrder p WHERE p.statut = :statut AND p.dateCreation < :dateLimite " +
           "AND NOT EXISTS (SELECT g FROM GrnHeader g WHERE g.purchaseOrder = p)")
    List<PurchaseOrder> findGhostPurchaseOrders(
            @Param("statut") POStatus statut,
            @Param("dateLimite") LocalDate dateLimite);

    /**
     * Verrou pessimiste pour éviter les transitions de statut concurrentes (approbation/rejet simultanés).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PurchaseOrder p WHERE p.idPo = :id")
    Optional<PurchaseOrder> findByIdWithLock(@Param("id") Integer id);
}
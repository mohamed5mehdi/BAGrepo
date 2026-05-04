package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {

    PurchaseOrder findByDaHeader_OidDa(Integer oidDa);

    List<PurchaseOrder> findByStatut(String statut);

    @Query("SELECT p FROM PurchaseOrder p WHERE p.dateCreation BETWEEN :startDate AND :endDate")
    List<PurchaseOrder> findByDateCreationBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT p FROM PurchaseOrder p WHERE p.montantTotal > :amount")
    List<PurchaseOrder> findByMontantTotalGreaterThan(@Param("amount") Double amount);
    @Query("SELECT p FROM PurchaseOrder p WHERE p.statut = 'VALIDE' AND p.dateCreation < :dateLimite " +
           "AND NOT EXISTS (SELECT g FROM GrnHeader g WHERE g.purchaseOrder = p)")
    List<PurchaseOrder> findGhostPurchaseOrders(@Param("dateLimite") LocalDate dateLimite);
}
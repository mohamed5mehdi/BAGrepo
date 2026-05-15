package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.GrnHeader;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrnHeaderRepository extends JpaRepository<GrnHeader, Long> {

    List<GrnHeader> findByPurchaseOrder_IdPo(Integer idPo);

    /**
     * Somme des quantités déjà reçues pour un PO + un article donné.
     * Sans verrou — utilisé uniquement en lecture hors-transaction critique.
     */
    @Query("SELECT COALESCE(SUM(d.receivedQuantity), 0) FROM GrnHeader g JOIN g.details d " +
           "WHERE g.purchaseOrder.idPo = :idPo AND d.itemCode = :itemCode")
    Integer sumReceivedQuantityByPoIdAndItemCode(
            @Param("idPo") Integer idPo,
            @Param("itemCode") String itemCode);

    /**
     * Verrou pessimiste (PESSIMISTIC_WRITE) sur les GRNs du PO.
     * Utilisé dans createGrn pour prévenir la race condition
     * de sur-réception concurrente sur le même PO.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GrnHeader g WHERE g.purchaseOrder.idPo = :idPo")
    List<GrnHeader> findByPurchaseOrderIdWithLock(@Param("idPo") Integer idPo);

    /**
     * Somme TOUTES les quantités reçues pour un PO, tous articles confondus.
     * Utilisé exclusivement pour les POs DemandeInterne (mono-article) où
     * le Magasinier peut assigner un itemCode arbitraire — la validation se fait
     * sur le total global vs demandeInterne.quantite.
     */
    @Query("SELECT COALESCE(SUM(d.receivedQuantity), 0) FROM GrnHeader g JOIN g.details d " +
           "WHERE g.purchaseOrder.idPo = :idPo")
    Integer sumAllReceivedByPoId(@Param("idPo") Integer idPo);

    List<GrnHeader> findByStatus(com.pfe.gestionsachat.model.GrnStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GrnHeader g WHERE g.id = :id")
    java.util.Optional<GrnHeader> findByIdWithLock(@Param("id") Long id);
}

package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.GrnHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrnHeaderRepository extends JpaRepository<GrnHeader, Long> {
    List<GrnHeader> findByPurchaseOrder_IdPo(Integer idPo);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(d.receivedQuantity), 0) FROM GrnHeader g JOIN g.details d WHERE g.purchaseOrder.idPo = :idPo AND d.itemCode = :itemCode")
    Integer sumReceivedQuantityByPoIdAndItemCode(@org.springframework.data.repository.query.Param("idPo") Integer idPo, @org.springframework.data.repository.query.Param("itemCode") String itemCode);
}

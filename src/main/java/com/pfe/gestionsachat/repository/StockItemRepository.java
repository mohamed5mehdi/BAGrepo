package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.StockItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockItemRepository extends JpaRepository<StockItem, Long> {

    List<StockItem> findByItemCode(String itemCode);

    List<StockItem> findByItemNameIgnoreCase(String itemName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StockItem s WHERE s.itemCode = :itemCode")
    List<StockItem> findByItemCodeWithLock(@Param("itemCode") String itemCode);

    /**
     * Recherche unique par itemCode et warehouse — reflète la contrainte @UniqueConstraint.
     * Utilisé dans addStock pour garantir l'unicité 1 article = 1 emplacement par entrepôt.
     */
    Optional<StockItem> findByItemCodeAndWarehouse_Id(String itemCode, Long warehouseId);

    /**
     * Verrou pessimiste pour addStock — élimine la race condition de mise à jour
     * concurrente du même article dans le même entrepôt.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StockItem s WHERE s.itemCode = :itemCode AND s.warehouse.id = :warehouseId")
    Optional<StockItem> findByItemCodeAndWarehouseIdWithLock(
            @Param("itemCode") String itemCode,
            @Param("warehouseId") Long warehouseId);
    /**
     * RISQUE-13 : JOIN FETCH obligatoire sur warehouse pour éviter LazyInitializationException
     * quand le controller accède à stockItem.warehouse.name hors contexte de transaction.
     * Retourne tous les articles avec stock > 0, triés par warehouse puis nom article.
     */
    @Query("SELECT s FROM StockItem s JOIN FETCH s.warehouse WHERE s.quantityAvailable > 0 ORDER BY s.warehouse.name, s.itemName")
    List<StockItem> findAvailableStock();
}

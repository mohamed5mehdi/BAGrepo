package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StockItemRepository extends JpaRepository<StockItem, Long> {
    List<StockItem> findByItemCode(String itemCode);
    List<StockItem> findByItemNameIgnoreCase(String itemName);
}

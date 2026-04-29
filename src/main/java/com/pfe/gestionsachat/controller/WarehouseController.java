package com.pfe.gestionsachat.controller;

import com.pfe.gestionsachat.model.StockItem;
import com.pfe.gestionsachat.model.Warehouse;
import com.pfe.gestionsachat.repository.StockItemRepository;
import com.pfe.gestionsachat.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
@CrossOrigin(origins = "*")
public class WarehouseController {

    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private StockItemRepository stockItemRepository;

    @GetMapping
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    @GetMapping("/stock")
    public List<StockItem> getStockItems() {
        return stockItemRepository.findAll();
    }
}

package com.example.inventory.application.port.out;

import com.example.inventory.domain.entity.Inventory;
import com.example.inventory.domain.valueobject.ProductId;
import com.example.inventory.domain.valueobject.InventoryId;

import java.util.Optional;
import java.util.List;

/**
 * Output port for inventory persistence operations
 * 
 * This interface defines the contract for inventory data access
 * following the Hexagonal Architecture principles.
 */
public interface InventoryRepository {

    /**
     * Save an inventory entity
     * 
     * @param inventory the inventory to save
     * @return the saved inventory
     */
    Inventory save(Inventory inventory);

    /**
     * Find inventory by ID
     * 
     * @param id the inventory ID
     * @return Optional containing the inventory if found
     */
    Optional<Inventory> findById(InventoryId id);

    /**
     * Find inventory by product ID
     * 
     * @param productId the product ID
     * @return Optional containing the inventory if found
     */
    Optional<Inventory> findByProductId(ProductId productId);

    /**
     * Find all inventories
     * 
     * @return list of all inventories
     */
    List<Inventory> findAll();

    /**
     * Find inventories with low stock (below minimum threshold)
     * 
     * @return list of inventories with low stock
     */
    List<Inventory> findLowStockInventories();

    /**
     * Delete inventory by ID
     * 
     * @param id the inventory ID
     */
    void deleteById(InventoryId id);

    /**
     * Check if inventory exists for product
     * 
     * @param productId the product ID
     * @return true if inventory exists
     */
    boolean existsByProductId(ProductId productId);
}
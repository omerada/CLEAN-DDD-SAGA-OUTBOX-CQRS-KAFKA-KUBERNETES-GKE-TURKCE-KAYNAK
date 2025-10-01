package com.example.inventory.infrastructure.persistence;

import com.example.inventory.application.port.out.InventoryRepository;
import com.example.inventory.domain.entity.Inventory;
import com.example.inventory.domain.valueobject.InventoryId;
import com.example.inventory.domain.valueobject.ProductId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository implementation for Inventory
 * 
 * This interface extends JpaRepository and implements the InventoryRepository
 * port
 * providing data access operations for the Inventory aggregate.
 */
@Repository
public interface JpaInventoryRepository extends JpaRepository<Inventory, String>, InventoryRepository {

    @Override
    default Optional<Inventory> findById(InventoryId id) {
        return findById(id.getValue());
    }

    @Override
    default Optional<Inventory> findByProductId(ProductId productId) {
        return findByProductIdValue(productId.getValue());
    }

    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdValue(@Param("productId") String productId);

    @Override
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= i.reorderPoint")
    List<Inventory> findLowStockInventories();

    @Override
    default void deleteById(InventoryId id) {
        deleteById(id.getValue());
    }

    @Override
    default boolean existsByProductId(ProductId productId) {
        return existsByProductIdValue(productId.getValue());
    }

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM Inventory i WHERE i.productId = :productId")
    boolean existsByProductIdValue(@Param("productId") String productId);
}
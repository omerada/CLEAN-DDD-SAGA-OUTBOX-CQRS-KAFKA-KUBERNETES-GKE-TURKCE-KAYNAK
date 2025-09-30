package com.example.order.application.query.repository;

import com.example.order.application.query.model.OrderListView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Order List View Repository
 * 
 * CQRS Read Side repository for optimized queries
 */
@Repository
public interface OrderListViewRepository extends JpaRepository<OrderListView, String> {
    
    /**
     * Find orders by customer ID with pagination
     */
    Page<OrderListView> findByCustomerId(String customerId, Pageable pageable);
    
    /**
     * Find orders by customer ID and status
     */
    Page<OrderListView> findByCustomerIdAndStatus(
        String customerId, 
        String status, 
        Pageable pageable
    );
    
    /**
     * Find orders by status
     */
    Page<OrderListView> findByStatus(String status, Pageable pageable);
    
    /**
     * Find active orders for customer (not cancelled or delivered)
     */
    @Query("SELECT o FROM OrderListView o WHERE o.customerId = :customerId " +
           "AND o.status NOT IN ('CANCELLED', 'DELIVERED') " +
           "ORDER BY o.createdAt DESC")
    List<OrderListView> findActiveOrdersByCustomerId(@Param("customerId") String customerId);
    
    /**
     * Find recent orders with limit
     */
    @Query("SELECT o FROM OrderListView o ORDER BY o.createdAt DESC")
    List<OrderListView> findRecentOrders(Pageable pageable);
    
    /**
     * Count orders by status
     */
    long countByStatus(String status);
    
    /**
     * Count orders by customer
     */
    long countByCustomerId(String customerId);
}
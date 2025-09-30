package com.example.order.application.port.out;

import com.example.order.domain.entity.Order;
import com.example.order.domain.valueobject.CustomerId;
import com.example.order.domain.valueobject.OrderId;
import com.example.order.domain.valueobject.OrderStatus;

import java.util.List;
import java.util.Optional;

/**
 * Order Repository Port (Secondary Port)
 * 
 * Output boundary defining persistence requirements
 * Technology-agnostic interface for order data access
 * To be implemented by infrastructure adapters
 */
public interface OrderRepositoryPort {

    /**
     * Save or update an order
     * 
     * @param order Order to persist
     */
    void save(Order order);

    /**
     * Find order by unique identifier
     * 
     * @param orderId Order identifier
     * @return Optional order if found
     */
    Optional<Order> findById(OrderId orderId);

    /**
     * Find all orders for a specific customer
     * 
     * @param customerId Customer identifier  
     * @return List of customer orders
     */
    List<Order> findByCustomerId(CustomerId customerId);

    /**
     * Find orders by status
     * 
     * @param status Order status filter
     * @return List of orders with specified status
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Check if order exists
     * 
     * @param orderId Order identifier
     * @return true if order exists
     */
    boolean existsById(OrderId orderId);

    /**
     * Delete order by identifier
     * 
     * @param orderId Order identifier
     */
    void deleteById(OrderId orderId);

    /**
     * Count orders by customer
     * 
     * @param customerId Customer identifier
     * @return Number of orders for customer
     */
    long countByCustomerId(CustomerId customerId);

    /**
     * Find all orders (use with caution in production)
     * 
     * @return All orders in system
     */
    List<Order> findAll();
}
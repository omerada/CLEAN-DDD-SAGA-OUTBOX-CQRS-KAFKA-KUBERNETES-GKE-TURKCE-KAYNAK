package com.example.order.application.port.out;

import com.example.order.domain.entity.Order;
import com.example.order.domain.valueobject.*;
import java.util.List;
import java.util.Optional;

/**
 * Order Repository Port (Secondary Port)
 * 
 * Abstraction for order persistence operations
 * Technology-agnostic interface for data access
 */
public interface OrderRepository {

    /**
     * Save order aggregate
     * Returns the saved order with any generated values
     */
    Order save(Order order);

    /**
     * Find order by ID
     */
    Optional<Order> findById(OrderId orderId);

    /**
     * Find orders by customer ID
     */
    List<Order> findByCustomerId(CustomerId customerId);

    /**
     * Find orders by status
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Find all orders with pagination
     */
    List<Order> findAll(int page, int size);

    /**
     * Find all orders
     */
    List<Order> findAll();

    /**
     * Check if order exists
     */
    boolean existsById(OrderId orderId);

    /**
     * Delete order by ID
     */
    void deleteById(OrderId orderId);

    /**
     * Count total orders
     */
    long count();

    /**
     * Count orders by status
     */
    long countByStatus(OrderStatus status);

    /**
     * Count orders by customer
     */
    long countByCustomerId(CustomerId customerId);
}
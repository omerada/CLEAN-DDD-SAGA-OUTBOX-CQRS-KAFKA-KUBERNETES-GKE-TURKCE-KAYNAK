package com.example.order.infrastructure.adapter.out.persistence;

import com.example.order.infrastructure.adapter.out.persistence.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for Order persistence
 * 
 * Spring Data JPA interface for database operations
 * Auto-implements CRUD operations
 */
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, String> {

    /**
     * Find orders by customer ID
     */
    @Query("SELECT o FROM OrderJpaEntity o LEFT JOIN FETCH o.items WHERE o.customerId = :customerId ORDER BY o.createdAt DESC")
    List<OrderJpaEntity> findByCustomerIdWithItems(@Param("customerId") String customerId);

    /**
     * Find order by ID with items (eagerly loaded)
     */
    @Query("SELECT o FROM OrderJpaEntity o LEFT JOIN FETCH o.items WHERE o.orderId = :orderId")
    Optional<OrderJpaEntity> findByOrderIdWithItems(@Param("orderId") String orderId);

    /**
     * Find orders by status
     */
    @Query("SELECT o FROM OrderJpaEntity o LEFT JOIN FETCH o.items WHERE o.status = :status ORDER BY o.createdAt DESC")
    List<OrderJpaEntity> findByStatusWithItems(@Param("status") OrderJpaEntity.OrderStatusEnum status);

    /**
     * Check if order exists by ID
     */
    boolean existsByOrderId(String orderId);

    /**
     * Count orders by customer
     */
    @Query("SELECT COUNT(o) FROM OrderJpaEntity o WHERE o.customerId = :customerId")
    long countByCustomerId(@Param("customerId") String customerId);
}
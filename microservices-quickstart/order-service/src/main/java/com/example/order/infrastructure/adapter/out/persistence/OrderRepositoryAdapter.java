package com.example.order.infrastructure.adapter.out.persistence;

import com.example.order.application.port.out.OrderRepositoryPort;
import com.example.order.domain.entity.Order;
import com.example.order.domain.valueobject.CustomerId;
import com.example.order.domain.valueobject.OrderId;
import com.example.order.domain.valueobject.OrderStatus;
import com.example.order.infrastructure.adapter.out.persistence.entity.OrderJpaEntity;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Order Repository Adapter (Secondary Adapter)
 * 
 * Implements the OrderRepositoryPort from application layer
 * Adapts JPA technology to domain requirements
 * 
 * Key Responsibilities:
 * - Convert between domain and persistence models
 * - Handle transaction boundaries
 * - Provide domain-specific query methods
 * - Maintain data consistency
 */
@Component
@Transactional
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final OrderJpaRepository jpaRepository;
    private final OrderPersistenceMapper mapper;

    public OrderRepositoryAdapter(OrderJpaRepository jpaRepository,
            OrderPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity jpaEntity = mapper.toJpaEntity(order);
        OrderJpaEntity savedEntity = jpaRepository.save(jpaEntity);
        
        // Convert back to domain model
        return mapper.toDomainEntity(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(OrderId orderId) {
        return jpaRepository.findByOrderIdWithItems(orderId.getValue())
                .map(mapper::toDomainEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByCustomerId(CustomerId customerId) {
        return jpaRepository.findByCustomerIdWithItems(customerId.getValue())
                .stream()
                .map(mapper::toDomainEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByStatus(OrderStatus status) {
        OrderJpaEntity.OrderStatusEnum jpaStatus = mapStatusToJpa(status);
        return jpaRepository.findByStatusWithItems(jpaStatus)
                .stream()
                .map(mapper::toDomainEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(OrderId orderId) {
        return jpaRepository.existsByOrderId(orderId.getValue());
    }

    @Override
    public void deleteById(OrderId orderId) {
        jpaRepository.deleteById(orderId.getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCustomerId(CustomerId customerId) {
        return jpaRepository.countByCustomerId(customerId.getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomainEntity)
                .toList();
    }

    /**
     * Helper method to map domain status to JPA enum
     * Delegated to mapper for consistency
     */
    private OrderJpaEntity.OrderStatusEnum mapStatusToJpa(OrderStatus status) {
        // Create a temporary order to use the mapper
        // In practice, you might extract this to a shared utility
        return switch (status) {
            case PENDING -> OrderJpaEntity.OrderStatusEnum.PENDING;
            case CONFIRMED -> OrderJpaEntity.OrderStatusEnum.CONFIRMED;
            case CANCELLED -> OrderJpaEntity.OrderStatusEnum.CANCELLED;
            case SHIPPED, DELIVERED, FAILED ->
                throw new IllegalArgumentException("Status not supported in persistence: " + status);
        };
    }
}
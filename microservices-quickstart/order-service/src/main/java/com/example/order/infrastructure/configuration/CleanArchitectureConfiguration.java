package com.example.order.infrastructure.configuration;

import com.example.order.application.port.in.CreateOrderUseCase;
import com.example.order.application.port.in.GetOrderUseCase;
import com.example.order.application.port.out.OrderRepositoryPort;
import com.example.order.application.port.out.OutboxRepositoryPort;
import com.example.order.application.service.OrderApplicationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clean Architecture Configuration
 * 
 * Wire together all layers according to dependency inversion principle:
 * - Controllers depend on use case interfaces (ports)
 * - Application services implement use case interfaces
 * - Application services depend on repository interfaces (ports)
 * - Repository adapters implement repository interfaces
 * 
 * This configuration ensures:
 * - Domain layer has no dependencies on other layers
 * - Application layer depends only on domain
 * - Infrastructure depends on application and domain
 * - Interfaces are owned by inner layers
 */
@Configuration
public class CleanArchitectureConfiguration {

    /**
     * Application Service Bean
     * 
     * Implements both CreateOrderUseCase and GetOrderUseCase
     * Depends on OrderRepositoryPort and OutboxRepositoryPort
     */
    @Bean
    public OrderApplicationService orderApplicationService(
            OrderRepositoryPort orderRepositoryPort,
            OutboxRepositoryPort outboxRepositoryPort) {
        return new OrderApplicationService(orderRepositoryPort, outboxRepositoryPort);
    }

    /**
     * Create Order Use Case Bean
     * 
     * Exposing application service as CreateOrderUseCase interface
     * Controllers will depend on this interface, not the concrete service
     */
    @Bean
    public CreateOrderUseCase createOrderUseCase(OrderApplicationService orderApplicationService) {
        return orderApplicationService;
    }

    /**
     * Get Order Use Case Bean
     * 
     * Exposing application service as GetOrderUseCase interface
     * Controllers will depend on this interface, not the concrete service
     */
    @Bean
    public GetOrderUseCase getOrderUseCase(OrderApplicationService orderApplicationService) {
        return orderApplicationService;
    }

    // Note: OrderRepositoryPort implementation (OrderRepositoryAdapter)
    // is automatically detected by Spring through @Component annotation
    // and will be injected where OrderRepositoryPort is required
}
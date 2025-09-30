package com.example.order.infrastructure.adapter.in.web.dto;

import com.example.order.domain.valueobject.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Create Order Request DTO
 * 
 * Web layer data transfer object for order creation
 * Contains validation annotations for HTTP requests
 */
public record CreateOrderRequest(
    @NotBlank(message = "Customer ID is required")
    @Pattern(regexp = "CUST-\\w+", message = "Customer ID must follow format: CUST-XXX")
    String customerId,
    
    @Valid
    @NotNull(message = "Items are required")
    @Size(min = 1, max = 50, message = "Order must contain 1-50 items")
    List<OrderItemRequest> items
) {
    
    /**
     * Order Item Request DTO
     */
    public record OrderItemRequest(
        @NotBlank(message = "Product ID is required")
        @Pattern(regexp = "PROD-\\w+", message = "Product ID must follow format: PROD-XXX")
        String productId,
        
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 1000, message = "Quantity cannot exceed 1000")
        Integer quantity,
        
        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.01", message = "Unit price must be positive")
        @DecimalMax(value = "99999.99", message = "Unit price cannot exceed 99999.99")
        Double unitPrice
    ) {
        /**
         * Convert to domain value objects
         */
        public com.example.order.application.port.in.CreateOrderUseCase.OrderItemCommand toDomainCommand() {
            return new com.example.order.application.port.in.CreateOrderUseCase.OrderItemCommand(
                ProductId.of(productId),
                quantity,
                Money.of(unitPrice)
            );
        }
    }
    
    /**
     * Convert to domain command
     */
    public com.example.order.application.port.in.CreateOrderUseCase.CreateOrderCommand toDomainCommand() {
        List<com.example.order.application.port.in.CreateOrderUseCase.OrderItemCommand> itemCommands = 
            items.stream()
                .map(OrderItemRequest::toDomainCommand)
                .toList();
                
        return new com.example.order.application.port.in.CreateOrderUseCase.CreateOrderCommand(
            CustomerId.of(customerId),
            itemCommands
        );
    }
}
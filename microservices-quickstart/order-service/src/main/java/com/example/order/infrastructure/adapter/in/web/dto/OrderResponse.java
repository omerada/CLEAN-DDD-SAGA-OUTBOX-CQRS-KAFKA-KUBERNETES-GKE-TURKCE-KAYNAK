package com.example.order.infrastructure.adapter.in.web.dto;

import com.example.order.domain.entity.Order;
import com.example.order.domain.entity.OrderItem;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Response DTO
 * 
 * Web layer data transfer object for order retrieval
 * Converts domain objects to JSON-serializable format
 */
public record OrderResponse(
    String orderId,
    String customerId,
    String status,
    Double totalAmount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<OrderItemResponse> items
) {
    
    /**
     * Order Item Response DTO
     */
    public record OrderItemResponse(
        String productId,
        Integer quantity,
        Double unitPrice,
        Double subtotal
    ) {
        /**
         * Create from domain entity
         */
        public static OrderItemResponse fromDomain(OrderItem orderItem) {
            return new OrderItemResponse(
                orderItem.getProductId().getValue(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice().getAmount().doubleValue(),
                orderItem.getSubtotal().getAmount().doubleValue()
            );
        }
    }
    
    /**
     * Create from domain aggregate
     */
    public static OrderResponse fromDomain(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
            .map(OrderItemResponse::fromDomain)
            .toList();
            
        return new OrderResponse(
            order.getId().getValue(),
            order.getCustomerId().getValue(),
            order.getStatus().name(),
            order.getTotalAmount().getAmount().doubleValue(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            itemResponses
        );
    }
}
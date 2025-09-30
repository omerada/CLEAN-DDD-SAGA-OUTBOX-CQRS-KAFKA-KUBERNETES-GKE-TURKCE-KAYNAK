package com.example.order.application.port.in;

import com.example.order.domain.valueobject.*;
import com.example.order.domain.entity.OrderItem;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Get Order Use Case (Primary Port)
 * 
 * Query interface for order retrieval
 * Supports both single order and multiple order queries
 */
public interface GetOrderUseCase {

    GetOrderResponse getOrder(GetOrderQuery query);
    
    List<GetOrderResponse> getOrdersByCustomer(GetOrdersByCustomerQuery query);
    
    List<GetOrderResponse> getAllOrders(GetAllOrdersQuery query);

    /**
     * Query for single order retrieval
     */
    record GetOrderQuery(OrderId orderId) {
        public GetOrderQuery {
            if (orderId == null) {
                throw new IllegalArgumentException("Order ID is required");
            }
        }
    }

    /**
     * Query for customer orders retrieval
     */
    record GetOrdersByCustomerQuery(CustomerId customerId) {
        public GetOrdersByCustomerQuery {
            if (customerId == null) {
                throw new IllegalArgumentException("Customer ID is required");
            }
        }
    }

    /**
     * Query for all orders (with pagination support)
     */
    record GetAllOrdersQuery(
        int page,
        int size,
        OrderStatus statusFilter
    ) {
        public GetAllOrdersQuery {
            if (page < 0) {
                throw new IllegalArgumentException("Page cannot be negative");
            }
            if (size <= 0 || size > 100) {
                throw new IllegalArgumentException("Size must be between 1 and 100");
            }
        }

        // Convenience constructor without filter
        public GetAllOrdersQuery(int page, int size) {
            this(page, size, null);
        }
    }

    /**
     * Response for order queries
     */
    record GetOrderResponse(
        OrderId orderId,
        CustomerId customerId,
        OrderStatus status,
        Money totalAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items
    ) {
        /**
         * Order item response for queries
         */
        public record OrderItemResponse(
            ProductId productId,
            int quantity,
            Money unitPrice,
            Money subtotal
        ) {
            public static OrderItemResponse from(OrderItem orderItem) {
                return new OrderItemResponse(
                    orderItem.getProductId(),
                    orderItem.getQuantity(),
                    orderItem.getUnitPrice(),
                    orderItem.getSubtotal()
                );
            }
        }

        public static GetOrderResponse from(com.example.order.domain.entity.Order order) {
            List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();

            return new GetOrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemResponses
            );
        }
    }
}
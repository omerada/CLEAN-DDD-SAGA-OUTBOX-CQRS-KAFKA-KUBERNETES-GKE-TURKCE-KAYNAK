package com.example.order.application.port.in;

import com.example.order.domain.valueobject.*;
import com.example.order.domain.entity.OrderItem;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Create Order Use Case (Primary Port)
 * 
 * Input boundary for order creation business process
 * Technology-agnostic interface defining the use case contract
 */
public interface CreateOrderUseCase {

    CreateOrderResponse createOrder(CreateOrderCommand command);

    /**
     * Command for order creation
     * Encapsulates all required data with validation
     */
    record CreateOrderCommand(
            CustomerId customerId,
            List<OrderItemCommand> items) {
        public CreateOrderCommand {
            if (customerId == null) {
                throw new IllegalArgumentException("Customer ID is required");
            }
            if (items == null || items.isEmpty()) {
                throw new IllegalArgumentException("Order items are required");
            }
            if (items.size() > 50) {
                throw new IllegalArgumentException("Order cannot contain more than 50 items");
            }
        }
    }

    /**
     * Order item command for creation
     */
    record OrderItemCommand(
            ProductId productId,
            int quantity,
            Money unitPrice) {
        public OrderItemCommand {
            if (productId == null) {
                throw new IllegalArgumentException("Product ID is required");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (quantity > 1000) {
                throw new IllegalArgumentException("Quantity cannot exceed 1000 per item");
            }
            if (unitPrice == null || !unitPrice.isPositive()) {
                throw new IllegalArgumentException("Unit price must be positive");
            }
        }
    }

    /**
     * Response for order creation
     */
    record CreateOrderResponse(
            OrderId orderId,
            CustomerId customerId,
            OrderStatus status,
            Money totalAmount,
            int totalItems,
            LocalDateTime createdAt,
            List<OrderItemResponse> items) {
        /**
         * Order item response
         */
        public record OrderItemResponse(
                ProductId productId,
                int quantity,
                Money unitPrice,
                Money subtotal) {
            public static OrderItemResponse from(OrderItem orderItem) {
                return new OrderItemResponse(
                        orderItem.getProductId(),
                        orderItem.getQuantity(),
                        orderItem.getUnitPrice(),
                        orderItem.getSubtotal());
            }
        }

        public static CreateOrderResponse from(com.example.order.domain.entity.Order order) {
            List<OrderItemResponse> itemResponses = order.getItems().stream()
                    .map(OrderItemResponse::from)
                    .toList();

            return new CreateOrderResponse(
                    order.getId(),
                    order.getCustomerId(),
                    order.getStatus(),
                    order.getTotalAmount(),
                    order.getItems().stream().mapToInt(OrderItem::getQuantity).sum(),
                    order.getCreatedAt(),
                    itemResponses);
        }
    }
}
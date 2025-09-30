package com.example.order.infrastructure.adapter.in.web;

import com.example.order.application.port.in.CreateOrderUseCase;
import com.example.order.application.port.in.GetOrderUseCase;
import com.example.order.domain.valueobject.OrderId;
import com.example.order.infrastructure.adapter.in.web.dto.CreateOrderRequest;
import com.example.order.infrastructure.adapter.in.web.dto.OrderResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

/**
 * Clean Architecture Web Adapter for Order Operations
 * 
 * Primary Adapter (driving side) that receives HTTP requests
 * and delegates to application use cases.
 * 
 * Key Principles:
 * - Does not contain business logic
 * - Converts between HTTP and domain formats
 * - Delegates all operations to use case ports
 * - Handles only web-specific concerns (validation, status codes)
 */
@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase,
            GetOrderUseCase getOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
    }

    /**
     * Create a new order
     * 
     * @param request Order creation request
     * @return Created order response
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        try {
            // Convert web request to domain command
            CreateOrderUseCase.CreateOrderCommand command = request.toDomainCommand();

            // Execute use case
            CreateOrderUseCase.CreateOrderResponse useCaseResponse = createOrderUseCase.createOrder(command);

            // Convert use case response to web response
            OrderResponse response = new OrderResponse(
                    useCaseResponse.orderId().getValue(),
                    useCaseResponse.customerId().getValue(),
                    useCaseResponse.status().name(),
                    useCaseResponse.totalAmount().getAmount().doubleValue(),
                    useCaseResponse.createdAt(),
                    useCaseResponse.createdAt(), // updatedAt same as createdAt for new orders
                    useCaseResponse.items().stream()
                            .map(item -> new OrderResponse.OrderItemResponse(
                                    item.productId().getValue(),
                                    item.quantity(),
                                    item.unitPrice().getAmount().doubleValue(),
                                    item.subtotal().getAmount().doubleValue()))
                            .toList());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            // Domain validation errors
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            // Unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get order by ID
     * 
     * @param orderId Order identifier
     * @return Order response or 404 if not found
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable @Pattern(regexp = "ORD-\\w+", message = "Order ID must follow format: ORD-XXX") String orderId) {
        try {
            // Convert web parameter to domain query
            OrderId orderIdVO = OrderId.of(orderId);
            GetOrderUseCase.GetOrderQuery query = new GetOrderUseCase.GetOrderQuery(orderIdVO);

            // Execute use case
            GetOrderUseCase.GetOrderResponse useCaseResponse = getOrderUseCase.getOrder(query);

            if (useCaseResponse == null) {
                return ResponseEntity.notFound().build();
            }

            // Convert use case response to web response
            OrderResponse response = new OrderResponse(
                    useCaseResponse.orderId().getValue(),
                    useCaseResponse.customerId().getValue(),
                    useCaseResponse.status().name(),
                    useCaseResponse.totalAmount().getAmount().doubleValue(),
                    useCaseResponse.createdAt(),
                    useCaseResponse.updatedAt(),
                    useCaseResponse.items().stream()
                            .map(item -> new OrderResponse.OrderItemResponse(
                                    item.productId().getValue(),
                                    item.quantity(),
                                    item.unitPrice().getAmount().doubleValue(),
                                    item.subtotal().getAmount().doubleValue()))
                            .toList());

            return ResponseEntity.ok().body(response);

        } catch (IllegalArgumentException e) {
            // Invalid order ID format
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            // Unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint
     * 
     * @return Service status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order Service is running - Clean Architecture Version");
    }
}
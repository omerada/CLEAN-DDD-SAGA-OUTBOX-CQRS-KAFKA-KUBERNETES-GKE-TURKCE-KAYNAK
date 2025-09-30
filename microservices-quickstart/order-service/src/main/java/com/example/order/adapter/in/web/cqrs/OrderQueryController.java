package com.example.order.adapter.in.web.cqrs;

import com.example.order.application.query.GetOrdersQuery;
import com.example.order.application.query.handler.OrderQueryHandler;
import com.example.order.application.query.handler.GetOrdersResult;
import com.example.order.application.query.model.OrderListView;
import com.example.order.domain.valueobject.CustomerId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Query Controller
 * 
 * CQRS Read Side controller for order queries.
 * Handles all read operations with optimized read models.
 */
@RestController
@RequestMapping("/api/orders/queries")
@CrossOrigin(origins = "*")
public class OrderQueryController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderQueryController.class);
    
    private final OrderQueryHandler queryHandler;
    
    public OrderQueryController(OrderQueryHandler queryHandler) {
        this.queryHandler = queryHandler;
    }
    
    /**
     * Get orders with filtering and pagination - CQRS Query
     */
    @GetMapping
    public ResponseEntity<GetOrdersResponse> getOrders(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        
        try {
            log.debug("Received get orders query - customer: {}, status: {}, page: {}, size: {}", 
                     customerId, status, page, size);
            
            // Create query
            GetOrdersQuery query = new GetOrdersQuery(
                customerId != null ? CustomerId.of(customerId) : null,
                status,
                page,
                size
            );
            
            // Execute query
            GetOrdersResult result = queryHandler.handle(query);
            
            // Convert to response
            GetOrdersResponse response = new GetOrdersResponse(
                result.getOrders().stream()
                    .map(this::toOrderSummaryDto)
                    .toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getCurrentPage(),
                result.getPageSize()
            );
            
            log.debug("Retrieved {} orders", result.getSize());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing get orders query", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get active orders for customer
     */
    @GetMapping("/active/{customerId}")
    public ResponseEntity<List<OrderSummaryDto>> getActiveOrders(
            @PathVariable String customerId) {
        
        try {
            log.debug("Getting active orders for customer: {}", customerId);
            
            List<OrderListView> activeOrders = queryHandler.getActiveOrdersForCustomer(customerId);
            
            List<OrderSummaryDto> response = activeOrders.stream()
                .map(this::toOrderSummaryDto)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting active orders for customer: {}", customerId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get recent orders
     */
    @GetMapping("/recent")
    public ResponseEntity<List<OrderSummaryDto>> getRecentOrders(
            @RequestParam(defaultValue = "10") Integer limit) {
        
        try {
            log.debug("Getting {} recent orders", limit);
            
            List<OrderListView> recentOrders = queryHandler.getRecentOrders(limit);
            
            List<OrderSummaryDto> response = recentOrders.stream()
                .map(this::toOrderSummaryDto)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting recent orders", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Convert OrderListView to DTO
     */
    private OrderSummaryDto toOrderSummaryDto(OrderListView orderView) {
        return new OrderSummaryDto(
            orderView.getOrderId(),
            orderView.getCustomerId(),
            orderView.getStatus(),
            orderView.getTotalAmount(),
            orderView.getCurrency(),
            orderView.getItemCount(),
            orderView.getCreatedAt(),
            orderView.getUpdatedAt(),
            orderView.isActive()
        );
    }
}

/**
 * Get Orders Response DTO
 */
record GetOrdersResponse(
    List<OrderSummaryDto> orders,
    long totalElements,
    int totalPages,
    int currentPage,
    int pageSize
) {}

/**
 * Order Summary DTO for read operations
 */
record OrderSummaryDto(
    String orderId,
    String customerId,
    String status,
    BigDecimal totalAmount,
    String currency,
    Integer itemCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    boolean isActive
) {}
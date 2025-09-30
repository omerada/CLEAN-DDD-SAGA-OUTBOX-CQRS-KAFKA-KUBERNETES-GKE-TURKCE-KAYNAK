package com.example.order.adapter.in.web.cqrs;

import com.example.order.application.command.CreateOrderCommand;
import com.example.order.application.command.OrderItemCommand;
import com.example.order.application.command.handler.OrderCommandHandler;
import com.example.order.application.command.handler.CreateOrderResult;
import com.example.order.domain.valueobject.CustomerId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

/**
 * Order Command Controller
 * 
 * CQRS Write Side controller for order commands.
 * Handles all write operations (create, update, delete).
 */
@RestController
@RequestMapping("/api/orders/commands")
@CrossOrigin(origins = "*")
public class OrderCommandController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderCommandController.class);
    
    private final OrderCommandHandler commandHandler;
    
    public OrderCommandController(OrderCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }
    
    /**
     * Create new order - CQRS Command
     */
    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        
        try {
            log.info("Received create order command for customer: {}", request.customerId());
            
            // Convert request to command
            CreateOrderCommand command = new CreateOrderCommand(
                CustomerId.of(request.customerId()),
                request.items().stream()
                    .map(item -> new OrderItemCommand(
                        item.productId(),
                        item.quantity(),
                        item.unitPrice()
                    ))
                    .toList()
            );
            
            // Execute command
            CreateOrderResult result = commandHandler.handle(command);
            
            if (result.isSuccess()) {
                CreateOrderResponse response = new CreateOrderResponse(
                    result.getOrderId().getValue(),
                    "Order created successfully"
                );
                
                log.info("Order created successfully: {}", result.getOrderId().getValue());
                
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                log.warn("Order creation failed: {}", result.getErrorMessage());
                
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CreateOrderResponse(null, result.getErrorMessage()));
            }
            
        } catch (Exception e) {
            log.error("Error processing create order command", e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new CreateOrderResponse(null, "Internal server error"));
        }
    }
}

/**
 * Create Order Request DTO
 */
record CreateOrderRequest(
    String customerId,
    List<OrderItemRequest> items
) {}

/**
 * Order Item Request DTO
 */
record OrderItemRequest(
    String productId,
    Integer quantity,
    BigDecimal unitPrice
) {}

/**
 * Create Order Response DTO
 */
record CreateOrderResponse(
    String orderId,
    String message
) {}
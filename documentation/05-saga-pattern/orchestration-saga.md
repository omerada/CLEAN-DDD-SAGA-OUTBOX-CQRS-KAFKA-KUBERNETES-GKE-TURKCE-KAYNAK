# 🎯 Orchestration SAGA Implementation

## Orchestration SAGA Nedir?

Orchestration SAGA'da merkezi bir koordinatör (Orchestrator) tüm SAGA adımlarını yönetir. Choreography'nin aksine, business process'in kontrolü tek bir yerde toplanır.

### Orchestration vs Choreography

```ascii
┌─────────────────────────────────────────────────────────────────────┐
│                   ORCHESTRATION vs CHOREOGRAPHY                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  CHOREOGRAPHY (Event-Driven)     ORCHESTRATION (Command-Driven)    │
│  ┌─────────────────────────┐     ┌─────────────────────────────┐   │
│  │ ┌─────┐ event ┌─────────┐│     │    ┌───────────────────┐   │   │
│  │ │ Svc │──────►│   Svc   ││     │    │   Orchestrator    │   │   │
│  │ │  A  │       │    B    ││     │    │                   │   │   │
│  │ └─────┘       └─────────┘│     │    └───┬───────────┬───┘   │   │
│  │     │ ▲           │      │     │        │           │       │   │
│  │  event│       event      │     │     cmd│           │cmd    │   │
│  │     ▼ │           ▼      │     │        ▼           ▼       │   │
│  │ ┌─────────┐ ◄─── ┌─────┐ │     │    ┌───────┐   ┌───────┐   │   │
│  │ │   Svc   │      │ Svc ││     │    │ Svc A │   │ Svc B │   │   │
│  │ │    D    │      │  C  ││     │    └───────┘   └───────┘   │   │
│  │ └─────────┘      └─────┘ │     │        │           │       │   │
│  └─────────────────────────┘     │     resp│           │resp   │   │
│                                   │        ▼           ▼       │   │
│  ✅ Loose coupling                │    ┌───────────────────┐   │   │
│  ✅ High autonomy                 │    │   Orchestrator    │   │   │
│  ❌ Complex debugging             │    └───────────────────┘   │   │
│  ❌ No central control            │                             │   │
│                                   │  ✅ Central control        │   │
│                                   │  ✅ Easy debugging         │   │
│                                   │  ❌ Single point failure   │   │
│                                   │  ❌ Tight coupling         │   │
│                                   └─────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

## SAGA Orchestrator Implementation

### SAGA State Machine

```java
// domain/saga/OrderProcessingSagaOrchestrator.java
package com.example.saga.orchestration;

import com.example.saga.domain.model.SagaExecution;
import com.example.saga.domain.valueobject.*;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * Order Processing SAGA Orchestrator
 *
 * Merkezi koordinatör pattern ile tüm SAGA adımlarını yönetir.
 * State machine mantığı ile forward ve backward recovery sağlar.
 */
@Component
@Slf4j
public class OrderProcessingSagaOrchestrator {

    private final InventoryServiceClient inventoryService;
    private final PaymentServiceClient paymentService;
    private final OrderServiceClient orderService;
    private final SagaExecutionRepository sagaExecutionRepository;
    private final SagaEventPublisher sagaEventPublisher;

    public OrderProcessingSagaOrchestrator(
        InventoryServiceClient inventoryService,
        PaymentServiceClient paymentService,
        OrderServiceClient orderService,
        SagaExecutionRepository sagaExecutionRepository,
        SagaEventPublisher sagaEventPublisher
    ) {
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.sagaExecutionRepository = sagaExecutionRepository;
        this.sagaEventPublisher = sagaEventPublisher;
    }

    /**
     * SAGA'yı başlat - Order placement'tan sonra
     */
    public SagaExecutionResult startOrderProcessingSaga(OrderPlacedEvent event) {
        log.info("Starting Order Processing SAGA for order: {}", event.getOrderId());

        try {
            // SAGA execution instance oluştur
            SagaExecution sagaExecution = SagaExecution.start(
                SagaId.generate(),
                SagaType.ORDER_PROCESSING,
                OrderId.of(event.getOrderId()),
                SagaState.STARTED,
                createSagaContext(event)
            );

            sagaExecutionRepository.save(sagaExecution);

            // İlk adımı başlat: Inventory Reservation
            executeStep1_ReserveInventory(sagaExecution);

            return SagaExecutionResult.started(sagaExecution.getId());

        } catch (Exception e) {
            log.error("Failed to start SAGA for order: {}", event.getOrderId(), e);
            return SagaExecutionResult.failed("Failed to start SAGA: " + e.getMessage());
        }
    }

    /**
     * Adım 1: Inventory Reservation
     */
    private void executeStep1_ReserveInventory(SagaExecution sagaExecution) {
        log.info("SAGA {}: Executing Step 1 - Reserve Inventory",
                sagaExecution.getId().getValue());

        try {
            // State'i güncelle
            sagaExecution.moveToState(SagaState.RESERVING_INVENTORY);
            sagaExecutionRepository.save(sagaExecution);

            // Inventory service'e command gönder
            ReserveInventoryCommand command = createReserveInventoryCommand(sagaExecution);

            CompletableFuture<InventoryReservationResult> future =
                inventoryService.reserveInventoryAsync(command);

            // Async response handler
            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    handleStep1_InventoryReservationFailed(sagaExecution, throwable.getMessage());
                } else if (result.isSuccessful()) {
                    handleStep1_InventoryReservationSuccess(sagaExecution, result);
                } else {
                    handleStep1_InventoryReservationFailed(sagaExecution, result.getFailureReason());
                }
            });

            // Timeout handler setup
            scheduleTimeoutHandler(sagaExecution, Duration.ofMinutes(5), SagaState.RESERVING_INVENTORY);

        } catch (Exception e) {
            log.error("SAGA {}: Error in Step 1 - Reserve Inventory",
                     sagaExecution.getId().getValue(), e);
            handleStep1_InventoryReservationFailed(sagaExecution, e.getMessage());
        }
    }

    /**
     * Adım 1 Başarılı: Inventory Reserved
     */
    private void handleStep1_InventoryReservationSuccess(
        SagaExecution sagaExecution,
        InventoryReservationResult result
    ) {
        log.info("SAGA {}: Step 1 completed successfully - Inventory Reserved",
                sagaExecution.getId().getValue());

        try {
            // SAGA context'e reservation bilgilerini ekle
            sagaExecution.addContextData("inventoryReservations", result.getReservations());
            sagaExecution.addContextData("reservationIds", result.getReservationIds());

            // Sonraki adıma geç: Payment Authorization
            executeStep2_AuthorizePayment(sagaExecution);

        } catch (Exception e) {
            log.error("SAGA {}: Error handling inventory reservation success",
                     sagaExecution.getId().getValue(), e);
            startCompensation(sagaExecution, "Error proceeding to payment: " + e.getMessage());
        }
    }

    /**
     * Adım 1 Başarısız: Inventory Reservation Failed
     */
    private void handleStep1_InventoryReservationFailed(
        SagaExecution sagaExecution,
        String reason
    ) {
        log.warn("SAGA {}: Step 1 failed - Inventory Reservation: {}",
                sagaExecution.getId().getValue(), reason);

        // Inventory reservation başarısız - order'ı cancel et
        sagaExecution.moveToState(SagaState.COMPENSATING);
        sagaExecution.addContextData("failureReason", reason);
        sagaExecutionRepository.save(sagaExecution);

        // Order cancellation compensation
        executeCompensation_CancelOrder(sagaExecution, reason);
    }

    /**
     * Adım 2: Payment Authorization
     */
    private void executeStep2_AuthorizePayment(SagaExecution sagaExecution) {
        log.info("SAGA {}: Executing Step 2 - Authorize Payment",
                sagaExecution.getId().getValue());

        try {
            // State'i güncelle
            sagaExecution.moveToState(SagaState.AUTHORIZING_PAYMENT);
            sagaExecutionRepository.save(sagaExecution);

            // Payment service'e command gönder
            AuthorizePaymentCommand command = createAuthorizePaymentCommand(sagaExecution);

            CompletableFuture<PaymentAuthorizationResult> future =
                paymentService.authorizePaymentAsync(command);

            // Async response handler
            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    handleStep2_PaymentAuthorizationFailed(sagaExecution, throwable.getMessage());
                } else if (result.isSuccessful()) {
                    handleStep2_PaymentAuthorizationSuccess(sagaExecution, result);
                } else {
                    handleStep2_PaymentAuthorizationFailed(sagaExecution, result.getFailureReason());
                }
            });

            // Timeout handler setup
            scheduleTimeoutHandler(sagaExecution, Duration.ofMinutes(3), SagaState.AUTHORIZING_PAYMENT);

        } catch (Exception e) {
            log.error("SAGA {}: Error in Step 2 - Authorize Payment",
                     sagaExecution.getId().getValue(), e);
            handleStep2_PaymentAuthorizationFailed(sagaExecution, e.getMessage());
        }
    }

    /**
     * Adım 2 Başarılı: Payment Authorized
     */
    private void handleStep2_PaymentAuthorizationSuccess(
        SagaExecution sagaExecution,
        PaymentAuthorizationResult result
    ) {
        log.info("SAGA {}: Step 2 completed successfully - Payment Authorized",
                sagaExecution.getId().getValue());

        try {
            // SAGA context'e payment bilgilerini ekle
            sagaExecution.addContextData("paymentId", result.getPaymentId());
            sagaExecution.addContextData("authorizationCode", result.getAuthorizationCode());

            // Son adıma geç: Order Confirmation
            executeStep3_ConfirmOrder(sagaExecution);

        } catch (Exception e) {
            log.error("SAGA {}: Error handling payment authorization success",
                     sagaExecution.getId().getValue(), e);
            startCompensation(sagaExecution, "Error confirming order: " + e.getMessage());
        }
    }

    /**
     * Adım 2 Başarısız: Payment Authorization Failed
     */
    private void handleStep2_PaymentAuthorizationFailed(
        SagaExecution sagaExecution,
        String reason
    ) {
        log.warn("SAGA {}: Step 2 failed - Payment Authorization: {}",
                sagaExecution.getId().getValue(), reason);

        // Payment başarısız - compensation başlat
        startCompensation(sagaExecution, "Payment authorization failed: " + reason);
    }

    /**
     * Adım 3: Order Confirmation
     */
    private void executeStep3_ConfirmOrder(SagaExecution sagaExecution) {
        log.info("SAGA {}: Executing Step 3 - Confirm Order",
                sagaExecution.getId().getValue());

        try {
            // State'i güncelle
            sagaExecution.moveToState(SagaState.CONFIRMING_ORDER);
            sagaExecutionRepository.save(sagaExecution);

            // Order service'e command gönder
            ConfirmOrderCommand command = createConfirmOrderCommand(sagaExecution);

            CompletableFuture<OrderConfirmationResult> future =
                orderService.confirmOrderAsync(command);

            // Async response handler
            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    handleStep3_OrderConfirmationFailed(sagaExecution, throwable.getMessage());
                } else if (result.isSuccessful()) {
                    handleStep3_OrderConfirmationSuccess(sagaExecution, result);
                } else {
                    handleStep3_OrderConfirmationFailed(sagaExecution, result.getFailureReason());
                }
            });

            // Timeout handler setup
            scheduleTimeoutHandler(sagaExecution, Duration.ofMinutes(2), SagaState.CONFIRMING_ORDER);

        } catch (Exception e) {
            log.error("SAGA {}: Error in Step 3 - Confirm Order",
                     sagaExecution.getId().getValue(), e);
            handleStep3_OrderConfirmationFailed(sagaExecution, e.getMessage());
        }
    }

    /**
     * Adım 3 Başarılı: Order Confirmed - SAGA Complete
     */
    private void handleStep3_OrderConfirmationSuccess(
        SagaExecution sagaExecution,
        OrderConfirmationResult result
    ) {
        log.info("SAGA {}: Step 3 completed successfully - Order Confirmed",
                sagaExecution.getId().getValue());

        try {
            // SAGA'yı başarıyla tamamla
            sagaExecution.complete();
            sagaExecutionRepository.save(sagaExecution);

            // Success event publish et
            SagaCompletedEvent completedEvent = new SagaCompletedEvent(
                sagaExecution.getId(),
                sagaExecution.getOrderId(),
                SagaResult.SUCCESS,
                sagaExecution.getDuration(),
                LocalDateTime.now()
            );

            sagaEventPublisher.publish(completedEvent);

            log.info("SAGA {}: Successfully completed for order {}",
                    sagaExecution.getId().getValue(),
                    sagaExecution.getOrderId().getValue());

        } catch (Exception e) {
            log.error("SAGA {}: Error completing SAGA",
                     sagaExecution.getId().getValue(), e);
        }
    }

    /**
     * Adım 3 Başarısız: Order Confirmation Failed
     */
    private void handleStep3_OrderConfirmationFailed(
        SagaExecution sagaExecution,
        String reason
    ) {
        log.warn("SAGA {}: Step 3 failed - Order Confirmation: {}",
                sagaExecution.getId().getValue(), reason);

        // Order confirmation başarısız - full compensation
        startCompensation(sagaExecution, "Order confirmation failed: " + reason);
    }

    /**
     * Compensation workflow başlat
     */
    private void startCompensation(SagaExecution sagaExecution, String reason) {
        log.info("SAGA {}: Starting compensation workflow - {}",
                sagaExecution.getId().getValue(), reason);

        try {
            sagaExecution.moveToState(SagaState.COMPENSATING);
            sagaExecution.addContextData("compensationReason", reason);
            sagaExecutionRepository.save(sagaExecution);

            // Compensation steps - reverse order
            if (sagaExecution.hasContextData("paymentId")) {
                // Payment authorized - void it
                executeCompensation_VoidPayment(sagaExecution);
            } else if (sagaExecution.hasContextData("inventoryReservations")) {
                // Only inventory reserved - release it
                executeCompensation_ReleaseInventory(sagaExecution);
            } else {
                // Nothing to compensate, just cancel order
                executeCompensation_CancelOrder(sagaExecution, reason);
            }

        } catch (Exception e) {
            log.error("SAGA {}: Error starting compensation",
                     sagaExecution.getId().getValue(), e);

            // Mark SAGA as failed
            sagaExecution.fail("Compensation start failed: " + e.getMessage());
            sagaExecutionRepository.save(sagaExecution);
        }
    }

    /**
     * Compensation: Void Payment
     */
    private void executeCompensation_VoidPayment(SagaExecution sagaExecution) {
        log.info("SAGA {}: Executing compensation - Void Payment",
                sagaExecution.getId().getValue());

        try {
            String paymentId = (String) sagaExecution.getContextData("paymentId");

            VoidPaymentCommand command = VoidPaymentCommand.create(
                PaymentId.of(paymentId),
                "SAGA compensation",
                "SAGA-ORCHESTRATOR"
            );

            CompletableFuture<PaymentVoidResult> future =
                paymentService.voidPaymentAsync(command);

            future.whenComplete((result, throwable) -> {
                if (throwable != null || !result.isSuccessful()) {
                    log.warn("SAGA {}: Payment void failed, continuing with inventory compensation",
                            sagaExecution.getId().getValue());
                }

                // Continue with inventory compensation
                executeCompensation_ReleaseInventory(sagaExecution);
            });

        } catch (Exception e) {
            log.error("SAGA {}: Error voiding payment",
                     sagaExecution.getId().getValue(), e);

            // Continue with inventory compensation even if payment void fails
            executeCompensation_ReleaseInventory(sagaExecution);
        }
    }

    /**
     * Compensation: Release Inventory
     */
    private void executeCompensation_ReleaseInventory(SagaExecution sagaExecution) {
        log.info("SAGA {}: Executing compensation - Release Inventory",
                sagaExecution.getId().getValue());

        try {
            @SuppressWarnings("unchecked")
            List<String> reservationIds = (List<String>) sagaExecution.getContextData("reservationIds");

            if (reservationIds != null && !reservationIds.isEmpty()) {
                ReleaseInventoryCommand command = ReleaseInventoryCommand.create(
                    reservationIds.stream()
                        .map(ReservationId::of)
                        .collect(Collectors.toList()),
                    "SAGA compensation"
                );

                CompletableFuture<InventoryReleaseResult> future =
                    inventoryService.releaseInventoryAsync(command);

                future.whenComplete((result, throwable) -> {
                    if (throwable != null || !result.isSuccessful()) {
                        log.warn("SAGA {}: Inventory release failed, continuing with order cancellation",
                                sagaExecution.getId().getValue());
                    }

                    // Final step: Cancel order
                    executeCompensation_CancelOrder(
                        sagaExecution,
                        (String) sagaExecution.getContextData("compensationReason")
                    );
                });
            } else {
                // No inventory to release, proceed to order cancellation
                executeCompensation_CancelOrder(
                    sagaExecution,
                    (String) sagaExecution.getContextData("compensationReason")
                );
            }

        } catch (Exception e) {
            log.error("SAGA {}: Error releasing inventory",
                     sagaExecution.getId().getValue(), e);

            // Continue with order cancellation
            executeCompensation_CancelOrder(sagaExecution, "Inventory release error: " + e.getMessage());
        }
    }

    /**
     * Compensation: Cancel Order
     */
    private void executeCompensation_CancelOrder(SagaExecution sagaExecution, String reason) {
        log.info("SAGA {}: Executing compensation - Cancel Order",
                sagaExecution.getId().getValue());

        try {
            CancelOrderCommand command = CancelOrderCommand.create(
                sagaExecution.getOrderId(),
                CancellationReason.SAGA_COMPENSATION,
                reason,
                "SAGA-ORCHESTRATOR"
            );

            CompletableFuture<OrderCancellationResult> future =
                orderService.cancelOrderAsync(command);

            future.whenComplete((result, throwable) -> {
                if (throwable != null || !result.isSuccessful()) {
                    log.error("SAGA {}: Order cancellation failed",
                             sagaExecution.getId().getValue());

                    sagaExecution.fail("Compensation failed: Order cancellation error");
                } else {
                    log.info("SAGA {}: Compensation completed successfully",
                            sagaExecution.getId().getValue());

                    sagaExecution.compensate();
                }

                sagaExecutionRepository.save(sagaExecution);

                // Publish SAGA completed event
                SagaCompletedEvent completedEvent = new SagaCompletedEvent(
                    sagaExecution.getId(),
                    sagaExecution.getOrderId(),
                    sagaExecution.getResult(),
                    sagaExecution.getDuration(),
                    LocalDateTime.now()
                );

                sagaEventPublisher.publish(completedEvent);
            });

        } catch (Exception e) {
            log.error("SAGA {}: Error cancelling order",
                     sagaExecution.getId().getValue(), e);

            sagaExecution.fail("Order cancellation error: " + e.getMessage());
            sagaExecutionRepository.save(sagaExecution);
        }
    }

    /**
     * Timeout handler
     */
    private void scheduleTimeoutHandler(
        SagaExecution sagaExecution,
        Duration timeout,
        SagaState expectedState
    ) {
        // Schedule timeout check
        CompletableFuture.delayedExecutor(timeout.toMillis(), TimeUnit.MILLISECONDS)
            .execute(() -> {
                try {
                    // Re-load SAGA execution to get latest state
                    Optional<SagaExecution> currentExecution =
                        sagaExecutionRepository.findById(sagaExecution.getId());

                    if (currentExecution.isPresent()) {
                        SagaExecution current = currentExecution.get();

                        // Check if still in expected state (timeout occurred)
                        if (current.getState() == expectedState) {
                            log.warn("SAGA {}: Timeout occurred in state {}",
                                    current.getId().getValue(), expectedState);

                            startCompensation(current,
                                "Timeout in state: " + expectedState);
                        }
                    }

                } catch (Exception e) {
                    log.error("Error handling SAGA timeout", e);
                }
            });
    }

    // Helper methods for command creation
    private ReserveInventoryCommand createReserveInventoryCommand(SagaExecution sagaExecution) {
        OrderContext orderContext = (OrderContext) sagaExecution.getContextData("orderContext");

        return ReserveInventoryCommand.create(
            sagaExecution.getOrderId(),
            orderContext.getItems().stream()
                .map(item -> InventoryReservationRequest.create(
                    ProductId.of(item.getProductId()),
                    Quantity.of(item.getQuantity()),
                    sagaExecution.getOrderId(),
                    Duration.ofMinutes(30)
                ))
                .collect(Collectors.toList())
        );
    }

    private AuthorizePaymentCommand createAuthorizePaymentCommand(SagaExecution sagaExecution) {
        OrderContext orderContext = (OrderContext) sagaExecution.getContextData("orderContext");

        return AuthorizePaymentCommand.create(
            sagaExecution.getOrderId(),
            orderContext.getCustomerId(),
            orderContext.getTotalAmount(),
            orderContext.getPaymentMethod()
        );
    }

    private ConfirmOrderCommand createConfirmOrderCommand(SagaExecution sagaExecution) {
        String paymentId = (String) sagaExecution.getContextData("paymentId");
        String authCode = (String) sagaExecution.getContextData("authorizationCode");
        @SuppressWarnings("unchecked")
        List<String> reservationIds = (List<String>) sagaExecution.getContextData("reservationIds");

        return ConfirmOrderCommand.create(
            sagaExecution.getOrderId(),
            PaymentConfirmation.create(PaymentId.of(paymentId), authCode),
            InventoryConfirmation.create(
                reservationIds.stream()
                    .map(ReservationId::of)
                    .collect(Collectors.toList())
            )
        );
    }

    private OrderContext createSagaContext(OrderPlacedEvent event) {
        return OrderContext.builder()
            .customerId(CustomerId.of(event.getCustomerId()))
            .items(event.getItems())
            .totalAmount(Money.of(event.getTotalAmount(), event.getCurrency()))
            .paymentMethod(event.getPaymentMethod())
            .shippingAddress(event.getShippingAddress())
            .build();
    }
}
```

## SAGA Execution Model

### SAGA State Management

```java
// domain/model/SagaExecution.java
package com.example.saga.domain.model;

import com.example.saga.domain.valueobject.*;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;

/**
 * SAGA Execution Aggregate
 *
 * SAGA instance'ının state'ini ve execution context'ini manage eder.
 */
@Entity
@Table(name = "saga_executions")
public class SagaExecution {

    @Id
    @Column(name = "id")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_type")
    private SagaType sagaType;

    @Column(name = "order_id")
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private SagaState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "result")
    private SagaResult result;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "context_data", columnDefinition = "TEXT")
    private String contextDataJson;

    @Transient
    private Map<String, Object> contextData = new HashMap<>();

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "retry_count")
    private int retryCount = 0;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    protected SagaExecution() {} // JPA constructor

    private SagaExecution(
        SagaId id,
        SagaType sagaType,
        OrderId orderId,
        SagaState state,
        Map<String, Object> contextData
    ) {
        this.id = id.getValue();
        this.sagaType = sagaType;
        this.orderId = orderId.getValue();
        this.state = state;
        this.result = SagaResult.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
        this.contextData = new HashMap<>(contextData);
        this.retryCount = 0;
    }

    public static SagaExecution start(
        SagaId id,
        SagaType sagaType,
        OrderId orderId,
        SagaState initialState,
        Object context
    ) {
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("orderContext", context);

        return new SagaExecution(id, sagaType, orderId, initialState, contextData);
    }

    /**
     * State transition
     */
    public void moveToState(SagaState newState) {
        if (!this.state.canTransitionTo(newState)) {
            throw new IllegalSagaStateTransitionException(
                String.format("Cannot transition from %s to %s", this.state, newState)
            );
        }

        this.state = newState;
        this.lastActivityAt = LocalDateTime.now();

        log.debug("SAGA {}: State changed to {}", this.id, newState);
    }

    /**
     * SAGA'yı başarıyla tamamla
     */
    public void complete() {
        if (!this.state.canTransitionTo(SagaState.COMPLETED)) {
            throw new IllegalSagaStateTransitionException(
                "SAGA cannot be completed from state: " + this.state
            );
        }

        this.state = SagaState.COMPLETED;
        this.result = SagaResult.SUCCESS;
        this.completedAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();

        log.info("SAGA {}: Completed successfully", this.id);
    }

    /**
     * SAGA'yı compensation ile tamamla
     */
    public void compensate() {
        if (!this.state.canTransitionTo(SagaState.COMPENSATED)) {
            throw new IllegalSagaStateTransitionException(
                "SAGA cannot be compensated from state: " + this.state
            );
        }

        this.state = SagaState.COMPENSATED;
        this.result = SagaResult.COMPENSATED;
        this.completedAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();

        log.info("SAGA {}: Compensated successfully", this.id);
    }

    /**
     * SAGA'yı fail et
     */
    public void fail(String errorMessage) {
        this.state = SagaState.FAILED;
        this.result = SagaResult.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();

        log.error("SAGA {}: Failed - {}", this.id, errorMessage);
    }

    /**
     * Context data management
     */
    public void addContextData(String key, Object value) {
        this.contextData.put(key, value);
        this.lastActivityAt = LocalDateTime.now();
    }

    public Object getContextData(String key) {
        return this.contextData.get(key);
    }

    public boolean hasContextData(String key) {
        return this.contextData.containsKey(key);
    }

    /**
     * Retry management
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.lastActivityAt = LocalDateTime.now();
    }

    public boolean canRetry(int maxRetries) {
        return this.retryCount < maxRetries;
    }

    /**
     * Timeout detection
     */
    public boolean isTimedOut(Duration maxDuration) {
        return Duration.between(this.startedAt, LocalDateTime.now())
                .compareTo(maxDuration) > 0;
    }

    public boolean isInactive(Duration inactivityThreshold) {
        return Duration.between(this.lastActivityAt, LocalDateTime.now())
                .compareTo(inactivityThreshold) > 0;
    }

    /**
     * Duration calculation
     */
    public Duration getDuration() {
        LocalDateTime endTime = this.completedAt != null ? this.completedAt : LocalDateTime.now();
        return Duration.between(this.startedAt, endTime);
    }

    // Getters
    public SagaId getId() { return SagaId.of(this.id); }
    public SagaType getSagaType() { return this.sagaType; }
    public OrderId getOrderId() { return OrderId.of(this.orderId); }
    public SagaState getState() { return this.state; }
    public SagaResult getResult() { return this.result; }
    public LocalDateTime getStartedAt() { return this.startedAt; }
    public LocalDateTime getCompletedAt() { return this.completedAt; }
    public LocalDateTime getLastActivityAt() { return this.lastActivityAt; }
    public String getErrorMessage() { return this.errorMessage; }
    public int getRetryCount() { return this.retryCount; }

    // JPA lifecycle methods for context data serialization
    @PrePersist
    @PreUpdate
    private void serializeContextData() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.contextDataJson = mapper.writeValueAsString(this.contextData);
        } catch (Exception e) {
            log.error("Error serializing SAGA context data", e);
        }
    }

    @PostLoad
    private void deserializeContextData() {
        try {
            if (this.contextDataJson != null) {
                ObjectMapper mapper = new ObjectMapper();
                TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
                this.contextData = mapper.readValue(this.contextDataJson, typeRef);
            }
        } catch (Exception e) {
            log.error("Error deserializing SAGA context data", e);
            this.contextData = new HashMap<>();
        }
    }
}
```

### SAGA State Enum

```java
// domain/valueobject/SagaState.java
package com.example.saga.domain.valueobject;

/**
 * SAGA execution states
 */
public enum SagaState {
    STARTED("SAGA started"),
    RESERVING_INVENTORY("Reserving inventory"),
    AUTHORIZING_PAYMENT("Authorizing payment"),
    CONFIRMING_ORDER("Confirming order"),
    COMPENSATING("Executing compensation"),
    COMPLETED("Successfully completed"),
    COMPENSATED("Compensated successfully"),
    FAILED("Failed");

    private final String description;

    SagaState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Valid state transitions
     */
    public boolean canTransitionTo(SagaState newState) {
        return switch (this) {
            case STARTED -> newState == RESERVING_INVENTORY || newState == FAILED;
            case RESERVING_INVENTORY -> newState == AUTHORIZING_PAYMENT ||
                                       newState == COMPENSATING ||
                                       newState == FAILED;
            case AUTHORIZING_PAYMENT -> newState == CONFIRMING_ORDER ||
                                       newState == COMPENSATING ||
                                       newState == FAILED;
            case CONFIRMING_ORDER -> newState == COMPLETED ||
                                    newState == COMPENSATING ||
                                    newState == FAILED;
            case COMPENSATING -> newState == COMPENSATED || newState == FAILED;
            default -> false; // Final states cannot transition
        };
    }

    public boolean isFinalState() {
        return this == COMPLETED || this == COMPENSATED || this == FAILED;
    }

    public boolean isActiveState() {
        return !isFinalState();
    }

    public boolean requiresCompensation() {
        return this == AUTHORIZING_PAYMENT ||
               this == CONFIRMING_ORDER ||
               this == COMPENSATING;
    }
}
```

---

## 🔄 Yapılan İşlemler & Mimari Açıklamalar

### 1. **Orchestration SAGA Architecture**

```ascii
┌─────────────────────────────────────────────────────────────────────┐
│                      ORCHESTRATION SAGA FLOW                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│                    ┌───────────────────────┐                       │
│                    │   SAGA Orchestrator   │                       │
│                    │                       │                       │
│                    │ • State Management    │                       │
│                    │ • Command Dispatch    │                       │
│                    │ • Error Handling      │                       │
│                    │ • Timeout Management  │                       │
│                    └───┬───────────────┬───┘                       │
│                        │               │                           │
│              ┌─────────▼───┐     ┌─────▼───────┐                   │
│              │   Step 1    │     │   Step 2    │                   │
│              │ Reserve     │────▶│ Authorize   │                   │
│              │ Inventory   │     │ Payment     │                   │
│              └─────────────┘     └─────────────┘                   │
│                        │               │                           │
│                        ▼               ▼                           │
│              ┌─────────────┐     ┌─────────────┐                   │
│              │ Inventory   │     │  Payment    │                   │
│              │  Service    │     │  Service    │                   │
│              └─────────────┘     └─────────────┘                   │
│                                                                     │
│  COMPENSATION FLOW (Bottom-up):                                    │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ 1. Void Payment → 2. Release Inventory → 3. Cancel Order  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

**💡 Orchestration Avantajları:**

- **Central Control**: Business process'in tüm kontrolü tek yerde
- **Easy Debugging**: SAGA execution'ı kolayca trace edilebilir
- **State Management**: Complex state machine'ler implement edilebilir
- **Error Handling**: Merkezi error handling ve retry logic

**⚠️ Orchestration Dezavantajları:**

- **Single Point of Failure**: Orchestrator down olursa tüm SAGA'lar durur
- **Tight Coupling**: Service'ler orchestrator'a depend eder
- **Scalability**: Orchestrator bottleneck olabilir
- **Complexity**: Orchestrator logic karmaşık olabilir

### 2. **SAGA State Machine Pattern**

```java
// State transitions with business rules
STARTED → RESERVING_INVENTORY (always)
RESERVING_INVENTORY → AUTHORIZING_PAYMENT (success) | COMPENSATING (failure)
AUTHORIZING_PAYMENT → CONFIRMING_ORDER (success) | COMPENSATING (failure)
CONFIRMING_ORDER → COMPLETED (success) | COMPENSATING (failure)
COMPENSATING → COMPENSATED (success) | FAILED (error)
```

### 3. **Timeout & Recovery Mechanisms**

Her SAGA step için:

- **Step Timeout**: Belirli süre sonra compensation başlar
- **Retry Logic**: Transient error'lar için automatic retry
- **Dead Letter**: Kalıcı failure'lar için manual intervention
- **Health Check**: SAGA Orchestrator health monitoring

---

Bu Orchestration SAGA implementation'ı ile distributed transaction'ları güvenilir şekilde manage edebilir, complex business workflow'ları handle edebilir ve robust error recovery sağlayabiliriz! 🎯

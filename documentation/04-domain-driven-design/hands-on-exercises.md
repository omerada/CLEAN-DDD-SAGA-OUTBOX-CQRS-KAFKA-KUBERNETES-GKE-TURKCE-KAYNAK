# 🎯 DDD Hands-On Exercises - Practical Implementation

## Exercise 1: Order Context Implementation

### Task: Complete Order Aggregate

Implement the complete Order aggregate with all business rules and policies.

**Requirements:**

- Order must enforce all business invariants
- Proper domain event publishing
- Policy-based business rule validation
- Complete state machine implementation

### Implementation Steps

#### Step 1: Create Order Aggregate Root

```java
// src/main/java/com/example/order/domain/model/Order.java
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "customer_id")
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;

    @Embedded
    private Money totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @Embedded
    private ShippingAddress shippingAddress;

    @Column(name = "placed_at")
    private LocalDateTime placedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    // Domain events (not persisted)
    @Transient
    private List<DomainEvent> domainEvents = new ArrayList<>();

    // Business logic methods
    public static Order placeOrder(
        CustomerId customerId,
        List<OrderItem> items,
        ShippingAddress shippingAddress,
        OrderPolicy orderPolicy
    ) {
        // TODO: Implement order placement logic
        // 1. Validate input parameters
        // 2. Create new order
        // 3. Apply business policies
        // 4. Calculate total amount
        // 5. Publish OrderPlacedEvent

        return null; // Placeholder
    }

    public void confirm(PaymentConfirmation paymentConfirmation,
                       InventoryConfirmation inventoryConfirmation) {
        // TODO: Implement order confirmation logic
        // 1. Validate current status
        // 2. Verify payment and inventory confirmations
        // 3. Change status to CONFIRMED
        // 4. Publish OrderConfirmedEvent
    }

    public OrderCancellationResult cancel(CancellationReason reason, String requestedBy) {
        // TODO: Implement order cancellation logic
        // 1. Check cancellation policy
        // 2. Calculate cancellation fees
        // 3. Change status if allowed
        // 4. Publish OrderCancelledEvent

        return null; // Placeholder
    }

    // Add getters, equals, hashCode
}
```

#### Step 2: Create OrderItem Entity

```java
// src/main/java/com/example/order/domain/model/OrderItem.java
@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "product_id")
    private String productId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "quantity"))
    })
    private Quantity quantity;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "unit_price")),
        @AttributeOverride(name = "currency", column = @Column(name = "unit_price_currency"))
    })
    private Money unitPrice;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "subtotal")),
        @AttributeOverride(name = "currency", column = @Column(name = "subtotal_currency"))
    })
    private Money subtotal;

    public static OrderItem create(ProductId productId, Quantity quantity, Money unitPrice) {
        // TODO: Implement OrderItem creation
        // 1. Validate input parameters
        // 2. Calculate subtotal
        // 3. Return new OrderItem

        return null; // Placeholder
    }

    public OrderItem increaseQuantity(Quantity additionalQuantity) {
        // TODO: Implement quantity increase (immutable)

        return null; // Placeholder
    }

    // Add getters, equals, hashCode
}
```

#### Step 3: Implement Value Objects

```java
// src/main/java/com/example/order/domain/valueobject/Money.java
@Embeddable
public class Money {
    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency")
    private String currency;

    protected Money() {} // JPA constructor

    private Money(BigDecimal amount, String currency) {
        // TODO: Implement validation and initialization
    }

    public static Money of(BigDecimal amount, String currency) {
        // TODO: Implement factory method

        return null; // Placeholder
    }

    public Money add(Money other) {
        // TODO: Implement addition with currency validation

        return null; // Placeholder
    }

    public Money subtract(Money other) {
        // TODO: Implement subtraction with validation

        return null; // Placeholder
    }

    public Money multiply(double multiplier) {
        // TODO: Implement multiplication

        return null; // Placeholder
    }

    public boolean isGreaterThan(Money other) {
        // TODO: Implement comparison

        return false; // Placeholder
    }

    // Add other methods, equals, hashCode
}
```

#### Step 4: Create Domain Events

```java
// src/main/java/com/example/order/domain/event/OrderPlacedEvent.java
public class OrderPlacedEvent implements DomainEvent {
    private final String eventId;
    private final String orderId;
    private final String customerId;
    private final BigDecimal totalAmount;
    private final String currency;
    private final List<String> productIds;
    private final LocalDateTime occurredAt;

    public OrderPlacedEvent(
        String orderId,
        String customerId,
        Money totalAmount,
        List<ProductId> productIds,
        LocalDateTime occurredAt
    ) {
        this.eventId = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount.getAmount();
        this.currency = totalAmount.getCurrency().getCurrencyCode();
        this.productIds = productIds.stream()
            .map(ProductId::getValue)
            .collect(Collectors.toList());
        this.occurredAt = occurredAt;
    }

    @Override
    public String getEventId() { return eventId; }

    @Override
    public LocalDateTime getOccurredAt() { return occurredAt; }

    @Override
    public String getAggregateId() { return orderId; }

    @Override
    public String getEventType() { return "OrderPlaced"; }

    // Getters
}
```

#### Step 5: Implement Repository Interface

```java
// src/main/java/com/example/order/domain/repository/OrderRepository.java
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId orderId);
    List<Order> findByCustomerId(CustomerId customerId);
    List<Order> findByStatus(OrderStatus status);
    boolean existsById(OrderId orderId);
    void delete(OrderId orderId);
    OrderId nextId();
}
```

### Verification Checklist

**Domain Model Completeness:**

- [ ] Order aggregate enforces all business invariants
- [ ] OrderItem entity properly validates data
- [ ] Value objects are immutable and self-validating
- [ ] Domain events capture all state changes

**Business Logic Implementation:**

- [ ] Order placement validates all input parameters
- [ ] Order confirmation requires payment and inventory validation
- [ ] Order cancellation follows business policies
- [ ] Status transitions follow state machine rules

**Domain Events:**

- [ ] OrderPlacedEvent published on order creation
- [ ] OrderConfirmedEvent published on confirmation
- [ ] OrderCancelledEvent published on cancellation
- [ ] Events contain all necessary information

---

## Exercise 2: Inventory Context Implementation

### Task: Complete Inventory Aggregate

Implement stock reservation and management with expiration handling.

**Requirements:**

- Stock reservations with automatic expiration
- Concurrent reservation handling
- Low stock alerts
- Stock movement audit trail

### Implementation Template

```java
// src/main/java/com/example/inventory/domain/model/Inventory.java
@Entity
@Table(name = "inventory")
public class Inventory {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "current_stock")
    private int currentStock;

    @Column(name = "reserved_stock")
    private int reservedStock;

    @Column(name = "available_stock")
    private int availableStock;

    @Column(name = "reorder_point")
    private int reorderPoint;

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL)
    private List<StockReservation> activeReservations = new ArrayList<>();

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL)
    private List<StockMovement> movements = new ArrayList<>();

    @Transient
    private List<DomainEvent> domainEvents = new ArrayList<>();

    public ReservationResult reserveStock(
        Quantity requestedQuantity,
        OrderId orderId,
        Duration reservationDuration
    ) {
        // TODO: Implement stock reservation
        // 1. Validate available stock
        // 2. Create reservation
        // 3. Update stock levels
        // 4. Publish StockReservedEvent
        // 5. Check for low stock alert

        return null; // Placeholder
    }

    public void confirmReservation(ReservationId reservationId) {
        // TODO: Implement reservation confirmation
        // 1. Find active reservation
        // 2. Convert to allocation
        // 3. Update stock levels
        // 4. Publish StockAllocatedEvent
    }

    public void cancelReservation(ReservationId reservationId, String reason) {
        // TODO: Implement reservation cancellation
        // 1. Find active reservation
        // 2. Release reserved stock
        // 3. Update stock levels
        // 4. Publish ReservationCancelledEvent
    }

    public List<ReservationId> processExpiredReservations() {
        // TODO: Implement expired reservation processing
        // 1. Find expired reservations
        // 2. Release stock for each
        // 3. Update stock levels
        // 4. Publish ReservationExpiredEvents

        return Collections.emptyList(); // Placeholder
    }

    // Add other methods
}
```

### Test Implementation

```java
// src/test/java/com/example/inventory/domain/model/InventoryTest.java
@ExtendWith(MockitoExtension.class)
class InventoryTest {

    @Test
    void shouldReserveStockSuccessfully() {
        // Given
        Inventory inventory = Inventory.initialize(
            ProductId.of("PROD-123"),
            StockLevel.of(Quantity.of(100)),
            ReorderPoint.of(Quantity.of(10)),
            new DefaultInventoryPolicy()
        );

        // When
        ReservationResult result = inventory.reserveStock(
            Quantity.of(5),
            OrderId.of("ORDER-123"),
            Duration.ofMinutes(30)
        );

        // Then
        assertThat(result.isSuccessful()).isTrue();
        assertThat(inventory.getAvailableStock().getQuantity().getValue()).isEqualTo(95);
        assertThat(inventory.getReservedStock().getQuantity().getValue()).isEqualTo(5);
        assertThat(inventory.getActiveReservations()).hasSize(1);
    }

    @Test
    void shouldFailReservationWhenInsufficientStock() {
        // Given
        Inventory inventory = Inventory.initialize(
            ProductId.of("PROD-123"),
            StockLevel.of(Quantity.of(5)),
            ReorderPoint.of(Quantity.of(10)),
            new DefaultInventoryPolicy()
        );

        // When
        ReservationResult result = inventory.reserveStock(
            Quantity.of(10),
            OrderId.of("ORDER-123"),
            Duration.ofMinutes(30)
        );

        // Then
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getFailureReason()).contains("Insufficient stock");
    }

    @Test
    void shouldProcessExpiredReservations() {
        // TODO: Implement test for expired reservation processing
    }

    @Test
    void shouldTriggerLowStockAlert() {
        // TODO: Implement test for low stock alert
    }
}
```

---

## Exercise 3: Payment Context Implementation

### Task: Complete Payment Aggregate

Implement payment state machine with fraud detection and policies.

**Requirements:**

- Payment authorization and capture flow
- Fraud detection integration
- Refund and void operations
- Payment policies enforcement

### Implementation Template

```java
// src/main/java/com/example/payment/domain/model/Payment.java
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "customer_id")
    private String customerId;

    @Embedded
    private Money amount;

    @Embedded
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL)
    private List<PaymentTransaction> transactions = new ArrayList<>();

    @Transient
    private List<DomainEvent> domainEvents = new ArrayList<>();

    public PaymentAuthorizationResult authorize(
        PaymentGatewayResult gatewayResult,
        RiskAssessment riskAssessment
    ) {
        // TODO: Implement payment authorization
        // 1. Validate current status
        // 2. Check fraud assessment
        // 3. Apply payment policies
        // 4. Update status and publish event

        return null; // Placeholder
    }

    public PaymentCaptureResult capture(Money captureAmount, String reason) {
        // TODO: Implement payment capture
        // 1. Validate capture amount
        // 2. Check capture policies
        // 3. Update status and publish event

        return null; // Placeholder
    }

    public PaymentRefundResult refund(Money refundAmount, String reason, String requestedBy) {
        // TODO: Implement payment refund
        // 1. Validate refund amount
        // 2. Check refund policies
        // 3. Calculate fees
        // 4. Update status and publish event

        return null; // Placeholder
    }

    // Add other methods
}
```

---

## Exercise 4: Cross-Context Integration

### Task: Implement Event Handlers

Create event handlers for cross-context coordination.

**Requirements:**

- Order events trigger inventory and payment actions
- Inventory events update order status
- Payment events confirm or cancel orders

### Implementation Template

```java
// src/main/java/com/example/order/application/handler/OrderEventHandler.java
@Component
@Slf4j
public class OrderEventHandler {

    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    @EventHandler
    public void handle(OrderPlacedEvent event) {
        log.info("Handling OrderPlacedEvent for order: {}", event.getOrderId());

        try {
            // Reserve inventory for order items
            List<InventoryReservationRequest> reservationRequests =
                createInventoryReservationRequests(event);

            InventoryReservationResult reservationResult =
                inventoryService.reserveMultipleProducts(reservationRequests);

            if (reservationResult.isSuccessful()) {
                // Authorize payment
                PaymentAuthorizationRequest authRequest =
                    createPaymentAuthorizationRequest(event);

                PaymentAuthorizationResult authResult =
                    paymentService.authorizePayment(authRequest);

                if (!authResult.isSuccessful()) {
                    // Release inventory reservations
                    inventoryService.cancelReservations(
                        reservationResult.getReservationIds()
                    );
                }
            }

        } catch (Exception e) {
            log.error("Error handling OrderPlacedEvent", e);
            // Implement compensation logic
        }
    }

    @EventHandler
    public void handle(StockReservedEvent event) {
        // TODO: Update order with inventory confirmation
    }

    @EventHandler
    public void handle(PaymentAuthorizedEvent event) {
        // TODO: Confirm order when both inventory and payment are ready
    }

    @EventHandler
    public void handle(PaymentAuthorizationFailedEvent event) {
        // TODO: Cancel order and release inventory
    }

    private List<InventoryReservationRequest> createInventoryReservationRequests(
        OrderPlacedEvent event
    ) {
        // TODO: Convert order items to reservation requests
        return Collections.emptyList();
    }

    private PaymentAuthorizationRequest createPaymentAuthorizationRequest(
        OrderPlacedEvent event
    ) {
        // TODO: Create payment authorization request
        return null;
    }
}
```

---

## Exercise 5: Integration Testing

### Task: End-to-End Order Processing Test

Create integration test that verifies complete order flow.

```java
// src/test/java/com/example/integration/OrderProcessingIntegrationTest.java
@SpringBootTest
@Transactional
@TestMethodOrder(OrderAnnotation.class)
class OrderProcessingIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private TestEventCaptor testEventCaptor;

    @Test
    @Order(1)
    void shouldProcessCompleteOrderSuccessfully() {
        // Given: Setup inventory and customer
        ProductId productId = ProductId.of("LAPTOP-123");
        inventoryService.initializeInventory(
            productId,
            StockLevel.of(Quantity.of(10)),
            ReorderPoint.of(Quantity.of(2))
        );

        CustomerId customerId = CustomerId.of("CUSTOMER-123");
        PaymentMethod paymentMethod = PaymentMethod.creditCard(
            "**** **** **** 1234",
            "1234",
            "12",
            "2025",
            "John Doe",
            BillingAddress.create("123 Main St", "City", "State", "12345", "US"),
            false
        );

        // When: Place order
        PlaceOrderRequest orderRequest = PlaceOrderRequest.builder()
            .customerId(customerId)
            .items(List.of(
                OrderItemRequest.builder()
                    .productId(productId)
                    .quantity(Quantity.of(2))
                    .unitPrice(Money.usd(BigDecimal.valueOf(999.99)))
                    .build()
            ))
            .shippingAddress(ShippingAddress.create(
                "456 Oak Ave", "City", "State", "54321", "US"
            ))
            .paymentMethod(paymentMethod)
            .build();

        PlaceOrderResult result = orderService.placeOrder(orderRequest);

        // Then: Verify order placed successfully
        assertThat(result.isSuccessful()).isTrue();

        OrderId orderId = result.getOrderId();

        // Verify events were published
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(testEventCaptor.getEventsByType(OrderPlacedEvent.class)).hasSize(1);
            assertThat(testEventCaptor.getEventsByType(StockReservedEvent.class)).hasSize(1);
            assertThat(testEventCaptor.getEventsByType(PaymentAuthorizedEvent.class)).hasSize(1);
            assertThat(testEventCaptor.getEventsByType(OrderConfirmedEvent.class)).hasSize(1);
        });

        // Verify final state
        Order order = orderService.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // Verify inventory updated
        Inventory inventory = inventoryService.findByProductId(productId).orElseThrow();
        assertThat(inventory.getAvailableStock().getQuantity().getValue()).isEqualTo(8);
        assertThat(inventory.getReservedStock().getQuantity().getValue()).isEqualTo(0);

        // Verify payment status
        Payment payment = paymentService.findByOrderId(orderId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    @Test
    @Order(2)
    void shouldHandleInsufficientInventoryGracefully() {
        // TODO: Test order failure due to insufficient inventory
    }

    @Test
    @Order(3)
    void shouldHandlePaymentFailureGracefully() {
        // TODO: Test order failure due to payment authorization failure
    }

    @Test
    @Order(4)
    void shouldProcessOrderCancellationCorrectly() {
        // TODO: Test order cancellation and compensation
    }
}
```

---

## Exercise Solutions

### Solution Guidelines

**Exercise 1 Solution Points:**

- Order aggregate must validate business rules in constructor and methods
- Domain events should be collected and published by infrastructure
- OrderItem calculations must be performed in domain layer
- Status transitions should follow state machine pattern

**Exercise 2 Solution Points:**

- Inventory reservations must handle concurrent access safely
- Stock levels must always be consistent (current = available + reserved)
- Expired reservations should be processed automatically
- Low stock alerts should be triggered based on business thresholds

**Exercise 3 Solution Points:**

- Payment state machine must enforce valid transitions
- Fraud assessment should influence authorization decisions
- Refund amounts cannot exceed captured amounts
- Transaction history must be maintained for audit

**Exercise 4 Solution Points:**

- Event handlers should be idempotent and handle failures gracefully
- Compensation logic should release resources on failures
- Event ordering may matter for some operations
- Saga pattern may be needed for complex flows

**Exercise 5 Solution Points:**

- Integration tests should use real Spring context
- Event verification requires eventual consistency testing
- Database state should be verified after async processing
- Error scenarios should test compensation logic

---

## Evaluation Criteria

### Domain Modeling (25 points)

- [ ] Aggregates properly designed with clear boundaries
- [ ] Entities and value objects correctly implemented
- [ ] Business rules enforced in domain layer
- [ ] Domain events capture important state changes

### Business Logic (25 points)

- [ ] Complex business rules properly encapsulated
- [ ] Policies pattern used for configurable rules
- [ ] State machines implemented correctly
- [ ] Validation logic in appropriate layers

### Technical Implementation (25 points)

- [ ] Clean code principles followed
- [ ] Proper error handling and exceptions
- [ ] Performance considerations addressed
- [ ] Testing strategy comprehensive

### Integration & Events (25 points)

- [ ] Cross-context communication via events
- [ ] Event handlers implement compensation logic
- [ ] Integration tests verify end-to-end flows
- [ ] Eventual consistency properly handled

**Minimum Passing Score: 75/100**

---

## Next Steps

After completing these exercises:

1. **🔄 SAGA Pattern** - Long-running business transactions
2. **📤 Outbox Pattern** - Reliable event publishing
3. **📊 CQRS Pattern** - Command Query Responsibility Segregation
4. **🎪 Event Sourcing** - Event-driven state management

### Self-Assessment Questions

1. How do aggregates maintain consistency boundaries?
2. When should you use domain services vs domain methods?
3. How do domain events enable loose coupling?
4. What makes a good bounded context boundary?
5. How do you handle eventual consistency in business flows?

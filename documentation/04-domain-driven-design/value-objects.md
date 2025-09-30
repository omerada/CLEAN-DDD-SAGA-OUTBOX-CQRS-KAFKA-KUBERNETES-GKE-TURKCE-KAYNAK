# 🎯 Domain Value Objects - Core Building Blocks

## Value Objects Implementation

### Money Value Object

```java
// domain/valueobject/Money.java
package com.example.order.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Money Value Object
 *
 * Immutable value object representing monetary amounts with currency.
 * Provides arithmetic operations and validation.
 */
public class Money {
    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.getInstance("USD"));

    private final BigDecimal amount;
    private final Currency currency;

    private Money(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }

        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance("USD"));
    }

    public static Money usd(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("USD"));
    }

    public static Money usd(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance("USD"));
    }

    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        validateSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Subtraction would result in negative amount");
        }
        return new Money(result, this.currency);
    }

    public Money multiply(double multiplier) {
        if (multiplier < 0) {
            throw new IllegalArgumentException("Multiplier cannot be negative");
        }
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }

    public Money divide(double divisor) {
        if (divisor <= 0) {
            throw new IllegalArgumentException("Divisor must be positive");
        }
        return new Money(this.amount.divide(BigDecimal.valueOf(divisor), RoundingMode.HALF_UP), this.currency);
    }

    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegativeOrZero() {
        return this.amount.compareTo(BigDecimal.ZERO) <= 0;
    }

    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: %s vs %s", this.currency, other.currency)
            );
        }
    }

    public BigDecimal getAmount() { return amount; }
    public Currency getCurrency() { return currency; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Objects.equals(amount, money.amount) &&
               Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return String.format("%s %s", currency.getCurrencyCode(), amount);
    }
}
```

### Quantity Value Object

```java
// domain/valueobject/Quantity.java
package com.example.order.domain.valueobject;

import java.util.Objects;

/**
 * Quantity Value Object
 *
 * Represents item quantities with validation.
 * Ensures quantities are always positive integers.
 */
public class Quantity {
    public static final Quantity ZERO = new Quantity(0);
    public static final Quantity ONE = new Quantity(1);

    private final int value;

    private Quantity(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.value = value;
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public Quantity add(Quantity other) {
        return new Quantity(this.value + other.value);
    }

    public Quantity subtract(Quantity other) {
        int result = this.value - other.value;
        if (result < 0) {
            throw new IllegalArgumentException("Subtraction would result in negative quantity");
        }
        return new Quantity(result);
    }

    public Quantity multiply(int multiplier) {
        if (multiplier < 0) {
            throw new IllegalArgumentException("Multiplier cannot be negative");
        }
        return new Quantity(this.value * multiplier);
    }

    public boolean isGreaterThan(Quantity other) {
        return this.value > other.value;
    }

    public boolean isLessThan(Quantity other) {
        return this.value < other.value;
    }

    public boolean isZero() {
        return this.value == 0;
    }

    public boolean isPositive() {
        return this.value > 0;
    }

    public boolean isNegativeOrZero() {
        return this.value <= 0;
    }

    public int getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quantity quantity = (Quantity) o;
        return value == quantity.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
```

### ID Value Objects

```java
// domain/valueobject/OrderId.java
package com.example.order.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Order ID Value Object
 *
 * Strongly typed identifier for Order aggregates.
 * Prevents ID confusion between different entity types.
 */
public class OrderId {
    private final String value;

    private OrderId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        this.value = value.trim();
    }

    public static OrderId generate() {
        return new OrderId("ORDER-" + UUID.randomUUID().toString());
    }

    public static OrderId of(String value) {
        return new OrderId(value);
    }

    public String getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderId orderId = (OrderId) o;
        return Objects.equals(value, orderId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
```

```java
// domain/valueobject/ProductId.java
package com.example.order.domain.valueobject;

import java.util.Objects;

/**
 * Product ID Value Object
 *
 * Strongly typed identifier for products.
 * Shared across different bounded contexts.
 */
public class ProductId {
    private final String value;

    private ProductId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }
        if (!value.matches("^[A-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Product ID must contain only uppercase letters, numbers, underscore and dash");
        }
        this.value = value.trim().toUpperCase();
    }

    public static ProductId of(String value) {
        return new ProductId(value);
    }

    public String getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductId productId = (ProductId) o;
        return Objects.equals(value, productId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
```

### Order Status Enum Value Object

```java
// domain/valueobject/OrderStatus.java
package com.example.order.domain.valueobject;

/**
 * Order Status Value Object
 *
 * Represents valid order states with transition rules.
 */
public enum OrderStatus {
    PENDING("Order is placed but not yet confirmed"),
    CONFIRMED("Order is confirmed and payment processed"),
    SHIPPED("Order has been shipped"),
    DELIVERED("Order has been delivered"),
    CANCELLED("Order has been cancelled");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if transition to new status is valid
     */
    public boolean canTransitionTo(OrderStatus newStatus) {
        switch (this) {
            case PENDING:
                return newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED:
                return newStatus == SHIPPED || newStatus == CANCELLED;
            case SHIPPED:
                return newStatus == DELIVERED || newStatus == CANCELLED;
            case DELIVERED:
                return false; // Final state
            case CANCELLED:
                return false; // Final state
            default:
                return false;
        }
    }

    public boolean isFinalState() {
        return this == DELIVERED || this == CANCELLED;
    }

    public boolean isActiveState() {
        return this == PENDING || this == CONFIRMED || this == SHIPPED;
    }
}
```

### Shipping Address Value Object

```java
// domain/valueobject/ShippingAddress.java
package com.example.order.domain.valueobject;

import java.util.Objects;

/**
 * Shipping Address Value Object
 *
 * Immutable address representation with validation.
 */
public class ShippingAddress {
    private final String street;
    private final String city;
    private final String state;
    private final String zipCode;
    private final String country;
    private final boolean expressDelivery;

    private ShippingAddress(
        String street,
        String city,
        String state,
        String zipCode,
        String country,
        boolean expressDelivery
    ) {
        if (street == null || street.trim().isEmpty()) {
            throw new IllegalArgumentException("Street is required");
        }
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City is required");
        }
        if (state == null || state.trim().isEmpty()) {
            throw new IllegalArgumentException("State is required");
        }
        if (zipCode == null || zipCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Zip code is required");
        }
        if (country == null || country.trim().isEmpty()) {
            throw new IllegalArgumentException("Country is required");
        }

        this.street = street.trim();
        this.city = city.trim();
        this.state = state.trim();
        this.zipCode = zipCode.trim();
        this.country = country.trim();
        this.expressDelivery = expressDelivery;
    }

    public static ShippingAddress create(
        String street,
        String city,
        String state,
        String zipCode,
        String country
    ) {
        return new ShippingAddress(street, city, state, zipCode, country, false);
    }

    public static ShippingAddress createWithExpress(
        String street,
        String city,
        String state,
        String zipCode,
        String country
    ) {
        return new ShippingAddress(street, city, state, zipCode, country, true);
    }

    public ShippingAddress withExpressDelivery() {
        return new ShippingAddress(street, city, state, zipCode, country, true);
    }

    public boolean requiresExpressDelivery() {
        return expressDelivery;
    }

    /**
     * Check if address is in metropolitan area (business rule)
     */
    public boolean isMetropolitan() {
        return isInMajorCity(city) || isInMajorMetroArea(state, city);
    }

    /**
     * Calculate shipping zone based on address
     */
    public ShippingZone getShippingZone() {
        if (isMetropolitan()) {
            return ShippingZone.METRO;
        }
        if (isRemoteArea()) {
            return ShippingZone.REMOTE;
        }
        return ShippingZone.STANDARD;
    }

    private boolean isInMajorCity(String city) {
        return Set.of("NEW YORK", "LOS ANGELES", "CHICAGO", "HOUSTON", "PHOENIX").contains(city.toUpperCase());
    }

    private boolean isInMajorMetroArea(String state, String city) {
        // Business logic for metro areas
        return state.equalsIgnoreCase("CA") && city.toUpperCase().contains("SAN");
    }

    private boolean isRemoteArea() {
        // Business logic for remote areas
        return state.equalsIgnoreCase("AK") || state.equalsIgnoreCase("HI");
    }

    public String getStreet() { return street; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getZipCode() { return zipCode; }
    public String getCountry() { return country; }
    public boolean isExpressDelivery() { return expressDelivery; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShippingAddress that = (ShippingAddress) o;
        return expressDelivery == that.expressDelivery &&
               Objects.equals(street, that.street) &&
               Objects.equals(city, that.city) &&
               Objects.equals(state, that.state) &&
               Objects.equals(zipCode, that.zipCode) &&
               Objects.equals(country, that.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, city, state, zipCode, country, expressDelivery);
    }

    @Override
    public String toString() {
        return String.format("%s, %s, %s %s, %s%s",
            street, city, state, zipCode, country,
            expressDelivery ? " (Express)" : ""
        );
    }

    public enum ShippingZone {
        METRO, STANDARD, REMOTE
    }
}
```

## Domain Event Interfaces

```java
// domain/event/DomainEvent.java
package com.example.order.domain.event;

import java.time.LocalDateTime;

/**
 * Base interface for all domain events
 */
public interface DomainEvent {
    String getEventId();
    LocalDateTime getOccurredAt();
    String getAggregateId();
    String getEventType();
}
```

```java
// domain/event/DomainEventPublisher.java
package com.example.order.domain.event;

import java.util.List;

/**
 * Domain Event Publisher Interface
 *
 * Abstracts the event publishing mechanism.
 * Implementations can use message queues, event stores, etc.
 */
public interface DomainEventPublisher {
    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);
}
```

## Repository Interfaces

```java
// domain/repository/OrderRepository.java
package com.example.order.domain.repository;

import com.example.order.domain.model.Order;
import com.example.order.domain.valueobject.*;
import java.util.Optional;
import java.util.List;

/**
 * Order Repository Interface
 *
 * Domain-focused repository interface.
 * Infrastructure layer provides implementation.
 */
public interface OrderRepository {

    /**
     * Save an order aggregate
     */
    Order save(Order order);

    /**
     * Find order by ID
     */
    Optional<Order> findById(OrderId orderId);

    /**
     * Find orders by customer
     */
    List<Order> findByCustomerId(CustomerId customerId);

    /**
     * Find orders by status
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Find orders requiring attention (business query)
     */
    List<Order> findOrdersRequiringAttention();

    /**
     * Check if order exists
     */
    boolean existsById(OrderId orderId);

    /**
     * Delete order (soft delete in practice)
     */
    void delete(OrderId orderId);

    /**
     * Get next order ID (for event sourcing scenarios)
     */
    OrderId nextId();
}
```

## Exception Hierarchy

```java
// domain/exception/OrderDomainException.java
package com.example.order.domain.exception;

/**
 * Base exception for order domain
 */
public abstract class OrderDomainException extends RuntimeException {

    protected OrderDomainException(String message) {
        super(message);
    }

    protected OrderDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Business rule violation exception
 */
public class BusinessRuleViolationException extends OrderDomainException {
    private final String ruleCode;

    public BusinessRuleViolationException(String ruleCode, String message) {
        super(message);
        this.ruleCode = ruleCode;
    }

    public String getRuleCode() { return ruleCode; }
}

/**
 * Invalid order state exception
 */
public class IllegalOrderStateException extends OrderDomainException {
    public IllegalOrderStateException(String message) {
        super(message);
    }
}

/**
 * Order not found exception
 */
public class OrderNotFoundException extends OrderDomainException {
    private final OrderId orderId;

    public OrderNotFoundException(OrderId orderId) {
        super("Order not found: " + orderId.getValue());
        this.orderId = orderId;
    }

    public OrderId getOrderId() { return orderId; }
}
```

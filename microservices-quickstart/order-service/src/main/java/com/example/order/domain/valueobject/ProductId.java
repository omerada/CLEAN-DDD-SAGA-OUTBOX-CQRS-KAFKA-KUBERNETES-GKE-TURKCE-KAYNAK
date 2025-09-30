package com.example.order.domain.valueobject;

import java.util.Objects;

/**
 * Product ID Value Object
 * 
 * References product from Product Catalog Context
 * Contains business rules for product identification
 */
public class ProductId {
    private final String value;

    private ProductId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }
        if (!value.startsWith("PROD-")) {
            throw new IllegalArgumentException("Product ID must follow format: PROD-XXX");
        }
        this.value = value;
    }

    public static ProductId of(String value) {
        return new ProductId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
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
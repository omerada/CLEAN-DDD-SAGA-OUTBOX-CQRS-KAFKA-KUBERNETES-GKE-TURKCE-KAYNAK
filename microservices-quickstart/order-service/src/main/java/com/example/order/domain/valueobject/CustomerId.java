package com.example.order.domain.valueobject;

import java.util.Objects;

/**
 * Customer ID Value Object
 * 
 * Represents customer identifier from Customer Management Context
 * This is a reference to another bounded context
 */
public class CustomerId {
    private final String value;

    private CustomerId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        if (!value.startsWith("CUST-")) {
            throw new IllegalArgumentException("Customer ID must follow format: CUST-XXX");
        }
        this.value = value;
    }

    public static CustomerId of(String value) {
        return new CustomerId(value);
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
        CustomerId that = (CustomerId) o;
        return Objects.equals(value, that.value);
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
package com.example.order.domain.valueobject;

import java.util.Objects;

/**
 * Quantity Value Object
 * 
 * Represents quantity in business domain
 */
public record Quantity(Integer value) {
    
    public Quantity {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (value > 1000) {
            throw new IllegalArgumentException("Quantity cannot exceed 1000");
        }
    }
    
    public static Quantity of(Integer value) {
        return new Quantity(value);
    }
    
    public Integer getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Quantity quantity = (Quantity) obj;
        return Objects.equals(value, quantity.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return "Quantity{" + value + "}";
    }
}
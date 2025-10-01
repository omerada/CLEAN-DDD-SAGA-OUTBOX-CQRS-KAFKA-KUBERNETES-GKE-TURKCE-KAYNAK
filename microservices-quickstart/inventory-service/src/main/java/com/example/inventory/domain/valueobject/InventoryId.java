package com.example.inventory.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Inventory ID Value Object
 */
public class InventoryId {
    private final String value;

    private InventoryId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Inventory ID cannot be null or empty");
        }
        this.value = value;
    }

    public static InventoryId of(String value) {
        return new InventoryId(value);
    }

    public static InventoryId generate() {
        return new InventoryId("INV-" + UUID.randomUUID().toString());
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
        InventoryId that = (InventoryId) o;
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
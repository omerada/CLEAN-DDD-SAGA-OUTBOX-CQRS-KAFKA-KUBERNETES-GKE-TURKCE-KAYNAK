package com.example.inventory.usecase;

import com.example.inventory.domain.valueobject.ProductId;
import com.example.inventory.domain.valueobject.Quantity;

/**
 * Use case for checking available stock
 * 
 * This interface defines the contract for stock checking functionality
 * in the inventory service.
 */
public interface CheckStockUseCase {

    /**
     * Check available stock for a specific product
     * 
     * @param productId the ID of the product to check
     * @return StockCheckResult containing availability information
     */
    StockCheckResult checkStock(ProductId productId);

    /**
     * Check if sufficient stock is available for a given quantity
     * 
     * @param productId        the ID of the product to check
     * @param requiredQuantity the quantity required
     * @return boolean indicating if sufficient stock is available
     */
    boolean isStockAvailable(ProductId productId, Quantity requiredQuantity);

    /**
     * Result of a stock check operation
     */
    public static class StockCheckResult {
        private final ProductId productId;
        private final Quantity availableQuantity;
        private final Quantity reservedQuantity;
        private final boolean inStock;

        public StockCheckResult(ProductId productId, Quantity availableQuantity,
                Quantity reservedQuantity, boolean inStock) {
            this.productId = productId;
            this.availableQuantity = availableQuantity;
            this.reservedQuantity = reservedQuantity;
            this.inStock = inStock;
        }

        public ProductId getProductId() {
            return productId;
        }

        public Quantity getAvailableQuantity() {
            return availableQuantity;
        }

        public Quantity getReservedQuantity() {
            return reservedQuantity;
        }

        public boolean isInStock() {
            return inStock;
        }

        public Quantity getTotalStock() {
            return Quantity.of(availableQuantity.getValue() + reservedQuantity.getValue());
        }
    }
}
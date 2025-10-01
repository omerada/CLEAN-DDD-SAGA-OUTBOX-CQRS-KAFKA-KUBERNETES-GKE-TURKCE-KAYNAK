package com.example.inventory.infrastructure.web;

import com.example.inventory.usecase.*;
import com.example.inventory.domain.valueobject.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * REST Controller for Inventory operations
 * 
 * This controller handles HTTP requests and delegates to use cases
 * following Clean Architecture principles.
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class InventoryRestController {

    private final ReserveStockUseCase reserveStockUseCase;
    private final CheckStockUseCase checkStockUseCase;
    private final ConfirmReservationUseCase confirmReservationUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;

    @GetMapping("/products/{productId}/stock")
    public ResponseEntity<StockCheckResponse> checkStock(@PathVariable String productId) {
        log.info("Checking stock for product: {}", productId);

        try {
            ProductId prodId = ProductId.of(productId);
            CheckStockUseCase.StockCheckResult result = checkStockUseCase.checkStock(prodId);

            StockCheckResponse response = StockCheckResponse.builder()
                    .productId(result.getProductId().getValue())
                    .availableQuantity(result.getAvailableQuantity().getValue())
                    .reservedQuantity(result.getReservedQuantity().getValue())
                    .totalQuantity(result.getTotalStock().getValue())
                    .inStock(result.isInStock())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking stock for product: {}", productId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> reserveStock(@Valid @RequestBody ReserveStockRequest request) {
        log.info("Reserving stock for product: {} quantity: {} order: {}",
                request.productId, request.quantity, request.orderId);

        try {
            ProductId productId = ProductId.of(request.productId);
            Quantity quantity = Quantity.of(request.quantity);

            ReserveStockUseCase.ReservationResult result = reserveStockUseCase.reserveStock(productId, quantity,
                    request.orderId);

            if (result.isSuccess()) {
                ReservationResponse response = ReservationResponse.builder()
                        .success(true)
                        .message(result.getMessage())
                        .reservationId(result.getReservationId().getValue())
                        .reservedQuantity(result.getReservedQuantity().getValue())
                        .build();

                return ResponseEntity.ok(response);
            } else {
                ReservationResponse response = ReservationResponse.builder()
                        .success(false)
                        .message(result.getMessage())
                        .build();

                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error reserving stock", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/reservations/{reservationId}/confirm")
    public ResponseEntity<ConfirmationResponse> confirmReservation(@PathVariable String reservationId) {
        log.info("Confirming reservation: {}", reservationId);

        try {
            ReservationId resId = ReservationId.of(reservationId);
            ConfirmReservationUseCase.ConfirmationResult result = confirmReservationUseCase.confirmReservation(resId);

            ConfirmationResponse response = ConfirmationResponse.builder()
                    .success(result.isSuccess())
                    .message(result.getMessage())
                    .reservationId(reservationId)
                    .build();

            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error confirming reservation: {}", reservationId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/reservations/{reservationId}/cancel")
    public ResponseEntity<CancellationResponse> cancelReservation(
            @PathVariable String reservationId,
            @Valid @RequestBody CancelReservationRequest request) {
        log.info("Cancelling reservation: {} with reason: {}", reservationId, request.reason);

        try {
            ReservationId resId = ReservationId.of(reservationId);
            CancelReservationUseCase.CancellationResult result = cancelReservationUseCase.cancelReservation(resId,
                    request.reason);

            CancellationResponse response = CancellationResponse.builder()
                    .success(result.isSuccess())
                    .message(result.getMessage())
                    .reservationId(reservationId)
                    .reason(request.reason)
                    .build();

            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error cancelling reservation: {}", reservationId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/products/{productId}/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @PathVariable String productId,
            @RequestParam Integer requiredQuantity) {
        log.info("Checking availability for product: {} quantity: {}", productId, requiredQuantity);

        try {
            ProductId prodId = ProductId.of(productId);
            Quantity reqQuantity = Quantity.of(requiredQuantity);

            boolean isAvailable = checkStockUseCase.isStockAvailable(prodId, reqQuantity);

            AvailabilityResponse response = AvailabilityResponse.builder()
                    .productId(productId)
                    .requiredQuantity(requiredQuantity)
                    .available(isAvailable)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking availability for product: {}", productId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Request/Response DTOs

    public static class ReserveStockRequest {
        @NotBlank(message = "Product ID is required")
        public String productId;

        @Positive(message = "Quantity must be positive")
        public Integer quantity;

        @NotBlank(message = "Order ID is required")
        public String orderId;
    }

    public static class CancelReservationRequest {
        @NotBlank(message = "Reason is required")
        public String reason;
    }

    @lombok.Builder
    public static class StockCheckResponse {
        public String productId;
        public Integer availableQuantity;
        public Integer reservedQuantity;
        public Integer totalQuantity;
        public Boolean inStock;
    }

    @lombok.Builder
    public static class ReservationResponse {
        public Boolean success;
        public String message;
        public String reservationId;
        public Integer reservedQuantity;
    }

    @lombok.Builder
    public static class ConfirmationResponse {
        public Boolean success;
        public String message;
        public String reservationId;
    }

    @lombok.Builder
    public static class CancellationResponse {
        public Boolean success;
        public String message;
        public String reservationId;
        public String reason;
    }

    @lombok.Builder
    public static class AvailabilityResponse {
        public String productId;
        public Integer requiredQuantity;
        public Boolean available;
    }
}
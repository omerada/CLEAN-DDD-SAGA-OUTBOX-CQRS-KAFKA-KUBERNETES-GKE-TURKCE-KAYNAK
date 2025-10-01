package com.example.inventory.application.service;

import com.example.inventory.usecase.*;
import com.example.inventory.domain.entity.Inventory;
import com.example.inventory.domain.entity.StockReservation;
import com.example.inventory.domain.valueobject.*;
import com.example.inventory.application.port.out.InventoryRepository;
import com.example.inventory.application.port.out.StockReservationRepository;
import com.example.inventory.application.port.out.EventPublisher;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Application Service implementing inventory use cases
 * 
 * This service orchestrates domain logic and coordinates with infrastructure
 * following Clean Architecture and DDD principles.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InventoryApplicationService implements
        ReserveStockUseCase,
        CheckStockUseCase,
        ConfirmReservationUseCase,
        CancelReservationUseCase {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository stockReservationRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public StockCheckResult checkStock(ProductId productId) {
        log.debug("Checking stock for product: {}", productId.getValue());

        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);

        if (inventoryOpt.isEmpty()) {
            log.warn("Product not found in inventory: {}", productId.getValue());
            return new StockCheckResult(productId, Quantity.of(0), Quantity.of(0), false);
        }

        Inventory inventory = inventoryOpt.get();
        Quantity reservedQuantity = stockReservationRepository.getTotalReservedByProductId(productId);

        log.debug("Stock check result for product {}: available={}, reserved={}",
                productId.getValue(), inventory.getAvailableQuantity().getValue(),
                reservedQuantity.getValue());

        return new StockCheckResult(
                productId,
                inventory.getAvailableQuantity(),
                reservedQuantity,
                inventory.getAvailableQuantity().getValue() > 0);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStockAvailable(ProductId productId, Quantity requiredQuantity) {
        log.debug("Checking if {} units available for product: {}",
                requiredQuantity.getValue(), productId.getValue());

        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);

        if (inventoryOpt.isEmpty()) {
            log.warn("Product not found: {}", productId.getValue());
            return false;
        }

        Inventory inventory = inventoryOpt.get();
        boolean isAvailable = inventory.canReserve(requiredQuantity);

        log.debug("Stock availability check for product {}: required={}, available={}, result={}",
                productId.getValue(), requiredQuantity.getValue(),
                inventory.getAvailableQuantity().getValue(), isAvailable);

        return isAvailable;
    }

    @Override
    public ReservationResult reserveStock(ProductId productId, Quantity quantity, String orderId) {
        log.info("Reserving {} units of product {} for order {}",
                quantity.getValue(), productId.getValue(), orderId);

        try {
            // Find inventory
            Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);

            if (inventoryOpt.isEmpty()) {
                String message = "Product not found: " + productId.getValue();
                log.error(message);
                return ReservationResult.failure(message);
            }

            Inventory inventory = inventoryOpt.get();

            // Check if reservation is possible
            if (!inventory.canReserve(quantity)) {
                String message = "Insufficient stock for product " + productId.getValue() +
                        ". Required: " + quantity.getValue() +
                        ", Available: " + inventory.getAvailableQuantity().getValue();
                log.warn(message);
                return ReservationResult.failure(message);
            }

            // Reserve stock in inventory
            var reservationResult = inventory.reserveStock(quantity, OrderId.of(orderId),
                    LocalDateTime.now().plusMinutes(30));

            // Create reservation record
            StockReservation reservation = StockReservation.create(
                    ReservationId.generate(),
                    productId,
                    quantity,
                    orderId,
                    LocalDateTime.now());

            // Save changes
            inventoryRepository.save(inventory);
            stockReservationRepository.save(reservation);

            // Publish domain events
            inventory.getUncommittedEvents().forEach(eventPublisher::publishEvent);
            reservation.getUncommittedEvents().forEach(eventPublisher::publishEvent);

            // Mark events as committed
            inventory.markEventsAsCommitted();
            reservation.markEventsAsCommitted();

            log.info("Successfully reserved {} units of product {} with reservation ID: {}",
                    quantity.getValue(), productId.getValue(), reservation.getId().getValue());

            return ReservationResult.success(reservation.getId(), quantity);

        } catch (Exception e) {
            String message = "Failed to reserve stock: " + e.getMessage();
            log.error(message, e);
            return ReservationResult.failure(message);
        }
    }

    @Override
    public ConfirmationResult confirmReservation(ReservationId reservationId) {
        log.info("Confirming reservation: {}", reservationId.getValue());

        try {
            // Find reservation
            Optional<StockReservation> reservationOpt = stockReservationRepository.findById(reservationId);

            if (reservationOpt.isEmpty()) {
                String message = "Reservation not found: " + reservationId.getValue();
                log.error(message);
                return ConfirmationResult.failure(message);
            }

            StockReservation reservation = reservationOpt.get();

            // Check if reservation can be confirmed
            if (!reservation.canBeConfirmed()) {
                String message = "Reservation cannot be confirmed. Current status: " +
                        reservation.getStatus();
                log.warn(message);
                return ConfirmationResult.failure(message);
            }

            // Find inventory
            Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(reservation.getProductId());

            if (inventoryOpt.isEmpty()) {
                String message = "Product not found: " + reservation.getProductId().getValue();
                log.error(message);
                return ConfirmationResult.failure(message);
            }

            Inventory inventory = inventoryOpt.get();

            // Confirm reservation
            reservation.confirm();
            inventory.confirmReservation(reservation.getQuantity());

            // Save changes
            stockReservationRepository.save(reservation);
            inventoryRepository.save(inventory);

            // Publish domain events
            reservation.getUncommittedEvents().forEach(eventPublisher::publishEvent);
            inventory.getUncommittedEvents().forEach(eventPublisher::publishEvent);

            // Mark events as committed
            reservation.markEventsAsCommitted();
            inventory.markEventsAsCommitted();

            log.info("Successfully confirmed reservation: {}", reservationId.getValue());

            return ConfirmationResult.success(reservationId);

        } catch (Exception e) {
            String message = "Failed to confirm reservation: " + e.getMessage();
            log.error(message, e);
            return ConfirmationResult.failure(message);
        }
    }

    @Override
    public CancellationResult cancelReservation(ReservationId reservationId, String reason) {
        log.info("Cancelling reservation: {} with reason: {}", reservationId.getValue(), reason);

        try {
            // Find reservation
            Optional<StockReservation> reservationOpt = stockReservationRepository.findById(reservationId);

            if (reservationOpt.isEmpty()) {
                String message = "Reservation not found: " + reservationId.getValue();
                log.error(message);
                return CancellationResult.failure(message);
            }

            StockReservation reservation = reservationOpt.get();

            // Check if reservation can be cancelled
            if (!reservation.canBeCancelled()) {
                String message = "Reservation cannot be cancelled. Current status: " +
                        reservation.getStatus();
                log.warn(message);
                return CancellationResult.failure(message);
            }

            // Find inventory
            Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(reservation.getProductId());

            if (inventoryOpt.isEmpty()) {
                String message = "Product not found: " + reservation.getProductId().getValue();
                log.error(message);
                return CancellationResult.failure(message);
            }

            Inventory inventory = inventoryOpt.get();

            // Cancel reservation
            reservation.cancel(reason);
            inventory.cancelReservation(reservation.getQuantity());

            // Save changes
            stockReservationRepository.save(reservation);
            inventoryRepository.save(inventory);

            // Publish domain events
            reservation.getUncommittedEvents().forEach(eventPublisher::publishEvent);
            inventory.getUncommittedEvents().forEach(eventPublisher::publishEvent);

            // Mark events as committed
            reservation.markEventsAsCommitted();
            inventory.markEventsAsCommitted();

            log.info("Successfully cancelled reservation: {} with reason: {}",
                    reservationId.getValue(), reason);

            return CancellationResult.success(reservationId, reason);

        } catch (Exception e) {
            String message = "Failed to cancel reservation: " + e.getMessage();
            log.error(message, e);
            return CancellationResult.failure(message);
        }
    }
}
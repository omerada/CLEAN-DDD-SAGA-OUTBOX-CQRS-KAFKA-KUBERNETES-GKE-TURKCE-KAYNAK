package com.example.inventory.application.port.out;

import com.example.inventory.domain.entity.StockReservation;
import com.example.inventory.domain.valueobject.ReservationId;
import com.example.inventory.domain.valueobject.ProductId;
import com.example.inventory.domain.valueobject.Quantity;
import com.example.inventory.domain.valueobject.ReservationStatus;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

/**
 * Output port for stock reservation persistence operations
 * 
 * This interface defines the contract for stock reservation data access
 * following the Hexagonal Architecture principles.
 */
public interface StockReservationRepository {

    /**
     * Save a stock reservation entity
     * 
     * @param reservation the reservation to save
     * @return the saved reservation
     */
    StockReservation save(StockReservation reservation);

    /**
     * Find reservation by ID
     * 
     * @param id the reservation ID
     * @return Optional containing the reservation if found
     */
    Optional<StockReservation> findById(ReservationId id);

    /**
     * Find reservations by product ID
     * 
     * @param productId the product ID
     * @return list of reservations for the product
     */
    List<StockReservation> findByProductId(ProductId productId);

    /**
     * Find reservations by order ID
     * 
     * @param orderId the order ID
     * @return list of reservations for the order
     */
    List<StockReservation> findByOrderId(String orderId);

    /**
     * Find reservations by status
     * 
     * @param status the reservation status
     * @return list of reservations with the given status
     */
    List<StockReservation> findByStatus(ReservationStatus status);

    /**
     * Get total reserved quantity for a product
     * 
     * @param productId the product ID
     * @return total reserved quantity
     */
    Quantity getTotalReservedByProductId(ProductId productId);

    /**
     * Find expired reservations
     * 
     * @param expirationTime the time before which reservations are considered
     *                       expired
     * @return list of expired reservations
     */
    List<StockReservation> findExpiredReservations(LocalDateTime expirationTime);

    /**
     * Find all reservations
     * 
     * @return list of all reservations
     */
    List<StockReservation> findAll();

    /**
     * Delete reservation by ID
     * 
     * @param id the reservation ID
     */
    void deleteById(ReservationId id);

    /**
     * Count reservations by status
     * 
     * @param status the reservation status
     * @return count of reservations with the given status
     */
    long countByStatus(ReservationStatus status);
}
package com.example.inventory.infrastructure.persistence;

import com.example.inventory.application.port.out.StockReservationRepository;
import com.example.inventory.domain.entity.StockReservation;
import com.example.inventory.domain.valueobject.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository implementation for StockReservation
 * 
 * This interface extends JpaRepository and implements the
 * StockReservationRepository port
 * providing data access operations for the StockReservation entity.
 */
@Repository
public interface JpaStockReservationRepository
        extends JpaRepository<StockReservation, String>, StockReservationRepository {

    @Override
    default Optional<StockReservation> findById(ReservationId id) {
        return findById(id.getValue());
    }

    @Override
    default List<StockReservation> findByProductId(ProductId productId) {
        return findByInventoryProductId(productId.getValue());
    }

    @Query("SELECT sr FROM StockReservation sr WHERE sr.inventory.productId = :productId")
    List<StockReservation> findByInventoryProductId(@Param("productId") String productId);

    @Override
    @Query("SELECT sr FROM StockReservation sr WHERE sr.orderId = :orderId")
    List<StockReservation> findByOrderId(@Param("orderId") String orderId);

    @Override
    default List<StockReservation> findByStatus(ReservationStatus status) {
        return findByStatusValue(status.name());
    }

    @Query("SELECT sr FROM StockReservation sr WHERE sr.status = :status")
    List<StockReservation> findByStatusValue(@Param("status") String status);

    @Override
    default Quantity getTotalReservedByProductId(ProductId productId) {
        Integer total = getTotalReservedQuantityByProductId(productId.getValue());
        return Quantity.of(total != null ? total : 0);
    }

    @Query("SELECT COALESCE(SUM(sr.quantity), 0) FROM StockReservation sr " +
            "WHERE sr.inventory.productId = :productId AND sr.status = 'ACTIVE'")
    Integer getTotalReservedQuantityByProductId(@Param("productId") String productId);

    @Override
    @Query("SELECT sr FROM StockReservation sr WHERE sr.expiresAt < :expirationTime AND sr.status = 'ACTIVE'")
    List<StockReservation> findExpiredReservations(@Param("expirationTime") LocalDateTime expirationTime);

    @Override
    default void deleteById(ReservationId id) {
        deleteById(id.getValue());
    }

    @Override
    default long countByStatus(ReservationStatus status) {
        return countByStatusValue(status.name());
    }

    @Query("SELECT COUNT(sr) FROM StockReservation sr WHERE sr.status = :status")
    long countByStatusValue(@Param("status") String status);
}
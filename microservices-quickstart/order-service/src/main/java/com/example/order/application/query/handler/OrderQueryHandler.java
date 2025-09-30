package com.example.order.application.query.handler;

import com.example.order.application.query.GetOrdersQuery;
import com.example.order.application.query.model.OrderListView;
import com.example.order.application.query.repository.OrderListViewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Order Query Handler
 * 
 * CQRS Read Side query handler for order queries.
 * Handles read operations with optimized read models.
 */
@Component
@Transactional(readOnly = true)
public class OrderQueryHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderQueryHandler.class);

    private final OrderListViewRepository orderListViewRepository;

    public OrderQueryHandler(OrderListViewRepository orderListViewRepository) {
        this.orderListViewRepository = orderListViewRepository;
    }

    /**
     * Handle get orders query
     */
    public GetOrdersResult handle(GetOrdersQuery query) {
        try {
            log.debug("Processing get orders query for customer: {}",
                    query.customerId() != null ? query.customerId().getValue() : "ALL");

            // Create pageable with sorting by creation date (newest first)
            Pageable pageable = createPageable(query);

            Page<OrderListView> ordersPage;

            if (query.customerId() != null) {
                // Customer-specific orders
                if (query.status() != null) {
                    ordersPage = orderListViewRepository.findByCustomerIdAndStatus(
                            query.customerId().getValue(),
                            query.status(),
                            pageable);
                } else {
                    ordersPage = orderListViewRepository.findByCustomerId(
                            query.customerId().getValue(),
                            pageable);
                }
            } else {
                // All orders (admin view)
                if (query.status() != null) {
                    ordersPage = orderListViewRepository.findByStatus(query.status(), pageable);
                } else {
                    ordersPage = orderListViewRepository.findAll(pageable);
                }
            }

            GetOrdersResult result = new GetOrdersResult(
                    ordersPage.getContent(),
                    ordersPage.getTotalElements(),
                    ordersPage.getTotalPages(),
                    ordersPage.getNumber(),
                    ordersPage.getSize());

            log.debug("Retrieved {} orders for query", ordersPage.getContent().size());

            return result;

        } catch (Exception e) {
            log.error("Error processing get orders query", e);
            throw new RuntimeException("Failed to retrieve orders", e);
        }
    }

    /**
     * Get active orders for customer
     */
    public List<OrderListView> getActiveOrdersForCustomer(String customerId) {
        try {
            log.debug("Getting active orders for customer: {}", customerId);

            List<OrderListView> activeOrders = orderListViewRepository
                    .findActiveOrdersByCustomerId(customerId);

            log.debug("Found {} active orders for customer: {}",
                    activeOrders.size(), customerId);

            return activeOrders;

        } catch (Exception e) {
            log.error("Error getting active orders for customer: {}", customerId, e);
            throw new RuntimeException("Failed to retrieve active orders", e);
        }
    }

    /**
     * Get recent orders with limit
     */
    public List<OrderListView> getRecentOrders(int limit) {
        try {
            log.debug("Getting {} recent orders", limit);

            Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
            List<OrderListView> recentOrders = orderListViewRepository.findRecentOrders(pageable);

            log.debug("Retrieved {} recent orders", recentOrders.size());

            return recentOrders;

        } catch (Exception e) {
            log.error("Error getting recent orders", e);
            throw new RuntimeException("Failed to retrieve recent orders", e);
        }
    }

    /**
     * Create pageable from query parameters
     */
    private Pageable createPageable(GetOrdersQuery query) {
        int page = query.page() != null ? query.page() : 0;
        int size = query.size() != null ? query.size() : 20;

        // Always sort by creation date, newest first
        Sort sort = Sort.by("createdAt").descending();

        return PageRequest.of(page, size, sort);
    }
}
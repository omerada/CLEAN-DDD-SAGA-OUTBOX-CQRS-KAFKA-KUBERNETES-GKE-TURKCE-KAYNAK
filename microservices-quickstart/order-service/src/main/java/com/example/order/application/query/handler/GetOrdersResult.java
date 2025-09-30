package com.example.order.application.query.handler;

import com.example.order.application.query.model.OrderListView;
import java.util.List;

/**
 * Query result for get orders operation
 */
public class GetOrdersResult {
    private final List<OrderListView> orders;
    private final long totalElements;
    private final int totalPages;
    private final int currentPage;
    private final int pageSize;

    public GetOrdersResult(
            List<OrderListView> orders,
            long totalElements,
            int totalPages,
            int currentPage,
            int pageSize) {
        this.orders = orders;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }

    public List<OrderListView> getOrders() {
        return orders;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public boolean isEmpty() {
        return orders.isEmpty();
    }

    public int getSize() {
        return orders.size();
    }
}
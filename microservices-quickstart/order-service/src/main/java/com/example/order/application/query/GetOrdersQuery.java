package com.example.order.application.query;

import com.example.order.domain.valueobject.CustomerId;

/**
 * Get Orders Query
 * 
 * CQRS Read Side Query for retrieving order list
 */
public record GetOrdersQuery(
        CustomerId customerId,
        String status,
        Integer page,
        Integer size) {

    public GetOrdersQuery {
        if (page != null && page < 0) {
            throw new IllegalArgumentException("Page must be non-negative");
        }
        if (size != null && (size <= 0 || size > 100)) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
    }

    public static GetOrdersQuery forCustomer(CustomerId customerId) {
        return new GetOrdersQuery(customerId, null, 0, 20);
    }

    public static GetOrdersQuery forCustomerWithStatus(CustomerId customerId, String status) {
        return new GetOrdersQuery(customerId, status, 0, 20);
    }

    public static GetOrdersQuery all() {
        return new GetOrdersQuery(null, null, 0, 20);
    }

    public GetOrdersQuery withPaging(Integer page, Integer size) {
        return new GetOrdersQuery(customerId, status, page, size);
    }
}
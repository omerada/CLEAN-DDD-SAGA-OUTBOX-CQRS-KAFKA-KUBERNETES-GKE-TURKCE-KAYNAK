package com.example.order.application.command.handler;

import com.example.order.domain.valueobject.OrderId;

/**
 * Command result for create order operation
 */
public class CreateOrderResult {
    private final boolean success;
    private final OrderId orderId;
    private final String errorMessage;

    private CreateOrderResult(boolean success, OrderId orderId, String errorMessage) {
        this.success = success;
        this.orderId = orderId;
        this.errorMessage = errorMessage;
    }

    public static CreateOrderResult success(OrderId orderId) {
        return new CreateOrderResult(true, orderId, null);
    }

    public static CreateOrderResult failure(String errorMessage) {
        return new CreateOrderResult(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
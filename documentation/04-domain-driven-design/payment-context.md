# 🎯 Payment Bounded Context - DDD Implementation

## Payment Aggregate Design

### Payment Aggregate Root

```java
// domain/model/Payment.java
package com.example.payment.domain.model;

import com.example.payment.domain.valueobject.*;
import com.example.payment.domain.event.*;
import com.example.payment.domain.policy.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Payment Aggregate Root
 *
 * Business Rules:
 * - Payment amount must match order total
 * - Payment method must be validated
 * - Fraud detection must be performed
 * - Payment state transitions follow business rules
 * - Refunds cannot exceed original payment amount
 * - Multi-currency payments require exchange rate handling
 */
public class Payment {
    private PaymentId id;
    private OrderId orderId;
    private CustomerId customerId;
    private Money amount;
    private Currency currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private List<PaymentTransaction> transactions;
    private FraudAssessment fraudAssessment;
    private PaymentPolicy paymentPolicy;
    private LocalDateTime initiatedAt;
    private LocalDateTime authorizedAt;
    private LocalDateTime capturedAt;
    private LocalDateTime settledAt;
    private String gatewayReference;

    // Domain events
    private List<DomainEvent> domainEvents = new ArrayList<>();

    private Payment() {
        this.transactions = new ArrayList<>();
    }

    /**
     * Factory method for initiating payment
     */
    public static Payment initiate(
        OrderId orderId,
        CustomerId customerId,
        Money amount,
        PaymentMethod paymentMethod,
        PaymentPolicy paymentPolicy
    ) {
        validatePaymentInitiation(orderId, customerId, amount, paymentMethod, paymentPolicy);

        Payment payment = new Payment();
        payment.id = PaymentId.generate();
        payment.orderId = orderId;
        payment.customerId = customerId;
        payment.amount = amount;
        payment.currency = amount.getCurrency();
        payment.paymentMethod = paymentMethod;
        payment.paymentPolicy = paymentPolicy;
        payment.status = PaymentStatus.INITIATED;
        payment.initiatedAt = LocalDateTime.now();

        // Initial fraud assessment
        payment.performInitialFraudAssessment();

        // Create initial transaction
        PaymentTransaction initTransaction = PaymentTransaction.initiate(
            payment.id,
            amount,
            paymentMethod,
            LocalDateTime.now()
        );
        payment.transactions.add(initTransaction);

        payment.addDomainEvent(new PaymentInitiatedEvent(
            payment.id,
            payment.orderId,
            payment.customerId,
            payment.amount,
            payment.paymentMethod.getType(),
            payment.initiatedAt
        ));

        return payment;
    }

    /**
     * Authorize payment (reserve funds)
     */
    public PaymentAuthorizationResult authorize(
        PaymentGatewayResult gatewayResult,
        RiskAssessment riskAssessment
    ) {
        if (status != PaymentStatus.INITIATED) {
            return PaymentAuthorizationResult.failed(
                "Payment can only be authorized from INITIATED status. Current: " + status
            );
        }

        // Check fraud assessment
        if (fraudAssessment.getRiskLevel() == RiskLevel.HIGH) {
            return PaymentAuthorizationResult.failed("Payment blocked due to high fraud risk");
        }

        // Validate gateway result
        if (!gatewayResult.isSuccessful()) {
            this.status = PaymentStatus.AUTHORIZATION_FAILED;

            addTransaction(PaymentTransaction.authorizationFailed(
                this.id,
                this.amount,
                gatewayResult.getErrorMessage(),
                LocalDateTime.now()
            ));

            addDomainEvent(new PaymentAuthorizationFailedEvent(
                this.id,
                this.orderId,
                gatewayResult.getErrorCode(),
                gatewayResult.getErrorMessage(),
                LocalDateTime.now()
            ));

            return PaymentAuthorizationResult.failed(gatewayResult.getErrorMessage());
        }

        // Apply payment policies
        PaymentPolicyResult policyResult = paymentPolicy.canAuthorizePayment(this, riskAssessment);
        if (!policyResult.isAllowed()) {
            this.status = PaymentStatus.AUTHORIZATION_DENIED;

            addDomainEvent(new PaymentAuthorizationDeniedEvent(
                this.id,
                this.orderId,
                policyResult.getDenialReason(),
                LocalDateTime.now()
            ));

            return PaymentAuthorizationResult.failed(policyResult.getDenialReason());
        }

        // Successful authorization
        this.status = PaymentStatus.AUTHORIZED;
        this.authorizedAt = LocalDateTime.now();
        this.gatewayReference = gatewayResult.getGatewayTransactionId();

        addTransaction(PaymentTransaction.authorized(
            this.id,
            this.amount,
            gatewayResult.getAuthorizationCode(),
            this.authorizedAt
        ));

        addDomainEvent(new PaymentAuthorizedEvent(
            this.id,
            this.orderId,
            this.customerId,
            this.amount,
            gatewayResult.getAuthorizationCode(),
            this.authorizedAt
        ));

        return PaymentAuthorizationResult.successful(
            gatewayResult.getAuthorizationCode(),
            this.authorizedAt
        );
    }

    /**
     * Capture payment (charge the customer)
     */
    public PaymentCaptureResult capture(Money captureAmount, String reason) {
        if (status != PaymentStatus.AUTHORIZED) {
            return PaymentCaptureResult.failed(
                "Payment can only be captured from AUTHORIZED status. Current: " + status
            );
        }

        // Validate capture amount
        if (captureAmount.isGreaterThan(this.amount)) {
            return PaymentCaptureResult.failed(
                String.format("Capture amount %s exceeds authorized amount %s",
                    captureAmount, this.amount)
            );
        }

        if (captureAmount.isNegativeOrZero()) {
            return PaymentCaptureResult.failed("Capture amount must be positive");
        }

        // Check capture policy
        CapturePolicyResult capturePolicyResult = paymentPolicy.canCapturePayment(
            this, captureAmount, reason
        );

        if (!capturePolicyResult.isAllowed()) {
            return PaymentCaptureResult.failed(capturePolicyResult.getDenialReason());
        }

        // Perform capture
        this.status = PaymentStatus.CAPTURED;
        this.capturedAt = LocalDateTime.now();

        addTransaction(PaymentTransaction.captured(
            this.id,
            captureAmount,
            reason,
            this.capturedAt
        ));

        addDomainEvent(new PaymentCapturedEvent(
            this.id,
            this.orderId,
            this.customerId,
            captureAmount,
            reason,
            this.capturedAt
        ));

        // If partial capture, update remaining authorized amount
        if (captureAmount.isLessThan(this.amount)) {
            Money remainingAmount = this.amount.subtract(captureAmount);
            addDomainEvent(new PartialCaptureCompletedEvent(
                this.id,
                this.orderId,
                captureAmount,
                remainingAmount,
                this.capturedAt
            ));
        }

        return PaymentCaptureResult.successful(captureAmount, this.capturedAt);
    }

    /**
     * Refund payment (full or partial)
     */
    public PaymentRefundResult refund(Money refundAmount, String reason, String requestedBy) {
        if (!canRefund()) {
            return PaymentRefundResult.failed(
                "Payment cannot be refunded in current status: " + status
            );
        }

        if (refundAmount.isGreaterThan(getCapturedAmount())) {
            return PaymentRefundResult.failed(
                String.format("Refund amount %s exceeds captured amount %s",
                    refundAmount, getCapturedAmount())
            );
        }

        // Check refund policy
        RefundPolicyResult refundPolicyResult = paymentPolicy.canRefundPayment(
            this, refundAmount, reason
        );

        if (!refundPolicyResult.isAllowed()) {
            return PaymentRefundResult.failed(refundPolicyResult.getDenialReason());
        }

        // Calculate refund fee if applicable
        Money refundFee = paymentPolicy.calculateRefundFee(this, refundAmount);
        Money netRefundAmount = refundAmount.subtract(refundFee);

        // Process refund
        RefundId refundId = RefundId.generate();

        addTransaction(PaymentTransaction.refunded(
            this.id,
            refundAmount,
            refundFee,
            reason,
            requestedBy,
            LocalDateTime.now()
        ));

        // Update status if full refund
        Money totalRefunded = getTotalRefundedAmount().add(refundAmount);
        if (totalRefunded.equals(getCapturedAmount())) {
            this.status = PaymentStatus.FULLY_REFUNDED;
        } else {
            this.status = PaymentStatus.PARTIALLY_REFUNDED;
        }

        addDomainEvent(new PaymentRefundedEvent(
            this.id,
            this.orderId,
            refundId,
            refundAmount,
            refundFee,
            netRefundAmount,
            reason,
            requestedBy,
            LocalDateTime.now()
        ));

        return PaymentRefundResult.successful(
            refundId,
            netRefundAmount,
            refundFee,
            LocalDateTime.now()
        );
    }

    /**
     * Void payment (cancel authorization)
     */
    public PaymentVoidResult voidPayment(String reason, String requestedBy) {
        if (status != PaymentStatus.AUTHORIZED) {
            return PaymentVoidResult.failed(
                "Payment can only be voided from AUTHORIZED status. Current: " + status
            );
        }

        // Check void policy
        VoidPolicyResult voidPolicyResult = paymentPolicy.canVoidPayment(this, reason);
        if (!voidPolicyResult.isAllowed()) {
            return PaymentVoidResult.failed(voidPolicyResult.getDenialReason());
        }

        this.status = PaymentStatus.VOIDED;

        addTransaction(PaymentTransaction.voided(
            this.id,
            this.amount,
            reason,
            requestedBy,
            LocalDateTime.now()
        ));

        addDomainEvent(new PaymentVoidedEvent(
            this.id,
            this.orderId,
            this.amount,
            reason,
            requestedBy,
            LocalDateTime.now()
        ));

        return PaymentVoidResult.successful(LocalDateTime.now());
    }

    /**
     * Handle chargeback
     */
    public void handleChargeback(
        Money chargebackAmount,
        String reason,
        String caseId,
        LocalDateTime receivedAt
    ) {
        if (!status.canReceiveChargeback()) {
            throw new IllegalPaymentStateException(
                "Payment cannot receive chargeback in status: " + status
            );
        }

        this.status = PaymentStatus.CHARGEBACK;

        addTransaction(PaymentTransaction.chargeback(
            this.id,
            chargebackAmount,
            reason,
            caseId,
            receivedAt
        ));

        addDomainEvent(new ChargebackReceivedEvent(
            this.id,
            this.orderId,
            chargebackAmount,
            reason,
            caseId,
            receivedAt
        ));
    }

    /**
     * Settle payment (final settlement)
     */
    public void settle(LocalDateTime settlementDate, Money settlementAmount) {
        if (status != PaymentStatus.CAPTURED) {
            throw new IllegalPaymentStateException(
                "Only captured payments can be settled. Current status: " + status
            );
        }

        this.status = PaymentStatus.SETTLED;
        this.settledAt = settlementDate;

        addTransaction(PaymentTransaction.settled(
            this.id,
            settlementAmount,
            settlementDate
        ));

        addDomainEvent(new PaymentSettledEvent(
            this.id,
            this.orderId,
            settlementAmount,
            settlementDate
        ));
    }

    private void performInitialFraudAssessment() {
        // Simplified fraud assessment - would integrate with fraud detection service
        FraudAssessmentBuilder builder = new FraudAssessmentBuilder();

        // Check payment method risk
        if (paymentMethod.isHighRisk()) {
            builder.addRiskFactor(FraudRiskFactor.HIGH_RISK_PAYMENT_METHOD);
        }

        // Check amount threshold
        if (amount.isGreaterThan(Money.of(1000))) {
            builder.addRiskFactor(FraudRiskFactor.HIGH_AMOUNT);
        }

        // Check customer history
        if (paymentMethod.isFirstTimeUse()) {
            builder.addRiskFactor(FraudRiskFactor.NEW_PAYMENT_METHOD);
        }

        this.fraudAssessment = builder.build();
    }

    private boolean canRefund() {
        return status == PaymentStatus.CAPTURED ||
               status == PaymentStatus.SETTLED ||
               status == PaymentStatus.PARTIALLY_REFUNDED;
    }

    private Money getCapturedAmount() {
        return transactions.stream()
            .filter(tx -> tx.getType() == TransactionType.CAPTURE)
            .map(PaymentTransaction::getAmount)
            .reduce(Money.ZERO, Money::add);
    }

    private Money getTotalRefundedAmount() {
        return transactions.stream()
            .filter(tx -> tx.getType() == TransactionType.REFUND)
            .map(PaymentTransaction::getAmount)
            .reduce(Money.ZERO, Money::add);
    }

    private void addTransaction(PaymentTransaction transaction) {
        this.transactions.add(transaction);
    }

    private void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    private static void validatePaymentInitiation(
        OrderId orderId,
        CustomerId customerId,
        Money amount,
        PaymentMethod paymentMethod,
        PaymentPolicy paymentPolicy
    ) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (amount == null || amount.isNegativeOrZero()) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method is required");
        }
        if (paymentPolicy == null) {
            throw new IllegalArgumentException("Payment policy is required");
        }
    }

    // Getters
    public PaymentId getId() { return id; }
    public OrderId getOrderId() { return orderId; }
    public CustomerId getCustomerId() { return customerId; }
    public Money getAmount() { return amount; }
    public Currency getCurrency() { return currency; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public List<PaymentTransaction> getTransactions() { return List.copyOf(transactions); }
    public FraudAssessment getFraudAssessment() { return fraudAssessment; }
    public LocalDateTime getInitiatedAt() { return initiatedAt; }
    public LocalDateTime getAuthorizedAt() { return authorizedAt; }
    public LocalDateTime getCapturedAt() { return capturedAt; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public String getGatewayReference() { return gatewayReference; }
    public List<DomainEvent> getDomainEvents() { return List.copyOf(domainEvents); }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    // Result classes for complex operations
    public static class PaymentAuthorizationResult {
        private final boolean successful;
        private final String authorizationCode;
        private final LocalDateTime authorizedAt;
        private final String failureReason;

        private PaymentAuthorizationResult(boolean successful, String authorizationCode,
                                         LocalDateTime authorizedAt, String failureReason) {
            this.successful = successful;
            this.authorizationCode = authorizationCode;
            this.authorizedAt = authorizedAt;
            this.failureReason = failureReason;
        }

        public static PaymentAuthorizationResult successful(String authCode, LocalDateTime authorizedAt) {
            return new PaymentAuthorizationResult(true, authCode, authorizedAt, null);
        }

        public static PaymentAuthorizationResult failed(String reason) {
            return new PaymentAuthorizationResult(false, null, null, reason);
        }

        public boolean isSuccessful() { return successful; }
        public String getAuthorizationCode() { return authorizationCode; }
        public LocalDateTime getAuthorizedAt() { return authorizedAt; }
        public String getFailureReason() { return failureReason; }
    }

    public static class PaymentCaptureResult {
        private final boolean successful;
        private final Money capturedAmount;
        private final LocalDateTime capturedAt;
        private final String failureReason;

        private PaymentCaptureResult(boolean successful, Money capturedAmount,
                                   LocalDateTime capturedAt, String failureReason) {
            this.successful = successful;
            this.capturedAmount = capturedAmount;
            this.capturedAt = capturedAt;
            this.failureReason = failureReason;
        }

        public static PaymentCaptureResult successful(Money amount, LocalDateTime capturedAt) {
            return new PaymentCaptureResult(true, amount, capturedAt, null);
        }

        public static PaymentCaptureResult failed(String reason) {
            return new PaymentCaptureResult(false, null, null, reason);
        }

        public boolean isSuccessful() { return successful; }
        public Money getCapturedAmount() { return capturedAmount; }
        public LocalDateTime getCapturedAt() { return capturedAt; }
        public String getFailureReason() { return failureReason; }
    }

    public static class PaymentRefundResult {
        private final boolean successful;
        private final RefundId refundId;
        private final Money netRefundAmount;
        private final Money refundFee;
        private final LocalDateTime refundedAt;
        private final String failureReason;

        private PaymentRefundResult(boolean successful, RefundId refundId, Money netRefundAmount,
                                  Money refundFee, LocalDateTime refundedAt, String failureReason) {
            this.successful = successful;
            this.refundId = refundId;
            this.netRefundAmount = netRefundAmount;
            this.refundFee = refundFee;
            this.refundedAt = refundedAt;
            this.failureReason = failureReason;
        }

        public static PaymentRefundResult successful(RefundId refundId, Money netAmount,
                                                   Money fee, LocalDateTime refundedAt) {
            return new PaymentRefundResult(true, refundId, netAmount, fee, refundedAt, null);
        }

        public static PaymentRefundResult failed(String reason) {
            return new PaymentRefundResult(false, null, null, null, null, reason);
        }

        public boolean isSuccessful() { return successful; }
        public RefundId getRefundId() { return refundId; }
        public Money getNetRefundAmount() { return netRefundAmount; }
        public Money getRefundFee() { return refundFee; }
        public LocalDateTime getRefundedAt() { return refundedAt; }
        public String getFailureReason() { return failureReason; }
    }

    public static class PaymentVoidResult {
        private final boolean successful;
        private final LocalDateTime voidedAt;
        private final String failureReason;

        private PaymentVoidResult(boolean successful, LocalDateTime voidedAt, String failureReason) {
            this.successful = successful;
            this.voidedAt = voidedAt;
            this.failureReason = failureReason;
        }

        public static PaymentVoidResult successful(LocalDateTime voidedAt) {
            return new PaymentVoidResult(true, voidedAt, null);
        }

        public static PaymentVoidResult failed(String reason) {
            return new PaymentVoidResult(false, null, reason);
        }

        public boolean isSuccessful() { return successful; }
        public LocalDateTime getVoidedAt() { return voidedAt; }
        public String getFailureReason() { return failureReason; }
    }
}
```

### Payment Transaction Entity

```java
// domain/model/PaymentTransaction.java
package com.example.payment.domain.model;

import com.example.payment.domain.valueobject.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Payment Transaction Entity
 *
 * Represents individual payment operations within a payment aggregate.
 * Provides immutable audit trail of all payment activities.
 */
public class PaymentTransaction {
    private TransactionId id;
    private PaymentId paymentId;
    private TransactionType type;
    private Money amount;
    private TransactionStatus status;
    private String reference;
    private String description;
    private Money fee;
    private String gatewayResponse;
    private LocalDateTime processedAt;

    private PaymentTransaction() {}

    public static PaymentTransaction initiate(
        PaymentId paymentId,
        Money amount,
        PaymentMethod paymentMethod,
        LocalDateTime processedAt
    ) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.id = TransactionId.generate();
        transaction.paymentId = paymentId;
        transaction.type = TransactionType.INITIATION;
        transaction.amount = amount;
        transaction.status = TransactionStatus.PENDING;
        transaction.description = "Payment initiated with " + paymentMethod.getType();
        transaction.fee = Money.ZERO;
        transaction.processedAt = processedAt;

        return transaction;
    }

    public static PaymentTransaction authorized(
        PaymentId paymentId,
        Money amount,
        String authorizationCode,
        LocalDateTime processedAt
    ) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.id = TransactionId.generate();
        transaction.paymentId = paymentId;
        transaction.type = TransactionType.AUTHORIZATION;
        transaction.amount = amount;
        transaction.status = TransactionStatus.COMPLETED;
        transaction.reference = authorizationCode;
        transaction.description = "Payment authorized";
        transaction.fee = Money.ZERO;
        transaction.processedAt = processedAt;

        return transaction;
    }

    public static PaymentTransaction authorizationFailed(
        PaymentId paymentId,
        Money amount,
        String errorMessage,
        LocalDateTime processedAt
    ) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.id = TransactionId.generate();
        transaction.paymentId = paymentId;
        transaction.type = TransactionType.AUTHORIZATION;
        transaction.amount = amount;
        transaction.status = TransactionStatus.FAILED;
        transaction.description = "Authorization failed: " + errorMessage;
        transaction.fee = Money.ZERO;
        transaction.processedAt = processedAt;

        return transaction;
    }

    public static PaymentTransaction captured(
        PaymentId paymentId,
        Money amount,
        String reason,
        LocalDateTime processedAt
    ) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.id = TransactionId.generate();
        transaction.paymentId = paymentId;
        transaction.type = TransactionType.CAPTURE;
        transaction.amount = amount;
        transaction.status = TransactionStatus.COMPLETED;
        transaction.description = "Payment captured - " + reason;
        transaction.fee = Money.ZERO;
        transaction.processedAt = processedAt;

        return transaction;
    }

    public static PaymentTransaction refunded(
        PaymentId paymentId,
        Money refundAmount,
        Money refundFee,
        String reason,
        String requestedBy,
        LocalDateTime processedAt
    ) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.id = TransactionId.generate();
        transaction.paymentId = paymentId;
        transaction.type = TransactionType.REFUND;
        transaction.amount = refundAmount;
        transaction.status = TransactionStatus.COMPLETED;
        transaction.fee = refundFee;
        transaction.description = String.format("Refund processed by %s - %s", requestedBy, reason);
        transaction.processedAt = processedAt;

        return transaction;
    }

    public static PaymentTransaction voided(
        PaymentId paymentId,
        Money amount,
        String reason,
        String requestedBy,
        LocalDateTime processedAt
    ) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.id = TransactionId.generate();
        transaction.paymentId = paymentId;
        transaction.type = TransactionType.VOID;
        transaction.amount = amount;
        transaction.status = TransactionStatus.COMPLETED;
        transaction.description = String.format("Payment voided by %s - %s", requestedBy, reason);
        transaction.fee = Money.ZERO;
        transaction.processedAt = processedAt;

        return transaction;
    }

    public static PaymentTransaction chargeback(
        PaymentId paymentId,
        Money chargebackAmount,
        String reason,
        String caseId,
        LocalDateTime processedAt
    ) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.id = TransactionId.generate();
        transaction.paymentId = paymentId;
        transaction.type = TransactionType.CHARGEBACK;
        transaction.amount = chargebackAmount;
        transaction.status = TransactionStatus.COMPLETED;
        transaction.reference = caseId;
        transaction.description = "Chargeback received - " + reason;
        transaction.fee = Money.ZERO;
        transaction.processedAt = processedAt;

        return transaction;
    }

    public static PaymentTransaction settled(
        PaymentId paymentId,
        Money settlementAmount,
        LocalDateTime settlementDate
    ) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.id = TransactionId.generate();
        transaction.paymentId = paymentId;
        transaction.type = TransactionType.SETTLEMENT;
        transaction.amount = settlementAmount;
        transaction.status = TransactionStatus.COMPLETED;
        transaction.description = "Payment settled";
        transaction.fee = Money.ZERO;
        transaction.processedAt = settlementDate;

        return transaction;
    }

    /**
     * Mark transaction as failed
     */
    public PaymentTransaction markAsFailed(String errorMessage) {
        PaymentTransaction failed = new PaymentTransaction();
        failed.id = this.id;
        failed.paymentId = this.paymentId;
        failed.type = this.type;
        failed.amount = this.amount;
        failed.status = TransactionStatus.FAILED;
        failed.reference = this.reference;
        failed.description = this.description + " - Failed: " + errorMessage;
        failed.fee = this.fee;
        failed.gatewayResponse = errorMessage;
        failed.processedAt = this.processedAt;

        return failed;
    }

    /**
     * Add gateway response information
     */
    public PaymentTransaction withGatewayResponse(String gatewayResponse) {
        PaymentTransaction updated = new PaymentTransaction();
        updated.id = this.id;
        updated.paymentId = this.paymentId;
        updated.type = this.type;
        updated.amount = this.amount;
        updated.status = this.status;
        updated.reference = this.reference;
        updated.description = this.description;
        updated.fee = this.fee;
        updated.gatewayResponse = gatewayResponse;
        updated.processedAt = this.processedAt;

        return updated;
    }

    // Getters
    public TransactionId getId() { return id; }
    public PaymentId getPaymentId() { return paymentId; }
    public TransactionType getType() { return type; }
    public Money getAmount() { return amount; }
    public TransactionStatus getStatus() { return status; }
    public String getReference() { return reference; }
    public String getDescription() { return description; }
    public Money getFee() { return fee; }
    public String getGatewayResponse() { return gatewayResponse; }
    public LocalDateTime getProcessedAt() { return processedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentTransaction that = (PaymentTransaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

## Payment Value Objects

### Payment Method Value Object

```java
// domain/valueobject/PaymentMethod.java
package com.example.payment.domain.valueobject;

import java.util.Objects;

/**
 * Payment Method Value Object
 *
 * Represents different payment methods with their characteristics.
 * Encapsulates payment method specific business rules.
 */
public class PaymentMethod {
    private final PaymentMethodType type;
    private final String maskedDetails;
    private final String last4Digits;
    private final String expiryMonth;
    private final String expiryYear;
    private final String holderName;
    private final BillingAddress billingAddress;
    private final boolean isFirstTimeUse;
    private final RiskLevel riskLevel;

    private PaymentMethod(
        PaymentMethodType type,
        String maskedDetails,
        String last4Digits,
        String expiryMonth,
        String expiryYear,
        String holderName,
        BillingAddress billingAddress,
        boolean isFirstTimeUse,
        RiskLevel riskLevel
    ) {
        this.type = type;
        this.maskedDetails = maskedDetails;
        this.last4Digits = last4Digits;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.holderName = holderName;
        this.billingAddress = billingAddress;
        this.isFirstTimeUse = isFirstTimeUse;
        this.riskLevel = riskLevel;
    }

    public static PaymentMethod creditCard(
        String maskedCardNumber,
        String last4Digits,
        String expiryMonth,
        String expiryYear,
        String holderName,
        BillingAddress billingAddress,
        boolean isFirstTimeUse
    ) {
        validateCreditCardDetails(maskedCardNumber, last4Digits, expiryMonth, expiryYear, holderName);

        RiskLevel riskLevel = calculateCreditCardRisk(isFirstTimeUse, billingAddress);

        return new PaymentMethod(
            PaymentMethodType.CREDIT_CARD,
            maskedCardNumber,
            last4Digits,
            expiryMonth,
            expiryYear,
            holderName,
            billingAddress,
            isFirstTimeUse,
            riskLevel
        );
    }

    public static PaymentMethod debitCard(
        String maskedCardNumber,
        String last4Digits,
        String expiryMonth,
        String expiryYear,
        String holderName,
        BillingAddress billingAddress,
        boolean isFirstTimeUse
    ) {
        validateCreditCardDetails(maskedCardNumber, last4Digits, expiryMonth, expiryYear, holderName);

        RiskLevel riskLevel = calculateDebitCardRisk(isFirstTimeUse, billingAddress);

        return new PaymentMethod(
            PaymentMethodType.DEBIT_CARD,
            maskedCardNumber,
            last4Digits,
            expiryMonth,
            expiryYear,
            holderName,
            billingAddress,
            isFirstTimeUse,
            riskLevel
        );
    }

    public static PaymentMethod digitalWallet(
        String walletProvider,
        String walletId,
        BillingAddress billingAddress,
        boolean isFirstTimeUse
    ) {
        if (walletProvider == null || walletProvider.trim().isEmpty()) {
            throw new IllegalArgumentException("Wallet provider is required");
        }
        if (walletId == null || walletId.trim().isEmpty()) {
            throw new IllegalArgumentException("Wallet ID is required");
        }

        String maskedDetails = walletProvider + " ending in " + walletId.substring(Math.max(0, walletId.length() - 4));
        RiskLevel riskLevel = calculateDigitalWalletRisk(walletProvider, isFirstTimeUse);

        return new PaymentMethod(
            PaymentMethodType.DIGITAL_WALLET,
            maskedDetails,
            walletId.substring(Math.max(0, walletId.length() - 4)),
            null,
            null,
            null,
            billingAddress,
            isFirstTimeUse,
            riskLevel
        );
    }

    public static PaymentMethod bankTransfer(
        String bankName,
        String accountNumber,
        BillingAddress billingAddress
    ) {
        if (bankName == null || bankName.trim().isEmpty()) {
            throw new IllegalArgumentException("Bank name is required");
        }
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number is required");
        }

        String maskedDetails = bankName + " ending in " + accountNumber.substring(Math.max(0, accountNumber.length() - 4));

        return new PaymentMethod(
            PaymentMethodType.BANK_TRANSFER,
            maskedDetails,
            accountNumber.substring(Math.max(0, accountNumber.length() - 4)),
            null,
            null,
            null,
            billingAddress,
            false, // Bank transfers are generally not first-time use in same way
            RiskLevel.LOW // Bank transfers generally lower risk
        );
    }

    /**
     * Check if payment method is expired
     */
    public boolean isExpired() {
        if (type != PaymentMethodType.CREDIT_CARD && type != PaymentMethodType.DEBIT_CARD) {
            return false; // Non-card payment methods don't expire
        }

        if (expiryMonth == null || expiryYear == null) {
            return false;
        }

        try {
            int expMonth = Integer.parseInt(expiryMonth);
            int expYear = Integer.parseInt(expiryYear);

            java.time.LocalDate now = java.time.LocalDate.now();
            java.time.LocalDate expiryDate = java.time.LocalDate.of(expYear, expMonth, 1)
                .plusMonths(1)
                .minusDays(1); // Last day of expiry month

            return now.isAfter(expiryDate);
        } catch (NumberFormatException e) {
            return true; // Invalid format considered expired
        }
    }

    /**
     * Check if payment method is considered high risk
     */
    public boolean isHighRisk() {
        return riskLevel == RiskLevel.HIGH;
    }

    /**
     * Check if this is first time using this payment method
     */
    public boolean isFirstTimeUse() {
        return isFirstTimeUse;
    }

    /**
     * Get payment method description for display
     */
    public String getDisplayDescription() {
        return switch (type) {
            case CREDIT_CARD -> "Credit Card " + maskedDetails;
            case DEBIT_CARD -> "Debit Card " + maskedDetails;
            case DIGITAL_WALLET -> maskedDetails;
            case BANK_TRANSFER -> "Bank Transfer " + maskedDetails;
            case CRYPTOCURRENCY -> "Crypto " + maskedDetails;
        };
    }

    /**
     * Check if payment method supports authorization/capture flow
     */
    public boolean supportsAuthCapture() {
        return type == PaymentMethodType.CREDIT_CARD ||
               type == PaymentMethodType.DIGITAL_WALLET;
    }

    /**
     * Check if payment method supports refunds
     */
    public boolean supportsRefunds() {
        return type != PaymentMethodType.CRYPTOCURRENCY; // Crypto generally doesn't support refunds
    }

    private static void validateCreditCardDetails(
        String maskedCardNumber,
        String last4Digits,
        String expiryMonth,
        String expiryYear,
        String holderName
    ) {
        if (maskedCardNumber == null || maskedCardNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Masked card number is required");
        }
        if (last4Digits == null || !last4Digits.matches("\\d{4}")) {
            throw new IllegalArgumentException("Last 4 digits must be 4 digits");
        }
        if (expiryMonth == null || !expiryMonth.matches("(0[1-9]|1[0-2])")) {
            throw new IllegalArgumentException("Expiry month must be MM format");
        }
        if (expiryYear == null || !expiryYear.matches("\\d{4}")) {
            throw new IllegalArgumentException("Expiry year must be YYYY format");
        }
        if (holderName == null || holderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Card holder name is required");
        }
    }

    private static RiskLevel calculateCreditCardRisk(boolean isFirstTimeUse, BillingAddress billingAddress) {
        int riskScore = 0;

        if (isFirstTimeUse) {
            riskScore += 20;
        }

        if (billingAddress.isInternational()) {
            riskScore += 15;
        }

        if (riskScore >= 30) {
            return RiskLevel.HIGH;
        } else if (riskScore >= 15) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }

    private static RiskLevel calculateDebitCardRisk(boolean isFirstTimeUse, BillingAddress billingAddress) {
        // Debit cards generally lower risk than credit cards
        int riskScore = 0;

        if (isFirstTimeUse) {
            riskScore += 15;
        }

        if (billingAddress.isInternational()) {
            riskScore += 10;
        }

        if (riskScore >= 20) {
            return RiskLevel.HIGH;
        } else if (riskScore >= 10) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }

    private static RiskLevel calculateDigitalWalletRisk(String walletProvider, boolean isFirstTimeUse) {
        // Digital wallets generally lower risk due to additional verification
        int riskScore = 0;

        if (isFirstTimeUse) {
            riskScore += 10;
        }

        // Some providers considered higher risk
        if (walletProvider.toLowerCase().contains("unknown")) {
            riskScore += 25;
        }

        if (riskScore >= 25) {
            return RiskLevel.HIGH;
        } else if (riskScore >= 10) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }

    // Getters
    public PaymentMethodType getType() { return type; }
    public String getMaskedDetails() { return maskedDetails; }
    public String getLast4Digits() { return last4Digits; }
    public String getExpiryMonth() { return expiryMonth; }
    public String getExpiryYear() { return expiryYear; }
    public String getHolderName() { return holderName; }
    public BillingAddress getBillingAddress() { return billingAddress; }
    public RiskLevel getRiskLevel() { return riskLevel; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentMethod that = (PaymentMethod) o;
        return isFirstTimeUse == that.isFirstTimeUse &&
               type == that.type &&
               Objects.equals(maskedDetails, that.maskedDetails) &&
               Objects.equals(last4Digits, that.last4Digits) &&
               Objects.equals(expiryMonth, that.expiryMonth) &&
               Objects.equals(expiryYear, that.expiryYear) &&
               Objects.equals(holderName, that.holderName) &&
               Objects.equals(billingAddress, that.billingAddress) &&
               riskLevel == that.riskLevel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, maskedDetails, last4Digits, expiryMonth, expiryYear,
                          holderName, billingAddress, isFirstTimeUse, riskLevel);
    }

    @Override
    public String toString() {
        return getDisplayDescription();
    }

    public enum PaymentMethodType {
        CREDIT_CARD,
        DEBIT_CARD,
        DIGITAL_WALLET,
        BANK_TRANSFER,
        CRYPTOCURRENCY
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH
    }
}
```

### Payment Status Enum

```java
// domain/valueobject/PaymentStatus.java
package com.example.payment.domain.valueobject;

/**
 * Payment Status Value Object
 *
 * Represents valid payment states with transition rules.
 */
public enum PaymentStatus {
    INITIATED("Payment initiated by customer"),
    AUTHORIZED("Payment authorized, funds reserved"),
    AUTHORIZATION_FAILED("Payment authorization failed"),
    AUTHORIZATION_DENIED("Payment authorization denied by policy"),
    CAPTURED("Payment captured, funds charged"),
    SETTLED("Payment settled with merchant"),
    PARTIALLY_REFUNDED("Payment partially refunded"),
    FULLY_REFUNDED("Payment fully refunded"),
    VOIDED("Payment authorization voided"),
    CHARGEBACK("Chargeback received"),
    EXPIRED("Payment authorization expired");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if transition to new status is valid
     */
    public boolean canTransitionTo(PaymentStatus newStatus) {
        return switch (this) {
            case INITIATED -> newStatus == AUTHORIZED ||
                            newStatus == AUTHORIZATION_FAILED ||
                            newStatus == AUTHORIZATION_DENIED ||
                            newStatus == EXPIRED;

            case AUTHORIZED -> newStatus == CAPTURED ||
                             newStatus == VOIDED ||
                             newStatus == EXPIRED;

            case CAPTURED -> newStatus == SETTLED ||
                           newStatus == PARTIALLY_REFUNDED ||
                           newStatus == FULLY_REFUNDED ||
                           newStatus == CHARGEBACK;

            case SETTLED -> newStatus == PARTIALLY_REFUNDED ||
                          newStatus == FULLY_REFUNDED ||
                          newStatus == CHARGEBACK;

            case PARTIALLY_REFUNDED -> newStatus == FULLY_REFUNDED ||
                                     newStatus == CHARGEBACK;

            default -> false; // Final states cannot transition
        };
    }

    public boolean isFinalState() {
        return this == AUTHORIZATION_FAILED ||
               this == AUTHORIZATION_DENIED ||
               this == FULLY_REFUNDED ||
               this == VOIDED ||
               this == CHARGEBACK ||
               this == EXPIRED;
    }

    public boolean isSuccessfulState() {
        return this == AUTHORIZED ||
               this == CAPTURED ||
               this == SETTLED;
    }

    public boolean canReceiveChargeback() {
        return this == CAPTURED ||
               this == SETTLED ||
               this == PARTIALLY_REFUNDED;
    }

    public boolean canBeRefunded() {
        return this == CAPTURED ||
               this == SETTLED ||
               this == PARTIALLY_REFUNDED;
    }

    public boolean canBeVoided() {
        return this == AUTHORIZED;
    }
}
```

## Payment Domain Events

```java
// domain/event/PaymentAuthorizedEvent.java
package com.example.payment.domain.event;

import com.example.payment.domain.valueobject.*;
import java.time.LocalDateTime;

/**
 * Payment Authorized Domain Event
 *
 * Published when payment is successfully authorized.
 * Triggers order confirmation process.
 */
public class PaymentAuthorizedEvent implements DomainEvent {
    private final PaymentId paymentId;
    private final OrderId orderId;
    private final CustomerId customerId;
    private final Money amount;
    private final String authorizationCode;
    private final LocalDateTime occurredAt;
    private final String eventId;

    public PaymentAuthorizedEvent(
        PaymentId paymentId,
        OrderId orderId,
        CustomerId customerId,
        Money amount,
        String authorizationCode,
        LocalDateTime occurredAt
    ) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.authorizationCode = authorizationCode;
        this.occurredAt = occurredAt;
        this.eventId = UUID.randomUUID().toString();
    }

    @Override
    public String getEventId() { return eventId; }

    @Override
    public LocalDateTime getOccurredAt() { return occurredAt; }

    @Override
    public String getAggregateId() { return paymentId.getValue(); }

    @Override
    public String getEventType() { return "PaymentAuthorized"; }

    public PaymentId getPaymentId() { return paymentId; }
    public OrderId getOrderId() { return orderId; }
    public CustomerId getCustomerId() { return customerId; }
    public Money getAmount() { return amount; }
    public String getAuthorizationCode() { return authorizationCode; }
}
```

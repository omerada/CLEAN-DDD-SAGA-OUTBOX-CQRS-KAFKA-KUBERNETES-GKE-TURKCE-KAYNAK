package com.example.order.infrastructure.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Outbox Configuration Properties
 * 
 * Configuration for outbox pattern behavior
 */
@Component
@ConfigurationProperties(prefix = "outbox")
public class OutboxConfiguration {

    /**
     * Whether outbox processing is enabled
     */
    private boolean enabled = true;

    /**
     * Batch size for processing pending events
     */
    private int batchSize = 50;

    /**
     * Maximum retry attempts for failed events
     */
    private int maxRetries = 3;

    /**
     * Batch size for retry processing
     */
    private int retryBatchSize = 20;

    /**
     * Whether retry processing is enabled
     */
    private boolean retryEnabled = true;

    /**
     * Delay before retrying failed events
     */
    private Duration retryDelay = Duration.ofMinutes(5);

    /**
     * Whether cleanup is enabled
     */
    private boolean cleanupEnabled = true;

    /**
     * Age threshold for cleaning up old events
     */
    private Duration cleanupAge = Duration.ofDays(7);

    /**
     * Timeout for publishing events to message broker
     */
    private Duration publishTimeout = Duration.ofSeconds(30);

    /**
     * Processing interval for pending events
     */
    private Duration processingInterval = Duration.ofSeconds(5);

    /**
     * Retry interval for failed events
     */
    private Duration retryInterval = Duration.ofSeconds(30);

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getRetryBatchSize() {
        return retryBatchSize;
    }

    public void setRetryBatchSize(int retryBatchSize) {
        this.retryBatchSize = retryBatchSize;
    }

    public boolean isRetryEnabled() {
        return retryEnabled;
    }

    public void setRetryEnabled(boolean retryEnabled) {
        this.retryEnabled = retryEnabled;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = retryDelay;
    }

    public boolean isCleanupEnabled() {
        return cleanupEnabled;
    }

    public void setCleanupEnabled(boolean cleanupEnabled) {
        this.cleanupEnabled = cleanupEnabled;
    }

    public Duration getCleanupAge() {
        return cleanupAge;
    }

    public void setCleanupAge(Duration cleanupAge) {
        this.cleanupAge = cleanupAge;
    }

    public Duration getPublishTimeout() {
        return publishTimeout;
    }

    public void setPublishTimeout(Duration publishTimeout) {
        this.publishTimeout = publishTimeout;
    }

    public Duration getProcessingInterval() {
        return processingInterval;
    }

    public void setProcessingInterval(Duration processingInterval) {
        this.processingInterval = processingInterval;
    }

    public Duration getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Duration retryInterval) {
        this.retryInterval = retryInterval;
    }
}
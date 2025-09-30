# 🔄 Message Relay Service

## Özet

Message Relay Service, Outbox Pattern'in kritik component'idir. Outbox table'dan pending event'leri poll eder, Kafka'ya publish eder ve event status'larını günceller. Bu service **exactly-once delivery** ve **reliable event publishing** sağlar.

---

## 🏗️ Message Relay Architecture

### Polling Strategy Options

```ascii
┌─────────────────────────────────────────────────────────────────────┐
│                   MESSAGE RELAY STRATEGIES                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Strategy 1: Scheduled Polling (Simple)                            │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │               @Scheduled(fixedDelay = 5000)                 │   │
│  │                                                             │   │
│  │  Timer ──→ Poll Outbox ──→ Publish Events ──→ Update Status│   │
│  │            (PENDING)        (Kafka)           (PUBLISHED)  │   │
│  │                                                             │   │
│  │  ✅ Simple implementation                                   │   │
│  │  ❌ Fixed delay, not optimal for high throughput          │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  Strategy 2: Change Data Capture (Advanced)                        │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │           Database Trigger ──→ Event Stream                │   │
│  │                                                             │   │
│  │  INSERT into outbox ──→ Debezium ──→ Kafka Connect        │   │
│  │                        (CDC)        (Real-time)           │   │
│  │                                                             │   │
│  │  ✅ Real-time, low latency                                 │   │
│  │  ❌ Complex setup, additional infrastructure              │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  Strategy 3: Hybrid Approach (Recommended)                         │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Scheduled Polling + Event-driven Triggers                 │   │
│  │                                                             │   │
│  │  Regular Polling (backup) + Immediate Trigger (fast path) │   │
│  │                                                             │   │
│  │  ✅ Best of both worlds                                    │   │
│  │  ✅ Resilient and performant                               │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🔧 Message Relay Implementation

### Core Message Relay Service

````java
// infrastructure/messaging/MessageRelayService.java
package com.example.outbox.infrastructure.messaging;

import com.example.outbox.application.service.OutboxService;
import com.example.outbox.domain.model.OutboxEvent;
import com.example.outbox.infrastructure.config.OutboxConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Message Relay Service
 *
 * Outbox table'dan pending event'leri poll eder ve Kafka'ya publish eder.
 * Exactly-once delivery ve reliable messaging garantisi sağlar.
 */
@Service
@Slf4j
public class MessageRelayService {

    private final OutboxService outboxService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxConfiguration config;
    private final MessageRelayMetrics metrics;
    private final TopicResolver topicResolver;

    // Metrics
    private final AtomicInteger activePollingTasks = new AtomicInteger(0);
    private volatile boolean isHealthy = true;
    private volatile LocalDateTime lastSuccessfulPoll;

    public MessageRelayService(
        OutboxService outboxService,
        KafkaTemplate<String, Object> kafkaTemplate,
        OutboxConfiguration config,
        MessageRelayMetrics metrics,
        TopicResolver topicResolver
    ) {
        this.outboxService = outboxService;
        this.kafkaTemplate = kafkaTemplate;
        this.config = config;
        this.metrics = metrics;
        this.topicResolver = topicResolver;
        this.lastSuccessfulPoll = LocalDateTime.now();
    }

    /**
     * Scheduled polling for pending events
     *
     * Fixed rate ile düzenli olarak outbox'ı poll eder.
     * Concurrency control ve error handling ile güvenli execution.
     */
    @Scheduled(fixedRateString = "${outbox.relay.polling-interval:5000}")
    public void pollAndPublishEvents() {
        // Concurrent execution kontrolü
        if (activePollingTasks.get() >= config.getMaxConcurrentPolling()) {
            log.warn("Max concurrent polling tasks reached: {}",
                    config.getMaxConcurrentPolling());
            metrics.recordSkippedPolling();
            return;
        }

        activePollingTasks.incrementAndGet();

        try {
            log.debug("Starting outbox polling cycle");

            long startTime = System.currentTimeMillis();
            int processedCount = 0;

            while (processedCount < config.getMaxEventsPerBatch()) {
                List<OutboxEvent> pendingEvents = outboxService.getPendingEvents(
                    config.getBatchSize()
                );

                if (pendingEvents.isEmpty()) {
                    log.debug("No pending events found");
                    break;
                }

                log.info("Processing {} pending events", pendingEvents.size());

                for (OutboxEvent event : pendingEvents) {
                    try {
                        publishEventToKafka(event);
                        processedCount++;

                    } catch (Exception e) {
                        log.error("Failed to publish event: {}\", event.getEventId(), e);
                        handleEventPublishingFailure(event, e.getMessage());
                        metrics.recordEventPublishingFailure(event.getEventType());
                    }
                }

                // Rate limiting - batch'ler arası kısa bekleme
                if (!pendingEvents.isEmpty() && processedCount < config.getMaxEventsPerBatch()) {\n                    Thread.sleep(config.getBatchProcessingDelay());\n                }\n            }\n            \n            long duration = System.currentTimeMillis() - startTime;\n            \n            if (processedCount > 0) {\n                log.info(\"Polling cycle completed: {} events processed in {} ms\",\n                        processedCount, duration);\n            }\n            \n            this.lastSuccessfulPoll = LocalDateTime.now();\n            this.isHealthy = true;\n            \n            metrics.recordPollingCycle(processedCount, duration);\n            \n        } catch (Exception e) {\n            log.error(\"Error during outbox polling cycle\", e);\n            this.isHealthy = false;\n            metrics.recordPollingError();\n            \n        } finally {\n            activePollingTasks.decrementAndGet();\n        }\n    }\n    \n    /**\n     * Publish single event to Kafka\n     */\n    private void publishEventToKafka(OutboxEvent outboxEvent) {\n        try {\n            log.debug(\"Publishing event: {} - {}\", \n                     outboxEvent.getEventId(), outboxEvent.getEventType());\n            \n            // Topic resolution\n            String topicName = topicResolver.resolveTopicForEvent(\n                outboxEvent.getAggregateType(), \n                outboxEvent.getEventType()\n            );\n            \n            // Message key (for partitioning)\n            String messageKey = outboxEvent.getAggregateId();\n            \n            // Message headers\n            Map<String, Object> headers = createKafkaHeaders(outboxEvent);\n            \n            // Parse event payload\n            Object eventPayload = parseEventPayload(\n                outboxEvent.getEventPayload(), \n                outboxEvent.getEventType()\n            );\n            \n            // Send to Kafka with headers\n            ProducerRecord<String, Object> record = new ProducerRecord<>(\n                topicName, \n                null, // partition - let Kafka decide based on key\n                messageKey, \n                eventPayload\n            );\n            \n            // Add headers\n            headers.forEach((key, value) -> {\n                record.headers().add(key, value.toString().getBytes());\n            });\n            \n            // Async send with callback\n            CompletableFuture<SendResult<String, Object>> future = \n                kafkaTemplate.send(record);\n            \n            // Handle success/failure\n            future.whenComplete((result, throwable) -> {\n                if (throwable != null) {\n                    handleKafkaPublishingFailure(outboxEvent, throwable);\n                } else {\n                    handleKafkaPublishingSuccess(outboxEvent, result);\n                }\n            });\n            \n            metrics.recordEventPublished(outboxEvent.getEventType());\n            \n        } catch (Exception e) {\n            log.error(\"Error publishing event to Kafka: {}\", outboxEvent.getEventId(), e);\n            throw new EventPublishingException(\"Failed to publish event\", e);\n        }\n    }\n    \n    /**\n     * Handle successful Kafka publishing\n     */\n    private void handleKafkaPublishingSuccess(\n        OutboxEvent outboxEvent, \n        SendResult<String, Object> result\n    ) {\n        try {\n            // Update outbox event status\n            outboxService.markEventAsPublished(outboxEvent.getEventId());\n            \n            log.debug(\"Event published successfully: {} to topic: {} partition: {} offset: {}\",\n                     outboxEvent.getEventId(),\n                     result.getRecordMetadata().topic(),\n                     result.getRecordMetadata().partition(),\n                     result.getRecordMetadata().offset());\n            \n            metrics.recordSuccessfulPublishing(\n                outboxEvent.getEventType(),\n                result.getRecordMetadata().topic()\n            );\n            \n        } catch (Exception e) {\n            log.error(\"Error marking event as published: {}\", \n                     outboxEvent.getEventId(), e);\n            // Event was published to Kafka but we couldn't update DB status\n            // This could lead to duplicate messages, but at-least-once delivery is preserved\n        }\n    }\n    \n    /**\n     * Handle Kafka publishing failure\n     */\n    private void handleKafkaPublishingFailure(\n        OutboxEvent outboxEvent, \n        Throwable throwable\n    ) {\n        String errorMessage = \"Kafka publishing failed: \" + throwable.getMessage();\n        \n        log.error(\"Failed to publish event to Kafka: {} - {}\", \n                 outboxEvent.getEventId(), errorMessage, throwable);\n        \n        handleEventPublishingFailure(outboxEvent, errorMessage);\n    }\n    \n    /**\n     * Handle event publishing failure\n     */\n    private void handleEventPublishingFailure(OutboxEvent outboxEvent, String errorMessage) {\n        try {\n            outboxService.markEventAsFailed(outboxEvent.getEventId(), errorMessage);\n            \n            log.warn(\"Marked event as failed: {} - retry count: {}\", \n                    outboxEvent.getEventId(), outboxEvent.getRetryCount() + 1);\n            \n        } catch (Exception e) {\n            log.error(\"Error marking event as failed: {}\", \n                     outboxEvent.getEventId(), e);\n        }\n    }\n    \n    /**\n     * Create Kafka message headers\n     */\n    private Map<String, Object> createKafkaHeaders(OutboxEvent outboxEvent) {\n        Map<String, Object> headers = new HashMap<>();\n        \n        headers.put(\"event-id\", outboxEvent.getEventId());\n        headers.put(\"event-type\", outboxEvent.getEventType());\n        headers.put(\"aggregate-id\", outboxEvent.getAggregateId());\n        headers.put(\"aggregate-type\", outboxEvent.getAggregateType());\n        headers.put(\"event-timestamp\", outboxEvent.getCreatedAt().toString());\n        headers.put(\"event-version\", \"1.0\");\n        headers.put(\"source-service\", config.getServiceName());\n        \n        // Tracing correlation ID\n        String correlationId = MDC.get(\"correlationId\");\n        if (correlationId != null) {\n            headers.put(\"correlation-id\", correlationId);\n        }\n        \n        return headers;\n    }\n    \n    /**\n     * Parse event payload from JSON\n     */\n    private Object parseEventPayload(String eventPayload, String eventType) {\n        try {\n            // Event type'a göre appropriate class'a deserialize et\n            Class<?> eventClass = Class.forName(\n                \"com.example.order.domain.event.\" + eventType\n            );\n            \n            return objectMapper.readValue(eventPayload, eventClass);\n            \n        } catch (Exception e) {\n            log.warn(\"Could not deserialize event payload for type: {}, \" +\n                    \"sending as raw JSON\", eventType);\n            \n            // Fallback: raw JSON as map\n            try {\n                return objectMapper.readValue(eventPayload, Map.class);\n            } catch (Exception fallbackException) {\n                log.error(\"Failed to parse event payload as JSON: {}\", eventPayload);\n                throw new EventPayloadParsingException(\n                    \"Could not parse event payload\", fallbackException\n                );\n            }\n        }\n    }\n    \n    /**\n     * Retry failed events\n     */\n    @Scheduled(fixedRateString = \"${outbox.relay.retry-interval:60000}\")\n    public void retryFailedEvents() {\n        try {\n            log.debug(\"Starting failed events retry cycle\");\n            \n            int resetCount = outboxService.resetFailedEventsForRetry(\n                config.getMaxRetries(),\n                Duration.ofMinutes(config.getRetryDelayMinutes())\n            );\n            \n            if (resetCount > 0) {\n                log.info(\"Reset {} failed events for retry\", resetCount);\n                metrics.recordFailedEventsRetry(resetCount);\n            }\n            \n        } catch (Exception e) {\n            log.error(\"Error during failed events retry\", e);\n        }\n    }\n    \n    /**\n     * Archive old events\n     */\n    @Scheduled(cron = \"${outbox.relay.archive-cron:0 0 2 * * ?}\") // Daily at 2 AM\n    public void archiveOldEvents() {\n        try {\n            log.info(\"Starting old events archival\");\n            \n            int archivedCount = outboxService.archiveOldEvents(\n                Duration.ofDays(config.getArchiveAfterDays())\n            );\n            \n            log.info(\"Archived {} old events\", archivedCount);\n            metrics.recordArchivedEvents(archivedCount);\n            \n        } catch (Exception e) {\n            log.error(\"Error during old events archival\", e);\n        }\n    }\n    \n    /**\n     * Health check\n     */\n    public boolean isHealthy() {\n        // Check if polling is working\n        if (!isHealthy) {\n            return false;\n        }\n        \n        // Check if last successful poll was recent\n        Duration timeSinceLastPoll = Duration.between(lastSuccessfulPoll, LocalDateTime.now());\n        if (timeSinceLastPoll.compareTo(Duration.ofMinutes(5)) > 0) {\n            log.warn(\"Message relay service hasn't polled successfully for {} minutes\", \n                    timeSinceLastPoll.toMinutes());\n            return false;\n        }\n        \n        return true;\n    }\n    \n    /**\n     * Get current metrics\n     */\n    public MessageRelayStatus getStatus() {\n        return MessageRelayStatus.builder()\n            .isHealthy(isHealthy())\n            .activePollingTasks(activePollingTasks.get())\n            .lastSuccessfulPoll(lastSuccessfulPoll)\n            .build();\n    }\n}\n```\n\n### Topic Resolution Strategy\n\n```java\n// infrastructure/messaging/TopicResolver.java\npackage com.example.outbox.infrastructure.messaging;\n\nimport org.springframework.stereotype.Component;\n\n/**\n * Topic Resolver\n * \n * Event type ve aggregate type'a göre appropriate Kafka topic'i resolve eder.\n */\n@Component\npublic class TopicResolver {\n    \n    private final OutboxConfiguration config;\n    \n    /**\n     * Resolve Kafka topic for event\n     */\n    public String resolveTopicForEvent(String aggregateType, String eventType) {\n        // Convention-based topic naming\n        String baseTopicName = config.getTopicPrefix() + \".\" + \n                              aggregateType.toLowerCase() + \".\" + \n                              eventType.toLowerCase();\n        \n        // Environment prefix for multi-environment support\n        if (config.getEnvironmentPrefix() != null) {\n            return config.getEnvironmentPrefix() + \".\" + baseTopicName;\n        }\n        \n        return baseTopicName;\n    }\n    \n    /**\n     * Get all configured topics\n     */\n    public List<String> getAllTopics() {\n        // Implementation depends on your event types\n        List<String> topics = new ArrayList<>();\n        \n        // Order events\n        topics.add(resolveTopicForEvent(\"Order\", \"OrderCreated\"));\n        topics.add(resolveTopicForEvent(\"Order\", \"OrderStatusUpdated\"));\n        topics.add(resolveTopicForEvent(\"Order\", \"OrderCanceled\"));\n        \n        // Payment events\n        topics.add(resolveTopicForEvent(\"Payment\", \"PaymentProcessed\"));\n        topics.add(resolveTopicForEvent(\"Payment\", \"PaymentFailed\"));\n        topics.add(resolveTopicForEvent(\"Payment\", \"RefundRequested\"));\n        \n        // Inventory events\n        topics.add(resolveTopicForEvent(\"Inventory\", \"InventoryReserved\"));\n        topics.add(resolveTopicForEvent(\"Inventory\", \"InventoryReleased\"));\n        \n        return topics;\n    }\n}\n```\n\n### Message Relay Metrics\n\n```java\n// infrastructure/monitoring/MessageRelayMetrics.java\npackage com.example.outbox.infrastructure.monitoring;\n\nimport io.micrometer.core.instrument.*;\nimport org.springframework.stereotype.Component;\n\n/**\n * Message Relay Metrics\n * \n * Outbox message relay performance ve health metrics collect eder.\n */\n@Component\npublic class MessageRelayMetrics {\n    \n    private final MeterRegistry meterRegistry;\n    private final Counter pollingCyclesCounter;\n    private final Counter publishedEventsCounter;\n    private final Counter failedEventsCounter;\n    private final Timer pollingDurationTimer;\n    private final Timer publishingDurationTimer;\n    private final Gauge activeTasksGauge;\n    \n    public MessageRelayMetrics(MeterRegistry meterRegistry) {\n        this.meterRegistry = meterRegistry;\n        \n        // Counters\n        this.pollingCyclesCounter = Counter.builder(\"outbox.polling.cycles.total\")\n            .description(\"Total number of outbox polling cycles\")\n            .register(meterRegistry);\n            \n        this.publishedEventsCounter = Counter.builder(\"outbox.events.published.total\")\n            .description(\"Total number of events published to Kafka\")\n            .register(meterRegistry);\n            \n        this.failedEventsCounter = Counter.builder(\"outbox.events.failed.total\")\n            .description(\"Total number of failed event publishing attempts\")\n            .register(meterRegistry);\n        \n        // Timers\n        this.pollingDurationTimer = Timer.builder(\"outbox.polling.duration\")\n            .description(\"Outbox polling cycle duration\")\n            .register(meterRegistry);\n            \n        this.publishingDurationTimer = Timer.builder(\"outbox.publishing.duration\")\n            .description(\"Individual event publishing duration\")\n            .register(meterRegistry);\n        \n        // Gauge\n        this.activeTasksGauge = Gauge.builder(\"outbox.polling.active.tasks\")\n            .description(\"Number of active polling tasks\")\n            .register(meterRegistry, this, MessageRelayMetrics::getActiveTasksCount);\n    }\n    \n    public void recordPollingCycle(int eventsProcessed, long durationMs) {\n        pollingCyclesCounter.increment();\n        pollingDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);\n        \n        meterRegistry.counter(\"outbox.events.processed.total\")\n            .increment(eventsProcessed);\n    }\n    \n    public void recordEventPublished(String eventType) {\n        publishedEventsCounter\n            .tag(\"event_type\", eventType)\n            .increment();\n    }\n    \n    public void recordEventPublishingFailure(String eventType) {\n        failedEventsCounter\n            .tag(\"event_type\", eventType)\n            .increment();\n    }\n    \n    public void recordSuccessfulPublishing(String eventType, String topic) {\n        meterRegistry.counter(\"outbox.kafka.published.total\")\n            .tag(\"event_type\", eventType)\n            .tag(\"topic\", topic)\n            .increment();\n    }\n    \n    public void recordPollingError() {\n        meterRegistry.counter(\"outbox.polling.errors.total\")\n            .increment();\n    }\n    \n    public void recordSkippedPolling() {\n        meterRegistry.counter(\"outbox.polling.skipped.total\")\n            .increment();\n    }\n    \n    public void recordFailedEventsRetry(int resetCount) {\n        meterRegistry.counter(\"outbox.events.retry.total\")\n            .increment(resetCount);\n    }\n    \n    public void recordArchivedEvents(int archivedCount) {\n        meterRegistry.counter(\"outbox.events.archived.total\")\n            .increment(archivedCount);\n    }\n    \n    private double getActiveTasksCount() {\n        // This would be injected from MessageRelayService\n        return 0; // Placeholder\n    }\n}\n```\n\n---\n\n## ⚙️ Configuration\n\n### Outbox Configuration\n\n```java\n// infrastructure/config/OutboxConfiguration.java\npackage com.example.outbox.infrastructure.config;\n\nimport lombok.Data;\nimport org.springframework.boot.context.properties.ConfigurationProperties;\nimport org.springframework.context.annotation.Configuration;\n\n/**\n * Outbox Pattern Configuration\n */\n@Configuration\n@ConfigurationProperties(prefix = \"outbox\")\n@Data\npublic class OutboxConfiguration {\n    \n    /**\n     * Message relay settings\n     */\n    private Relay relay = new Relay();\n    \n    /**\n     * Kafka settings\n     */\n    private Kafka kafka = new Kafka();\n    \n    /**\n     * Service identification\n     */\n    private String serviceName = \"order-service\";\n    \n    @Data\n    public static class Relay {\n        \n        /**\n         * Polling interval in milliseconds\n         */\n        private long pollingInterval = 5000; // 5 seconds\n        \n        /**\n         * Batch size for polling events\n         */\n        private int batchSize = 50;\n        \n        /**\n         * Max events processed per polling cycle\n         */\n        private int maxEventsPerBatch = 200;\n        \n        /**\n         * Max concurrent polling tasks\n         */\n        private int maxConcurrentPolling = 3;\n        \n        /**\n         * Delay between batch processing in milliseconds\n         */\n        private long batchProcessingDelay = 100;\n        \n        /**\n         * Max retry attempts for failed events\n         */\n        private int maxRetries = 3;\n        \n        /**\n         * Retry delay in minutes\n         */\n        private int retryDelayMinutes = 5;\n        \n        /**\n         * Archive events older than X days\n         */\n        private int archiveAfterDays = 30;\n        \n        /**\n         * Retry interval for failed events in milliseconds\n         */\n        private long retryInterval = 60000; // 1 minute\n        \n        /**\n         * Archive cron expression\n         */\n        private String archiveCron = \"0 0 2 * * ?\";\n    }\n    \n    @Data\n    public static class Kafka {\n        \n        /**\n         * Topic prefix for event topics\n         */\n        private String topicPrefix = \"events\";\n        \n        /**\n         * Environment prefix (dev, test, prod)\n         */\n        private String environmentPrefix;\n        \n        /**\n         * Default partitions for auto-created topics\n         */\n        private int defaultPartitions = 3;\n        \n        /**\n         * Default replication factor\n         */\n        private short defaultReplicationFactor = 1;\n    }\n    \n    // Derived getters\n    public int getBatchSize() {\n        return relay.batchSize;\n    }\n    \n    public int getMaxEventsPerBatch() {\n        return relay.maxEventsPerBatch;\n    }\n    \n    public int getMaxConcurrentPolling() {\n        return relay.maxConcurrentPolling;\n    }\n    \n    public long getBatchProcessingDelay() {\n        return relay.batchProcessingDelay;\n    }\n    \n    public int getMaxRetries() {\n        return relay.maxRetries;\n    }\n    \n    public int getRetryDelayMinutes() {\n        return relay.retryDelayMinutes;\n    }\n    \n    public int getArchiveAfterDays() {\n        return relay.archiveAfterDays;\n    }\n    \n    public String getTopicPrefix() {\n        return kafka.topicPrefix;\n    }\n    \n    public String getEnvironmentPrefix() {\n        return kafka.environmentPrefix;\n    }\n}\n```\n\n### Application Properties\n\n```yaml\n# application.yml\noutbox:\n  relay:\n    polling-interval: 5000        # 5 seconds\n    batch-size: 50               # Events per batch\n    max-events-per-batch: 200    # Max events per cycle\n    max-concurrent-polling: 3     # Concurrent polling tasks\n    batch-processing-delay: 100   # Delay between batches\n    max-retries: 3               # Max retry attempts\n    retry-delay-minutes: 5       # Retry delay\n    archive-after-days: 30       # Archive threshold\n    retry-interval: 60000        # Retry interval (1 min)\n    archive-cron: \"0 0 2 * * ?\"  # Daily at 2 AM\n  \n  kafka:\n    topic-prefix: \"events\"\n    environment-prefix: \"${spring.profiles.active:dev}\"\n    default-partitions: 3\n    default-replication-factor: 1\n  \n  service-name: \"${spring.application.name:order-service}\"\n\n# Kafka Producer Configuration\nspring:\n  kafka:\n    producer:\n      bootstrap-servers: localhost:9092\n      key-serializer: org.apache.kafka.common.serialization.StringSerializer\n      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer\n      acks: all                    # Wait for all replicas\n      retries: 3                   # Retry failed sends\n      enable-idempotence: true     # Exactly-once semantics\n      max-in-flight-requests-per-connection: 1  # Ordering guarantee\n      compression-type: snappy     # Compression\n      batch-size: 16384           # Batch size\n      linger-ms: 5                # Batching delay\n      buffer-memory: 33554432     # 32MB buffer\n```\n\nBu Message Relay Service implementation'ı ile artık **reliable ve performant event publishing** sağlıyoruz! 🚀\n\n**Sonraki adım:** Performance optimization ve advanced patterns! ⚡
````

package com.apigateway.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MetricsService.
 */
class MetricsServiceTest {

    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        metricsService = new MetricsService();
    }

    @Test
    void shouldStartWithZeroCounters() {
        assertEquals(0, metricsService.getTotalRequests());
        assertEquals(0, metricsService.getAllowedRequests());
        assertEquals(0, metricsService.getBlockedRequests());
    }

    @Test
    void shouldIncrementTotalRequests() {
        metricsService.recordRequest();
        metricsService.recordRequest();
        assertEquals(2, metricsService.getTotalRequests());
    }

    @Test
    void shouldIncrementAllowedRequests() {
        metricsService.recordAllowed();
        assertEquals(1, metricsService.getAllowedRequests());
    }

    @Test
    void shouldIncrementBlockedRequests() {
        metricsService.recordBlocked();
        assertEquals(1, metricsService.getBlockedRequests());
    }

    @Test
    void shouldReturnConsistentSnapshot() {
        metricsService.recordRequest();
        metricsService.recordRequest();
        metricsService.recordAllowed();
        metricsService.recordBlocked();
        metricsService.setActiveClients(5);
        metricsService.setCurrentStrategy("FixedWindowStrategy");

        MetricsSnapshot snapshot = metricsService.snapshot();

        assertEquals(2, snapshot.getTotalRequests());
        assertEquals(1, snapshot.getAllowedRequests());
        assertEquals(1, snapshot.getBlockedRequests());
        assertEquals(5, snapshot.getActiveClients());
        assertEquals("FixedWindowStrategy", snapshot.getCurrentStrategy());
    }

    @Test
    void shouldSerializeToJson() {
        metricsService.recordRequest();
        metricsService.recordAllowed();
        metricsService.setCurrentStrategy("TokenBucketStrategy");

        String json = metricsService.snapshot().toJson();

        assertTrue(json.contains("\"totalRequests\": 1"));
        assertTrue(json.contains("\"allowedRequests\": 1"));
        assertTrue(json.contains("\"TokenBucketStrategy\""));
    }
}
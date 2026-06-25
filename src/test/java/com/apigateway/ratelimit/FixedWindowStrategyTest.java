package com.apigateway.ratelimit;

import com.apigateway.clock.ClockProvider;
import com.apigateway.core.RateLimitConfig;
import com.apigateway.core.RateLimitResult;
import com.apigateway.core.RequestContext;
import com.apigateway.ratelimit.strategy.FixedWindowStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FixedWindowStrategy.
 *
 * Uses a fake/controllable clock so we can simulate
 * time passing without actually waiting.
 */
class FixedWindowStrategyTest {

    // Controllable clock — we set the time manually in tests
    private AtomicLong fakeTime;
    private ClockProvider fakeClock;

    private RateLimitConfig config;
    private FixedWindowStrategy strategy;

    @BeforeEach
    void setUp() {
        fakeTime  = new AtomicLong(1000L);
        fakeClock = () -> fakeTime.get(); // Lambda implements ClockProvider

        config = new RateLimitConfig.Builder()
            .algorithmType("FIXED_WINDOW")
            .maxRequests(3)
            .windowSize(Duration.ofSeconds(10))
            .build();

        strategy = new FixedWindowStrategy(config, fakeClock);
    }

    @Test
    void shouldAllowRequestsWithinLimit() {
        RequestContext context = new RequestContext("client-1", Instant.now());

        RateLimitResult r1 = strategy.allow(context);
        RateLimitResult r2 = strategy.allow(context);
        RateLimitResult r3 = strategy.allow(context);

        assertTrue(r1.isAllowed(), "Request 1 should be allowed");
        assertTrue(r2.isAllowed(), "Request 2 should be allowed");
        assertTrue(r3.isAllowed(), "Request 3 should be allowed");
    }

    @Test
    void shouldBlockRequestWhenLimitReached() {
        RequestContext context = new RequestContext("client-1", Instant.now());

        // Use up all 3 allowed requests
        strategy.allow(context);
        strategy.allow(context);
        strategy.allow(context);

        // 4th request should be blocked
        RateLimitResult result = strategy.allow(context);
        assertFalse(result.isAllowed(), "4th request should be blocked");
    }

    @Test
    void shouldTrackRemainingRequestsCorrectly() {
        RequestContext context = new RequestContext("client-1", Instant.now());

        RateLimitResult r1 = strategy.allow(context);
        assertEquals(2, r1.getRemainingRequests(), "Should have 2 remaining after 1st request");

        RateLimitResult r2 = strategy.allow(context);
        assertEquals(1, r2.getRemainingRequests(), "Should have 1 remaining after 2nd request");

        RateLimitResult r3 = strategy.allow(context);
        assertEquals(0, r3.getRemainingRequests(), "Should have 0 remaining after 3rd request");
    }

    @Test
    void shouldResetCounterAfterWindowExpires() {
        RequestContext context = new RequestContext("client-1", Instant.now());

        // Use up the limit
        strategy.allow(context);
        strategy.allow(context);
        strategy.allow(context);

        // Verify blocked
        assertFalse(strategy.allow(context).isAllowed());

        // Advance time past the window (10 seconds + 1ms)
        fakeTime.set(1000L + 10_001L);

        // Should be allowed again after window reset
        RateLimitResult result = strategy.allow(context);
        assertTrue(result.isAllowed(), "Should be allowed after window reset");
    }

    @Test
    void shouldTrackDifferentClientsIndependently() {
        RequestContext client1 = new RequestContext("client-1", Instant.now());
        RequestContext client2 = new RequestContext("client-2", Instant.now());

        // Use up client-1's limit
        strategy.allow(client1);
        strategy.allow(client1);
        strategy.allow(client1);
        assertFalse(strategy.allow(client1).isAllowed(), "client-1 should be blocked");

        // client-2 should still be allowed
        assertTrue(strategy.allow(client2).isAllowed(), "client-2 should be independent");
    }
}
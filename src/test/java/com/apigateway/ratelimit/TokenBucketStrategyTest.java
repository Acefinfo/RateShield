package com.apigateway.ratelimit;

import com.apigateway.clock.ClockProvider;
import com.apigateway.core.RateLimitConfig;
import com.apigateway.core.RateLimitResult;
import com.apigateway.core.RequestContext;
import com.apigateway.ratelimit.strategy.TokenBucketStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TokenBucketStrategy.
 */
class TokenBucketStrategyTest {

    private AtomicLong fakeTime;
    private ClockProvider fakeClock;

    private RateLimitConfig config;
    private TokenBucketStrategy strategy;

    @BeforeEach
    void setUp() {
        fakeTime  = new AtomicLong(0L);
        fakeClock = () -> fakeTime.get();

        config = new RateLimitConfig.Builder()
            .algorithmType("TOKEN_BUCKET")
            .tokenCapacity(5)
            .refillTokens(2)
            .refillDuration(Duration.ofSeconds(1))
            .build();

        strategy = new TokenBucketStrategy(config, fakeClock);
    }

    @Test
    void shouldAllowRequestsWhenTokensAvailable() {
        RequestContext context = new RequestContext("client-1", Instant.now());

        // Bucket starts full (5 tokens)
        for (int i = 0; i < 5; i++) {
            assertTrue(strategy.allow(context).isAllowed(),
                "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void shouldBlockWhenBucketIsEmpty() {
        RequestContext context = new RequestContext("client-1", Instant.now());

        // Drain all 5 tokens
        for (int i = 0; i < 5; i++) {
            strategy.allow(context);
        }

        // Next request should be blocked
        assertFalse(strategy.allow(context).isAllowed(),
            "Should be blocked when bucket is empty");
    }

    @Test
    void shouldRefillTokensAfterInterval() {
        RequestContext context = new RequestContext("client-1", Instant.now());

        // Drain all tokens
        for (int i = 0; i < 5; i++) {
            strategy.allow(context);
        }
        assertFalse(strategy.allow(context).isAllowed(), "Should be blocked");

        // Advance time by 1 second — refills 2 tokens
        fakeTime.set(1000L);

        assertTrue(strategy.allow(context).isAllowed(),
            "Should be allowed after token refill");
    }

    @Test
    void shouldNotExceedCapacityOnRefill() {
        RequestContext context = new RequestContext("client-1", Instant.now());

        // Advance time by 100 seconds — would add 200 tokens but cap is 5
        fakeTime.set(100_000L);

        // Should only allow up to capacity (5), not 200
        for (int i = 0; i < 5; i++) {
            assertTrue(strategy.allow(context).isAllowed(),
                "Request " + (i + 1) + " should be allowed up to capacity");
        }
        assertFalse(strategy.allow(context).isAllowed(),
            "Should be blocked after capacity is reached");
    }

    @Test
    void shouldTrackClientsIndependently() {
        RequestContext client1 = new RequestContext("client-1", Instant.now());
        RequestContext client2 = new RequestContext("client-2", Instant.now());

        // Drain client-1
        for (int i = 0; i < 5; i++) strategy.allow(client1);
        assertFalse(strategy.allow(client1).isAllowed());

        // client-2 should be unaffected
        assertTrue(strategy.allow(client2).isAllowed());
    }
}
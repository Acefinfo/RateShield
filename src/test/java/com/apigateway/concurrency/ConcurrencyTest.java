package com.apigateway.concurrency;

import com.apigateway.clock.ClockProvider;
import com.apigateway.core.RateLimitConfig;
import com.apigateway.core.RateLimitResult;
import com.apigateway.core.RequestContext;
import com.apigateway.metrics.MetricsService;
import com.apigateway.ratelimit.RateLimiter;
import com.apigateway.ratelimit.RateLimitStrategy;
import com.apigateway.ratelimit.strategy.FixedWindowStrategy;
import com.apigateway.ratelimit.strategy.TokenBucketStrategy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency tests — simulate hundreds of parallel requests.
 *
 * What we verify:
 * - Allowed + Blocked = Total (no requests lost or double-counted)
 * - Allowed never exceeds the configured limit
 * - No exceptions under concurrent load
 *
 * CountDownLatch usage:
 * - startLatch: holds all threads until they're all ready,
 *   then releases them simultaneously for maximum contention.
 * - doneLatch: main thread waits until all workers finish.
 */
class ConcurrencyTest {

    private static final ClockProvider FIXED_CLOCK = () -> System.currentTimeMillis();

    @Test
    void fixedWindow_shouldNeverExceedLimitUnderConcurrentLoad() throws InterruptedException {
        int maxRequests  = 50;
        int totalThreads = 200;

        RateLimitConfig config = new RateLimitConfig.Builder()
            .algorithmType("FIXED_WINDOW")
            .maxRequests(maxRequests)
            .windowSize(Duration.ofMinutes(1))
            .build();

        RateLimitStrategy strategy  = new FixedWindowStrategy(config, FIXED_CLOCK);
        MetricsService    metrics   = new MetricsService();
        RateLimiter       limiter   = new RateLimiter(strategy, metrics);

        LongAdder allowed = new LongAdder();
        LongAdder blocked = new LongAdder();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(totalThreads);

        ExecutorService pool = Executors.newFixedThreadPool(totalThreads);

        for (int i = 0; i < totalThreads; i++) {
            pool.submit(() -> {
                try {
                    startLatch.await(); // wait until all threads are ready
                    RequestContext ctx = new RequestContext("client-1", Instant.now());
                    RateLimitResult result = limiter.evaluate(ctx);
                    if (result.isAllowed()) allowed.increment();
                    else                    blocked.increment();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // release all threads at once
        doneLatch.await();      // wait for all to finish
        pool.shutdown();

        long totalAllowed  = allowed.sum();
        long totalBlocked  = blocked.sum();

        System.out.println("Fixed Window Concurrency Test:");
        System.out.println("  Allowed: " + totalAllowed);
        System.out.println("  Blocked: " + totalBlocked);
        System.out.println("  Total  : " + (totalAllowed + totalBlocked));

        // Critical assertions
        assertEquals(totalThreads, totalAllowed + totalBlocked,
            "Every request must be either allowed or blocked — none lost");

        assertTrue(totalAllowed <= maxRequests,
            "Allowed requests must never exceed the configured limit");
    }

    @Test
    void tokenBucket_shouldNeverExceedCapacityUnderConcurrentLoad() throws InterruptedException {
        int capacity     = 30;
        int totalThreads = 150;

        RateLimitConfig config = new RateLimitConfig.Builder()
            .algorithmType("TOKEN_BUCKET")
            .tokenCapacity(capacity)
            .refillTokens(5)
            .refillDuration(Duration.ofSeconds(10))
            .build();

        RateLimitStrategy strategy = new TokenBucketStrategy(config, FIXED_CLOCK);
        MetricsService    metrics  = new MetricsService();
        RateLimiter       limiter  = new RateLimiter(strategy, metrics);

        LongAdder allowed = new LongAdder();
        LongAdder blocked = new LongAdder();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(totalThreads);

        ExecutorService pool = Executors.newFixedThreadPool(totalThreads);

        for (int i = 0; i < totalThreads; i++) {
            pool.submit(() -> {
                try {
                    startLatch.await();
                    RequestContext ctx = new RequestContext("client-1", Instant.now());
                    RateLimitResult result = limiter.evaluate(ctx);
                    if (result.isAllowed()) allowed.increment();
                    else                    blocked.increment();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        pool.shutdown();

        long totalAllowed = allowed.sum();
        long totalBlocked = blocked.sum();

        System.out.println("Token Bucket Concurrency Test:");
        System.out.println("  Allowed: " + totalAllowed);
        System.out.println("  Blocked: " + totalBlocked);
        System.out.println("  Total  : " + (totalAllowed + totalBlocked));

        assertEquals(totalThreads, totalAllowed + totalBlocked,
            "Every request must be accounted for");

        assertTrue(totalAllowed <= capacity,
            "Allowed must not exceed bucket capacity");
    }

    @Test
    void metrics_shouldCountAccuratelyUnderConcurrentLoad() throws InterruptedException {
        int totalThreads = 100;
        int maxRequests  = 40;

        RateLimitConfig config = new RateLimitConfig.Builder()
            .algorithmType("FIXED_WINDOW")
            .maxRequests(maxRequests)
            .windowSize(Duration.ofMinutes(1))
            .build();

        RateLimitStrategy strategy = new FixedWindowStrategy(config, FIXED_CLOCK);
        MetricsService    metrics  = new MetricsService();
        RateLimiter       limiter  = new RateLimiter(strategy, metrics);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(totalThreads);
        ExecutorService pool      = Executors.newFixedThreadPool(totalThreads);

        for (int i = 0; i < totalThreads; i++) {
            pool.submit(() -> {
                try {
                    startLatch.await();
                    limiter.evaluate(new RequestContext("client-1", Instant.now()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        pool.shutdown();

        // Metrics must be perfectly accurate
        assertEquals(totalThreads,
            metrics.getAllowedRequests() + metrics.getBlockedRequests(),
            "Metrics allowed + blocked must equal total threads");

        assertEquals(totalThreads, metrics.getTotalRequests(),
            "Total requests in metrics must equal thread count");
    }
}
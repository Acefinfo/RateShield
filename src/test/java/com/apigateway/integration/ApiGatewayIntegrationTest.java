package com.apigateway.integration;

import com.apigateway.client.IpAddressIdentifier;
import com.apigateway.clock.SystemClockProvider;
import com.apigateway.core.RateLimitConfig;
import com.apigateway.gateway.ApiGateway;
import com.apigateway.metrics.MetricsService;
import com.apigateway.ratelimit.strategy.FixedWindowStrategy;
import com.apigateway.repository.ClientState;
import com.apigateway.repository.ClientStateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests — starts a real HTTP server on port 8081
 * and sends real HTTP requests through the full gateway stack.
 */
class ApiGatewayIntegrationTest {

    // Use 8081 to avoid conflict with a running gateway on 8080
    private static final int    TEST_PORT = 8081;
    private static final String BASE_URL  = "http://localhost:" + TEST_PORT;

    private ApiGateway gateway;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() throws Exception {
        RateLimitConfig config = new RateLimitConfig.Builder()
            .algorithmType("FIXED_WINDOW")
            .maxRequests(3)
            .windowSize(Duration.ofMinutes(1))
            .build();

        SystemClockProvider clock    = new SystemClockProvider();
        MetricsService      metrics  = new MetricsService();
        FixedWindowStrategy strategy = new FixedWindowStrategy(config, clock);

        ClientStateRepository<? extends ClientState> repo =
            strategy.getRepository();

        // Pass TEST_PORT (8081) so tests don't clash with main gateway
        gateway = new ApiGateway(
            TEST_PORT,
            config,
            strategy,
            new IpAddressIdentifier(),
            metrics,
            repo
        );

        gateway.start();
        httpClient = HttpClient.newHttpClient();

        // Give the server a moment to fully start
        Thread.sleep(200);
    }

    @AfterEach
    void tearDown() {
        if (gateway != null) {
            gateway.stop();
        }
    }

    @Test
    void shouldReturn200ForAllowedRequests() throws Exception {
        HttpResponse<String> response = sendRequest("/");
        assertEquals(200, response.statusCode(),
            "First request should return HTTP 200");
    }

    @Test
    void shouldReturn429WhenLimitExceeded() throws Exception {
        // Use up the 3-request limit
        sendRequest("/");
        sendRequest("/");
        sendRequest("/");

        // 4th request should be rate limited
        HttpResponse<String> response = sendRequest("/");
        assertEquals(429, response.statusCode(),
            "4th request should return HTTP 429");
    }

    @Test
    void shouldIncludeRateLimitHeadersOnAllowedResponse() throws Exception {
        HttpResponse<String> response = sendRequest("/");

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("X-RateLimit-Limit").isPresent(),
            "Should include X-RateLimit-Limit header");
        assertTrue(response.headers().firstValue("X-RateLimit-Remaining").isPresent(),
            "Should include X-RateLimit-Remaining header");
        assertTrue(response.headers().firstValue("X-RateLimit-Reset").isPresent(),
            "Should include X-RateLimit-Reset header");
    }

    @Test
    void shouldIncludeRetryAfterHeaderWhenBlocked() throws Exception {
        // Exhaust the limit
        sendRequest("/");
        sendRequest("/");
        sendRequest("/");

        HttpResponse<String> response = sendRequest("/");

        assertEquals(429, response.statusCode());
        assertTrue(response.headers().firstValue("Retry-After").isPresent(),
            "Blocked response should include Retry-After header");
    }

    @Test
    void shouldReturnMetricsJson() throws Exception {
        // Make some requests first so metrics are non-zero
        sendRequest("/");
        sendRequest("/");

        HttpResponse<String> response = sendRequest("/metrics");

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("totalRequests"),
            "Metrics JSON should contain totalRequests");
        assertTrue(body.contains("allowedRequests"),
            "Metrics JSON should contain allowedRequests");
        assertTrue(body.contains("blockedRequests"),
            "Metrics JSON should contain blockedRequests");
        assertTrue(body.contains("currentStrategy"),
            "Metrics JSON should contain currentStrategy");
    }

    // ── Helper ────────────────────────────────────────────────

    private HttpResponse<String> sendRequest(String path) throws Exception {
        String url = BASE_URL + path;
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
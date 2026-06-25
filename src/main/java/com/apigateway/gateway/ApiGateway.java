package com.apigateway.gateway;

import com.apigateway.cleanup.CleanupScheduler;
import com.apigateway.client.ClientIdentifier;
import com.apigateway.client.HeaderClientIdentifier;
import com.apigateway.client.IpAddressIdentifier;
import com.apigateway.clock.ClockProvider;
import com.apigateway.clock.SystemClockProvider;
import com.apigateway.core.RateLimitConfig;
import com.apigateway.core.RateLimitResult;
import com.apigateway.core.RequestContext;
import com.apigateway.factory.RateLimitStrategyFactory;
import com.apigateway.metrics.MetricsService;
import com.apigateway.metrics.MetricsSnapshot;
import com.apigateway.ratelimit.RateLimiter;
import com.apigateway.ratelimit.RateLimitStrategy;
import com.apigateway.repository.ClientState;
import com.apigateway.repository.ClientStateRepository;
import com.apigateway.ratelimit.strategy.FixedWindowStrategy;
import com.apigateway.ratelimit.strategy.TokenBucketStrategy;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;


/**
 * Lightweight API Gateway that performs request rate limiting.
 *
 * The gateway:
 * 
 *     Identifies clients using configurable identification strategies.
 *     Applies a rate-limiting algorithm.
 *     Exposes metrics through a dedicated endpoint.
 *     Periodically cleans up inactive client state.
 *
 */
public class ApiGateway {

    private static final int  DEFAULT_PORT = 8080;				// Default post used by the gateway
    private static final int  THREAD_POOL_SIZE = 10;				// Number of worker threads used by the embedded HTTP server.
    private static final long INACTIVITY_THRESHOLD = 5 * 60 * 1000L;	// Client state is removed after 5 minutes of inactivity.
    private static final long CLEANUP_INTERVAL = 60 * 1000L;		// Client state is removed after 5 minutes of inactivity.

    private final HttpServer server;
    private final RateLimiter rateLimiter;
    private final ClientIdentifier clientIdentifier;
    private final MetricsService metricsService;
    private final CleanupScheduler cleanupScheduler;
    private final RateLimitConfig config;
    private final int port;

    
    /**
     * Creates and configures a new API Gateway instance.
     *
     * @param port target port on which the HTTP server will listen
     * @param config rate-limiting configuration
     * @param strategy rate-limiting strategy implementation
     * @param clientIdentifier client identification mechanism
     * @param metricsService service responsible for collecting metrics
     * @param repository client state repository used by cleanup scheduler
     * @throws IOException if the HTTP server cannot be created
     */
    public ApiGateway(
            int port,
            RateLimitConfig config,
            RateLimitStrategy strategy,
            ClientIdentifier clientIdentifier,
            MetricsService metricsService,
            ClientStateRepository<? extends ClientState> repository) throws IOException {

        this.port = port;
        this.config = config;
        this.metricsService = metricsService;
        this.clientIdentifier = clientIdentifier;
        this.rateLimiter = new RateLimiter(strategy, metricsService);

        // Wrap the chosen strategy with the central rate limiter.
        metricsService.setCurrentStrategy(rateLimiter.getStrategyName());
        
        	// Record the active strategy for metrics visibility.
        this.cleanupScheduler = new CleanupScheduler(repository, metricsService, INACTIVITY_THRESHOLD, CLEANUP_INTERVAL);

        // Create embedded Http server
        this.server = HttpServer.create(new InetSocketAddress(port), 0);

        // Main endpoint for processing requests
        server.createContext("/", this::handleGatewayRequest);
       
        // Main endpoint for processing request 
        server.createContext("/metrics", this::handleMetricsRequest);

        // Endpoint exposing runtime metrics.
        server.setExecutor(Executors.newFixedThreadPool(THREAD_POOL_SIZE));
    }

    
    /**
     * Handles incoming client requests and applies rate limiting.
     *
     * @param exchange HTTP request/response exchange
     * @throws IOException if response writing fails
     */
    private void handleGatewayRequest(HttpExchange exchange) throws IOException {
        try {
            String clientId = clientIdentifier.identify(exchange);	// Resolve the unique client identifier
            RequestContext context = new RequestContext(clientId, Instant.now());	// Build request context used by the rate limiter
            RateLimitResult result = rateLimiter.evaluate(context);	// Evaluate whether the request should be allowed

            if (result.isAllowed()) {
                sendAllowedResponse(exchange, result);
            } else {
                sendBlockedResponse(exchange, result);
            }
        } catch (Exception e) {
            sendErrorResponse(exchange, "Internal gateway error: " + e.getMessage());
        }
    }

    /**
     * Handles requests for gateway metrics.
     *
     * @param exchange HTTP request/response exchange
     * @throws IOException if response writing fails
     */
    private void handleMetricsRequest(HttpExchange exchange) throws IOException {
        try {
            MetricsSnapshot snapshot = metricsService.snapshot();
            String json = snapshot.toJson();

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = json.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            sendErrorResponse(exchange, "Metrics error: " + e.getMessage());
        }
    }

    
    /**
     * Sends a successful response when a request passes
     * the rate-limiting checks.
     *
     * @param exchange HTTP exchange
     * @param result rate-limit evaluation result
     * @throws IOException if response writing fails
     */
    private void sendAllowedResponse(HttpExchange exchange,RateLimitResult result) throws IOException {
    	
    	 // Inform clients about their current quota status.
        exchange.getResponseHeaders().set("X-RateLimit-Limit", String.valueOf(config.getMaxRequests()));
        exchange.getResponseHeaders().set("X-RateLimit-Remaining", String.valueOf(result.getRemainingRequests()));
        exchange.getResponseHeaders().set("X-RateLimit-Reset", String.valueOf(result.getResetTimeMillis()));
        exchange.getResponseHeaders().set("Content-Type", "text/plain");

        String body  = "Request allowed\n";
        byte[] bytes = body.getBytes();
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Sends a 429 (Too Many Requests) response when the
     * rate limit has been exceeded.
     *
     * @param exchange HTTP exchange
     * @param result rate-limit evaluation result
     * @throws IOException if response writing fails
     */
    private void sendBlockedResponse(HttpExchange exchange, RateLimitResult result) throws IOException {
    	// Calculate how long the client should wait before retrying.
    	long retryAfterSeconds =(result.getResetTimeMillis() - System.currentTimeMillis()) / 1000;
        retryAfterSeconds = Math.max(1, retryAfterSeconds);	 // Ensure Retry-After is always at least one second.

        exchange.getResponseHeaders().set("Retry-After", String.valueOf(retryAfterSeconds));
        exchange.getResponseHeaders().set("Content-Type", "text/plain");

        String body  = "429 Too Many Requests - Rate limit exceeded\n";
        byte[] bytes = body.getBytes();
        exchange.sendResponseHeaders(429, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Sends a generic internal server error response.
     *
     * @param exchange HTTP exchange
     * @param message error message
     * @throws IOException if response writing fails
     */
    private void sendErrorResponse(HttpExchange exchange,
                                   String message) throws IOException {
        byte[] bytes = message.getBytes();
        exchange.sendResponseHeaders(500, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Starts the gateway server.
     */
    public void start() {
        server.start();
        System.out.println("RateShield Gateway started on: " + port);
        System.out.println("Strategy : "  + rateLimiter.getStrategyName());
        System.out.println("Endpoints: http://localhost:" + port + "/");
        System.out.println("Metrics  : http://localhost:" + port + "/metrics");
    }

    
    /**
     * Starts the gateway server.
     */
    public void stop() {
        cleanupScheduler.shutdown();
        server.stop(0);
        System.out.println("RateShield Gateway stopped.");
    }

    /**
     * Application entry point.
     *
     * Creates gateway configuration, initializes dependencies,
     * registers a shutdown hook, and starts the server.
     *
     * @param args command-line arguments
     * @throws IOException if server initialization fails
     */
    public static void main(String[] args) throws IOException {
    	// Build rate-limiting configuration.
        RateLimitConfig config = new RateLimitConfig.Builder()
            .algorithmType("FIXED_WINDOW")
            .maxRequests(10)
            .windowSize(Duration.ofMinutes(1))
            .tokenCapacity(100)
            .refillTokens(10)
            .refillDuration(Duration.ofSeconds(1))
            .build();

        ClockProvider clock = new SystemClockProvider();
        MetricsService metrics = new MetricsService();
        
        // Create strategy based on configuration.
        RateLimitStrategy strategy = RateLimitStrategyFactory.create(config, clock);
        ClientStateRepository<? extends ClientState> repository = getRepository(strategy);

        ClientIdentifier identifier = new HeaderClientIdentifier(
            "X-Client-ID", new IpAddressIdentifier());

        // Identify clients by header first, then fallback to IP address.
        ApiGateway gateway = new ApiGateway(DEFAULT_PORT, config, strategy, identifier, metrics, repository);

        // Gracefully stop the gateway on JVM shutdown.
        Runtime.getRuntime().addShutdownHook(new Thread(gateway::stop));
        gateway.start();
    }

    /**
     * Extracts the underlying repository from the configured
     * rate-limiting strategy.
     *
     * @param strategy active rate-limiting strategy
     * @return client state repository
     * @throws IllegalArgumentException if strategy type is unsupported
     */
    private static ClientStateRepository<? extends ClientState> getRepository(
            RateLimitStrategy strategy) {
        if (strategy instanceof FixedWindowStrategy) {
            return ((FixedWindowStrategy) strategy).getRepository();
        } else if (strategy instanceof TokenBucketStrategy) {
            return ((TokenBucketStrategy) strategy).getRepository();
        }
        throw new IllegalArgumentException(
            "Unknown strategy type: " + strategy.getClass().getName());
    }
}
package com.apigateway.ratelimit;

import com.apigateway.core.RateLimitResult;
import com.apigateway.core.RequestContext;
import com.apigateway.metrics.MetricsService;

/**
 * Coordinates rate-limit evaluation and metrics collection.
 *
 * This class delegates rate-limit decisions to a configured
 * RateLimitStrategy and records request statistics
 * through MetricsService.
 */
public class RateLimiter {
	
	private final RateLimitStrategy strategy;		// Strategy used to determine whether a request is allowed.
	private final MetricsService metricsService;	// Service responsible for tracking rate-limit metrics.
	
	/**
     * Creates a new RateLimiter instance.
     *
     * @param strategy the rate-limiting strategy implementation
     * @param metricsService service used to record request metrics
     * @throws IllegalArgumentException if strategy or metricsService is null
     */
	public RateLimiter(RateLimitStrategy strategy, MetricsService metricsService) {
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy must not be null");
        }
        if (metricsService == null) {
            throw new IllegalArgumentException("MetricsService must not be null");
        }
        this.strategy       = strategy;
        this.metricsService = metricsService;
    }
	
	/**
     * Evaluates whether a request should be allowed.
     *
     * The total request count is recorded before evaluation.
     * Depending on the result, either the allowed or blocked
     * request metric is incremented.
     *
     * @param context request-specific information used by the strategy
     * @return the result of the rate-limit evaluation
     */
	public RateLimitResult evaluate(RequestContext context) {
		
		metricsService.recordRequest();		// Record every incoming requests
		
		RateLimitResult result = strategy.allow(context);	// Delegate rate-limit decision to the configured strategy.
		
		// Update metrics based on the evaluation result.
		if(result.isAllowed()) {
			metricsService.recordAllowed();
		}
		else {
			metricsService.recordBlocked();
		}
		
		return result;
	}
	
	/**
     * Returns the simple class name of the configured strategy.
     *
     * Useful for logging, monitoring, or exposing diagnostics.
     *
     * @return strategy class name
     */
	public String getStrategyName() {
        return strategy.getClass().getSimpleName();
    }

}

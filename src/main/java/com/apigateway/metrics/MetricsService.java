package com.apigateway.metrics;

import java.util.concurrent.atomic.LongAdder;

/**
 * Thread safe service for trading gateway operational metrics.
 */

public class MetricsService {
	
	private final LongAdder totalRequests = new LongAdder();	// Total number of requests received by the gateway.
	private final LongAdder allowedRequests = new LongAdder();	// Number of requests successfully allowed
	private final LongAdder blockedRequests = new LongAdder();	// Number of requests blocked by rate limiting
	
	private volatile int activeClients = 0;
	
	private volatile String currentStrategy = "UNKNOWN";
	
	/**
     * Records a new incoming request.
     */
	public void recordRequest() {
		totalRequests.increment();
	}
	
	/**
     * Records a request that was allowed.
     */
	public void recordAllowed() {
		allowedRequests.increment();
	}
	
	/**
     * Records a request that was blocked.
     */
	public void recordBlocked() {
		blockedRequests.increment();
	}
	
	/**
     * Updates the number of active clients.
     *
     * @param count Current active client count
     */
	public void setActiveClients(int count) {
        this.activeClients = count;
    }

	/**
     * Updates the name of the active rate-limiting strategy.
     *
     * @param strategyName Strategy currently in use
     */
    public void setCurrentStrategy(String strategyName) {
        this.currentStrategy = strategyName;
    }
    
    /**
     * Creates an immutable snapshot of all current metrics.
     * Used by the monitoring layer and /metrics endpoint.
     *
     * @return Current metrics snapshot
     */
    public MetricsSnapshot snapshot() {
    	return new MetricsSnapshot(
    			totalRequests.sum(),
                allowedRequests.sum(),
                blockedRequests.sum(),
                activeClients,
                currentStrategy);
    }
	
    // ── Getters (for testing) ─────────────────────────────────

    public long getTotalRequests()   { return totalRequests.sum(); }
    public long getAllowedRequests()  { return allowedRequests.sum(); }
    public long getBlockedRequests() { return blockedRequests.sum(); }
    public int  getActiveClients()   { return activeClients; }
    public String getCurrentStrategy() { return currentStrategy; }

}

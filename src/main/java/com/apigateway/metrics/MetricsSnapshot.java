package com.apigateway.metrics;


/**
 * Immutable snapshot of gateway metrics at a point in time.
 * This is what gets serealized to JSON at GET metrics
 */
public class MetricsSnapshot {

	private final long totalRequests;		// Total number of request received
	private final long allowedRequests;		// Number of request that are allowed to be processed.
	private final long blockedRequests;		// Number of requests blocked due to rate limiting.
	private final int activeClients;		// Number of currently active clients being tracked
	private final String currentStrategy;	// Name of currently active ratelimiting strategy
	
	/**
	 * Creates an immutable metrics snapshot.
     *
     * @param totalRequests   Total requests received
     * @param allowedRequests Total requests allowed
     * @param blockedRequests Total requests blocked
     * @param activeClients   Number of active clients
     * @param currentStrategy Current rate-limiting strategy
	 */
	public MetricsSnapshot(long totalRequests,long allowedRequests,long blockedRequests,int activeClients,String currentStrategy) {
		this.totalRequests    = totalRequests;
        this.allowedRequests  = allowedRequests;
        this.blockedRequests  = blockedRequests;
        this.activeClients    = activeClients;
        this.currentStrategy  = currentStrategy;
	}
	
	
	/*
	 * Getter methods for all the attributes.
	 */
	public long getTotalRequests()   { return totalRequests; }
    public long getAllowedRequests()  { return allowedRequests; }
    public long getBlockedRequests() { return blockedRequests; }
    public int  getActiveClients()   { return activeClients; }
    public String getCurrentStrategy() { return currentStrategy; }
    
    /**
     * Converts the snapshot into a JSON-formatted string.
     * This JSON is returned by the metrics endpoint. 
     * 
     * @return Metrics represented as JSON
     */
    public String toJson() {
    	return "{\n" +
                "  \"totalRequests\": "   + totalRequests   + ",\n" +
                "  \"allowedRequests\": " + allowedRequests  + ",\n" +
                "  \"blockedRequests\": " + blockedRequests  + ",\n" +
                "  \"activeClients\": "  + activeClients    + ",\n" +
                "  \"currentStrategy\": \"" + currentStrategy + "\"\n" +
                "}";
    }
    
    /**
     * Returns the JSON representation of this snapshot.
     *
     * @return JSON-formatted metrics string.
     */
    @Override
    public String toString() {
    	return toJson();
    }
}

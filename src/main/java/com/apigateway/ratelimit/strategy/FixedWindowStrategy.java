package com.apigateway.ratelimit.strategy;

import com.apigateway.clock.ClockProvider;
import com.apigateway.core.RateLimitConfig;
import com.apigateway.core.RateLimitResult;
import com.apigateway.core.RequestContext;
import com.apigateway.ratelimit.RateLimitStrategy;
import com.apigateway.repository.ConcurrentHashMapClientStateRepository;
import com.apigateway.repository.FixedWindowClientState;


/**
 * Fixed Window rate limiting implementation.
 *
 * Requests are counted within a fixed time window.
 * When the window expires, the counter is reset.
 */
public class FixedWindowStrategy implements RateLimitStrategy {
	
	private final RateLimitConfig config;	// Configuration containing max requests and window size.
	private final ClockProvider clock;		// Time provider used instead of System.currentTimeMillis() to improve testability
	private final ConcurrentHashMapClientStateRepository<FixedWindowClientState> repository;	// Repository used to store per-client state.
	
	
	/**
     * Creates a Fixed Window strategy.
     *
     * @param config rate limit configuration
     * @param clock time provider
     */
	public FixedWindowStrategy(RateLimitConfig config, ClockProvider clock) {
		this.config = config;
		this.clock = clock;
		this.repository = new ConcurrentHashMapClientStateRepository<>();
		
	}
	
	/**
     * Determines whether the incoming request should be allowed.
     *
     * @param context request information
     * @return rate limit decision
     */
	@Override
	public RateLimitResult allow(RequestContext context) {
		long now      = clock.currentTimeMillis();
        long windowMs = config.getWindowSize().toMillis();
        int  maxReqs  = config.getMaxRequests();
        
        // Retrieve existing state or create a new one for this client
        FixedWindowClientState state = repository.getOrCreate(context.getClientId(),new FixedWindowClientState(now));
        
     // Synchronize on the client's state object to ensure window reset and request counting happen safely
        synchronized (state) {
        	// Reset the window if it has expired.
            if (now - state.getWindowStart() >= windowMs) {
                state.resetWindow(now);
            }
            
            
            int count = state.getRequestCount().incrementAndGet();	// Atomically increment request count.
            state.updateLastAccessTime(now);						// Update client's last access time for cleanup purposes.
            
            
            long windowEnd = state.getWindowStart() + windowMs;		 // Calculate when the current window en
            
            //  // Allow request if under limit.
            if (count <= maxReqs) {
                return RateLimitResult.allowed(maxReqs - count, windowEnd);
            } else {
                return RateLimitResult.denied(windowEnd);
            }              
        }
	}
	
	 /**
     * Exposes repository for testing and monitoring.
     *
     * @return client state repository
     */
	public ConcurrentHashMapClientStateRepository<FixedWindowClientState> getRepository() {
        return repository;
    }

}

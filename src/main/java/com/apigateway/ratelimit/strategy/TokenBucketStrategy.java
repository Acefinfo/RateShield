package com.apigateway.ratelimit.strategy;

import com.apigateway.clock.ClockProvider;
import com.apigateway.core.RateLimitConfig;
import com.apigateway.core.RateLimitResult;
import com.apigateway.core.RequestContext;
import com.apigateway.ratelimit.RateLimitStrategy;
import com.apigateway.repository.ConcurrentHashMapClientStateRepository;
import com.apigateway.repository.TokenBucketClientState;


/**
 * Token Bucket rate limiting implementing
 */
public class TokenBucketStrategy implements RateLimitStrategy {
	
	private final RateLimitConfig config;	// Rate limiting configuration containing: Bucket capacity, refill token count, refill duration
	private final ClockProvider clock;		// Time provider abstraction. This allows easier testing.
	private final ConcurrentHashMapClientStateRepository<TokenBucketClientState> repository;	// Stores the token state for each client.
	
	/**
	 * Create new Token Bucket strategy
	 * @param config rate limit configuration
	 * @param clock clock implementation used to obtain current time 
	 */
	public TokenBucketStrategy(RateLimitConfig config, ClockProvider clock) {
		this.config = config;
		this.clock = clock;
		this.repository = new ConcurrentHashMapClientStateRepository<>();
	}
	
	/**
	 * Evaluates weather a request should be allowed
	 * 
	 * Steps:
	 * - Gets the client's token bucket state.
	 * - Refill tokens if enough time has passed.
	 * - Attempt to consume a token.
	 * - Allow request if successful.
	 * - Otherwise deny the request.
	 */
	@Override
	public RateLimitResult allow(RequestContext context) {
        long now = clock.currentTimeMillis();		// Current timestamp
        int  capacity = config.getTokenCapacity();	// Maxmium number of tokens allowed in bucket
        int  refillTokens = config.getRefillTokens();	// Number of tokens added during each refill
        long refillMs = config.getRefillDuration().toMillis();	// Refill interval in milliseconds

       /*
        * Retrieve existing state for this client or create new bucket initialized with full capacity.
        */
        TokenBucketClientState state = repository.getOrCreate(
            context.getClientId(),
            new TokenBucketClientState(capacity, now)
        );

        /*
         * Synchronized on the client state so refill and consume occur as a single atomic operation
         */
        synchronized (state) {
        	
        	// Calculation of time ellapsed and number of completed refill intervals
            long elapsed = now - state.getLastRefillTime();	
            long refillPeriods = elapsed / refillMs;

            /*
             * Add tokens only if at least one refill interval has passed
             */
            if (refillPeriods > 0) {
                long tokensToAdd = refillPeriods * refillTokens;	// Total tokens to add
                long currentTokens = state.getAvailableTokens();	// Current token count
                long newTokens = Math.min(capacity, currentTokens + tokensToAdd);	// Ensure bucket never exceeds configured capacity.
                state.setAvailableTokens(newTokens);	// Updates token count

                /*
                 * Advance refill timestamp by exact number of 
                 * refill periods processed
                 */
                state.setLastRefillTime(state.getLastRefillTime() + refillPeriods * refillMs);
            }

            // Update client's last access timestamp
            state.updateLastAccessTime(now); 

            long remaining = state.getAvailableTokens();	// Current available tokens 
            
            /*
             * If tokens exist, consume one and allow request.
             */
            if (remaining > 0) {
                state.setAvailableTokens(remaining - 1);
                
                long nextRefill = state.getLastRefillTime() + refillMs;
                return RateLimitResult.allowed(remaining - 1, nextRefill);
            } 
            
            /*
             * No tokens available deny request.
             */
            else {
                long nextRefill = state.getLastRefillTime() + refillMs;
                return RateLimitResult.denied(nextRefill);
            }
        }
    }
	 /**
     * Returns the repository storing client token bucket states.
     *
     * Mainly useful for testing and monitoring.
     */
    public ConcurrentHashMapClientStateRepository<TokenBucketClientState> getRepository() {
        return repository;
    }
}
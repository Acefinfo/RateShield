package com.apigateway.factory;

import com.apigateway.clock.ClockProvider;
import com.apigateway.core.RateLimitConfig;
import com.apigateway.ratelimit.RateLimitStrategy;
import com.apigateway.ratelimit.strategy.FixedWindowStrategy;
import com.apigateway.ratelimit.strategy.TokenBucketStrategy;

/**
 * Factory creating RateLimitStretegy instances.
 */
public class RateLimitStrategyFactory {
	
	public static final String FIXED_WINDOW = "FIXED_WINDOW";	// Identifier for the Fixed Window algorithm
	public static final String TOKEN_BUCKET = "TOKEN_BUCKET";	// Identifier for the Token Bucket algorithms
	
	/**
	 * Private constructor prevents instantiation
	 * 
	 * This class is factory class and should only be accessed through its static methods
	 */
	private RateLimitStrategyFactory() {}
	
	
	/**
	 * Creates the appropriate RateLimitStrategy based on the algorithm 
	 * type defined in configurations
	 * 
	 * @param config rate limit configuration
	 * @param clock clock provider used by strategies
	 * @return concrete RateLimitStrategy implementation
	 * 
	 * @throws IllegalArgumentException if config is null,
     *         clock is null, or algorithm type is unsupported
	 */
	public static RateLimitStrategy create(RateLimitConfig config, ClockProvider clock) {
        // Validate configuration object
		if (config == null) {
            throw new IllegalArgumentException("RateLimitConfig must not be null");
        }
        // Validate clock provider
		if (clock == null) {
            throw new IllegalArgumentException("ClockProvider must not be null");
        }

        String type = config.getAlgorithmType();	// Read algorithm selected in configuration
        
        /*
         * Create the correct strategy implementation.
         * Runtime algorithm selection happens here.
         */	
        switch(type) {
        
        case FIXED_WINDOW:
        	return new FixedWindowStrategy(config, clock);
        
        case TOKEN_BUCKET:
        	return new TokenBucketStrategy(config, clock);
        
        default:
        	throw new IllegalArgumentException("Unknown rate limit algorithm: "+ type +". "+"Support types are FIXED_WINDOW and TOKEN_BUCKET");   	
        }
	}
}
	
	
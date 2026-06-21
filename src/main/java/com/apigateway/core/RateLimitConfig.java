package com.apigateway.core;

import java.time.Duration;


/**
 * Stores configuration values for rate limiting algorithms.
 * Uses Builder Pattern to create immutable objects.
 */
public class RateLimitConfig {
	
	private final String algorithmType; 	// Type of algorithm (FIXED_WINDOW, TOKEN_BUCKET, etc.)
	private final int maxRequests;			// Maxmium request allowed in a window 
	private final Duration windowSize;		// Window duration for Fixed Window algorithm
	private final int tokenCapacity;		// Maxmium token bucket capacity  
	private final int refillTokens;			// Number of tokens that will be added on refill 
	private final Duration refillDuration;	// Time interval between refills
	
	/**
     * Private constructor.
     * Can only be called through Builder.build().
     */
    private RateLimitConfig(Builder builder) {
        this.algorithmType   = builder.algorithmType;
        this.maxRequests     = builder.maxRequests;
        this.windowSize      = builder.windowSize;
        this.tokenCapacity   = builder.tokenCapacity;
        this.refillTokens    = builder.refillTokens;
        this.refillDuration  = builder.refillDuration;
    }
    
    // Read-only getters methods 
    public String getAlgorithmType() {
    	return algorithmType;
    }
    
    public int getMaxRequests() {
    	return maxRequests;
    }
    
    public Duration getWindowSize() {
    	return windowSize;
    }
    
    public int getTokenCapacity() {
    	return tokenCapacity;
    }
    
    public int getRefillTokens() {
    	return refillTokens;
    }
    
    public Duration getRefillDuration() {
    	return refillDuration;
    }
    
    
    /**
     * Builder Pattern implementation.
     * Allows fluent configuration.
     */
    public static class Builder{
    	
    	// Default values
    	 private String algorithmType = "FIXED_WINDOW";
         private int maxRequests      = 100;
         private Duration windowSize  = Duration.ofMinutes(1);
         private int tokenCapacity    = 100;
         private int refillTokens     = 10;
         private Duration refillDuration = Duration.ofSeconds(1);
         
         public Builder algorithmType(String algorithmType) {
        	 this.algorithmType = algorithmType;
        	 return this;
         }
         
         public Builder maxRequests(int maxRequests) {
             this.maxRequests = maxRequests;
             return this;
         }
         public Builder windowSize(Duration windowSize) {
             this.windowSize = windowSize;
             return this;
         }
         public Builder tokenCapacity(int tokenCapacity) {
             this.tokenCapacity = tokenCapacity;
             return this;
         }
         public Builder refillTokens(int refillTokens) {
             this.refillTokens = refillTokens;
             return this;
         }
         public Builder refillDuration(Duration refillDuration) {
             this.refillDuration = refillDuration;
             return this;
         }
         
         /**
          * Creates the immutable RateLimitConfig object.
          */
         public RateLimitConfig build() {
        	 return new RateLimitConfig(this);
         }
    }
	
	

}

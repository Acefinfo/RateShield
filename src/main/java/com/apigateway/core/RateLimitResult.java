package com.apigateway.core;


/**
 * Represents the result of rate limit check. 
 */
public class RateLimitResult {
	
	private final boolean allowed;
	private final long remainingRequests;
	private final long resetTimeMillis;
	
	/**
	 * Private constructor
	 * 
	 * This constructor is made private as to make the object creation 
	 * through the static factory methods ( i.e are: allowed() or denied())
	 * This ensures valid and consistent objects are created.
	 */
	private RateLimitResult(boolean allowed, long remainingRequests, long resetTimeMillis) {
		this.allowed = allowed;
        this.remainingRequests = remainingRequests;
        this.resetTimeMillis = resetTimeMillis;
	}
	
	/**
	 * Creates RateLimitResult representing successful request.
	 * 
	 *  @param remainingRequests number of request left in current window
	 *  @param resetTimeMillis time when the limit resets
	 *  @return an allowed RateLimitResult result
	 */
	public static RateLimitResult allowed(long remainingRequests, long resetTimeMillis) {
        return new RateLimitResult(true, remainingRequests, resetTimeMillis);
    }
	
	/**
	 * Create a RateLimitResult representing a blocked result.
	 * 
	 * Remaining requests are set to 0 by default.
	 * @param resetTimeMillis time when the limit resets
	 * @return denied RateLimitResult
	 */
	public static RateLimitResult denied(long resetTimeMillis) {
        return new RateLimitResult(false, 0, resetTimeMillis);
    }
	
	/**
     * Returns whether the request is allowed.
     *
     * @return true if allowed, false otherwise
     */
	public boolean isAllowed() {
		return allowed;
	}
	
	/**
     * Returns the number of requests remaining.
     *
     * @return remaining requests
     */
	public long getRemainingRequests() {
		return remainingRequests;
	}
	
	/**
     * Returns the time stamp (milliseconds) when
     * the rate-limit window resets.
     *
     * @return reset time stamp
     */
	public long getResetTimeMillis() {
		return resetTimeMillis;
	}
	
	/**
     * Returns a readable string representation of the object.
     * Useful for logging and debugging.
     */
	@Override
	public String toString(){
		return "RateLimitResult{allowed=" + allowed +
	               ", remainingRequests=" + remainingRequests +
	               ", resetTimeMillis=" + resetTimeMillis + "}";
	}
	
	

}

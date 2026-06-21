package com.apigateway.exception;


/**
 * Thrown when a request is rejected due to rate limiting.
 *
 * Carries the RateLimitResult so the gateway can use
 * remainingRequests and resetTimeMillis to set response headers.
 */
public class RateLimitException extends GatewayException {
	
	private final long resetTimeMillis;
	
	/**
     * @param clientId the ID of the client that exceeded the rate limit
     * @param resetTimeMillis the time (in milliseconds since epoch) when the rate limit will reset
     */
	public RateLimitException (String clientId, long resetTimeMillis ) {
		super("Rate limit exceeded for client: " + clientId);
		this .resetTimeMillis = resetTimeMillis;
	}
	
	/**
     * Returns the time (in milliseconds since epoch) when the rate limit
     * will reset for the client.
     *
     * @return the reset time in milliseconds
     */
	 public long getResetTimeMillis() {
	        return resetTimeMillis;
	    }
	
}

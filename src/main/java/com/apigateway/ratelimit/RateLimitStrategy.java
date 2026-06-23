package com.apigateway.ratelimit;

import com.apigateway.core.RateLimitResult;
import com.apigateway.core.RequestContext;

/**
 * Core strategy interface for all rate-limiting algorithms.
 *
 * Any rate-limiting implementation (Fixed Window, Token Bucket,
 * Sliding Window, etc.) must implement this interface.
 */
public interface RateLimitStrategy {
	
	/**
     * Evaluates whether the request in the given context should be allowed or denied.
     *
     * @param context information about the incoming request
     * @return the result of the rate-limit check
     */
	RateLimitResult allow(RequestContext context);
	

}

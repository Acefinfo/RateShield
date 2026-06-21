package com.apigateway.clock;


/*
 * Provides the current time
 * This is important in testing phase. 
 */
public interface ClockProvider {
	
	long currentTimeMillis();

}

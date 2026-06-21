package com.apigateway.clock;


/*
 * Implementation that uses the System clock
 */
public class SystemClockProvider implements ClockProvider {

	@Override
	public long currentTimeMillis() {
		
		// Returns the current system time in milliseconds 
		return System.currentTimeMillis();
	}
}

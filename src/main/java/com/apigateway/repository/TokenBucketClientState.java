package com.apigateway.repository;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Stores per client state for the Token bucket algorithm
 * 
 * 
 * 
 * 
 */
public class TokenBucketClientState extends ClientState {

	private final AtomicLong availableTokens;	//  Current number of available tokens for the client. AtomicLong provides thread safe update.
	private volatile long lastRefillTime;		// Time stamp when tokens were last refilled.
	
	/**
	 * Creates a new TokenBucketClientState for a client.
	 * 
	 * @param initialTokens initialize number of tokens in the bucket
	 * @param currentTime current system time when state is created
	 */
	public TokenBucketClientState(long initialTokens, long currentTime) {
		super(currentTime);
		this.availableTokens = new AtomicLong(initialTokens);
		this.lastRefillTime = currentTime;
	}
	
	/**
	 * Returns the current number of available tokens
	 */
	public long getAvailableTokens() {
		return availableTokens.get();
	}
	
	/**
	 * Updates the available token count
	 * Used by the TokenBucket strategy after a refill operations.
	 * @param tokens
	 */
	public void setAvailableTokens(long tokens) {
		availableTokens.set(tokens);
	}
	
	/**
	 * Returns the time stamp of the last refill operation
	 */
	public long getLastRefillTime() {
		return lastRefillTime;
	}
	
	/**
	 * Updates the last refill time stamp
	 * @param timeMillis time at which refill occurred
	 */
	public void setLastRefillTime(long timeMillis) {
		this.lastRefillTime = timeMillis;
	}
	
	/**
	 * Attempts to consume one token from the bucket
	 * 
	 * Uses CAS (Compare and Set) loop to safely decrement of the token count when 
	 * multiple thread access the bucket concurrently.
	 * 
	 * @return true if token was successfully consumed, false if no token are consumed.
	 * 		 
	 */
	public boolean tryConsume() {
		long current;
		do {
			current = availableTokens.get();
			if (current <= 0)
				return false;
		
		// Automatically replaces current value with current-1. If another thread changed the value first , compare and set the fails and loop retires.
		} while ( !availableTokens.compareAndSet(current, current-1) );
		return true;
	}
}

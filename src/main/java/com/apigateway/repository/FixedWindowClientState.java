package com.apigateway.repository;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stores per-client state for the Fixed Window algorithm
 */
public class FixedWindowClientState extends ClientState {
	
	// Timestamp (in milliseconds) when the current fixed window started.
	private volatile long windowStart;
	// Tracks the number of requests made within the current window. AtomicInteger provides thread safe increment 
	private final AtomicInteger requestCount;
	
	/**
     * Creates a new client state for a fixed window.
     *
     * @param windowStart start time of the current window in milliseconds
     */
	public FixedWindowClientState(long windowStart) {
		super(windowStart);
		this.windowStart  = windowStart;
        this.requestCount = new AtomicInteger(0);
	}
	
	public long getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(long windowStart) {
        this.windowStart = windowStart;
    }

    
    public AtomicInteger getRequestCount() {
        return requestCount;
    }
    
    /**
     * Resets the current window by updating its start time
     * and clearing the request count.
     *
     * @param newWindowStart start time of the new window
     */
    public void resetWindow(long newWindowStart) {
    	this.windowStart = newWindowStart;
        this.requestCount.set(0);
    }
    
    

}

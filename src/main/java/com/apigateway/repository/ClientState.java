package com.apigateway.repository;

public abstract class ClientState {

	// Tracks when this client last made a request.
	// 'volatile' ensures visibility of updates across multiple threads.
	private volatile long lastAccessTimeMillis;
	
	 /**
     * Constructor initializes the client's last access time.
     *
     * @param lastAccessTimeMillis initial timestamp of last client request in milliseconds
     */
	protected ClientState(long lastAccessTimeMillis) {
		this.lastAccessTimeMillis = lastAccessTimeMillis;
	}
	
	/**
     * Returns the last recorded access time of the client.
     *
     * @return timestamp in milliseconds of the last request
     */
	public long getLastAccessTimeMillis() {
		return lastAccessTimeMillis;
	}
	
	 /**
     * Updates the last access time of the client.
     *
     * @param timeMillis new timestamp in milliseconds representing latest request time
     */
	public void updateLastAccessTime(long timeMillis) {
		this.lastAccessTimeMillis = timeMillis;
	}
	
	
}

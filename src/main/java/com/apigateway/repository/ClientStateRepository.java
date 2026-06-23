package com.apigateway.repository;


/**
 * Generic repository interface for storing and managing per-client state.
 *
 * @param <T> the type of client state being stored
 */
public interface ClientStateRepository <T extends ClientState> {
	
	
	/**
	 * Return existing state for clientId or creates and stores  new 
	 * default state if none exists.
	 * 
	 * @param clientId
	 * @param defaultState
	 * @return
	 */
	T getOrCreate(String clientId, T defaultState);
	
	
	/**
	 * Removes state of client who have been inactive longer than threshold.
	 * 
	 * @param thresholdMillis
	 */
	void removeInactiveClient(long thresholdMillis);
	
	
	/**
     * Returns the number of clients currently tracked.
     */
	int Size();
	
	
	
}

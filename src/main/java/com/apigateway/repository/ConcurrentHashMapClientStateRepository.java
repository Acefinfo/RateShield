package com.apigateway.repository;

import java.util.concurrent.ConcurrentHashMap;


/**
 * Thread safe repository implementation that stores client states
 * using a ConcurrentHashMap.
 */
public class ConcurrentHashMapClientStateRepository <T extends ClientState> implements ClientStateRepository<T>{
	
	// Stores client states keyed by client ID.
    // ConcurrentHashMap allows safe concurrent access without locking the entire collection.
	private final ConcurrentHashMap<String, T> store = new ConcurrentHashMap<>();
	
	
	 /**
     * Returns the existing state for a client if present.
     * Otherwise, atomically stores and returns the provided default state.
     *
     * @param clientId unique identifier of the client
     * @param defaultState state to create if the client is not already tracked
     * @return existing or newly created client state
     */
	@Override
	public T getOrCreate(String clientId , T defaultState) {
		 // Atomically inserts the state only if the client ID is not already present in the map.
		T existing = store.putIfAbsent(clientId, defaultState);
		
		return existing != null ? existing : defaultState;
	}
	
	/**
     * Removes client states that have been inactive longer
     * than the specified threshold.
     *
     * @param thresholdMillis inactivity threshold in milliseconds
     */
	@Override
	public void removeInactiveClient(long thresholdMillis) {
		
		// Remove entries whose last access time is older than the provided threshold.
		store.entrySet().removeIf(entry-> entry.getValue().getLastAccessTimeMillis()< thresholdMillis);
	}
	
	/**
     * Returns the number of client states currently tracked.
     *
     * @return total number of tracked clients
     */
	@Override
	public int Size() {
		return store.size();
	}

}

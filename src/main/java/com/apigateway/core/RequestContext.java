package com.apigateway.core;

import java.time.Instant;


/**
 * Holds request-specific information for processing.
 */
public class RequestContext {
	
	private final String clientId;
	private final Instant requestTime;
	
	public RequestContext(String clientId, Instant requestTime) {
		
		// This ensures the valid client id is provided. 
		if (clientId == null || clientId.isBlank()){
			throw new IllegalArgumentException("Client id must not be null or blank");
		}
		
		// This ensures the request time stamp is provided.
		if (requestTime == null) {
			throw new IllegalArgumentException("requestTime must now be null");
		}
		
		this.clientId = clientId;
		this.requestTime = requestTime;	
		
	}
	
	
	/*
	 * Returns the client identifier. 
	 */
	public String getClientId() {
		return clientId;
	}
	
	/*
	 * Returns when the request was received. 
	 */
	public Instant getRequestTime() {
		return requestTime;
	}
	
	@Override
	public String toString() {
		return "RequestContext{clientId='" + clientId + "', requestTime=" + requestTime + "}";
	}
	
	 
	

}

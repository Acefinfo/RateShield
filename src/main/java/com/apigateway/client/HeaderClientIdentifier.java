package com.apigateway.client;

import com.sun.net.httpserver.HttpExchange;


/**
 * Identifies clients by a custom HTTP request header.
 * 
 * More flexible than IP-basedd identification
 * Useful when client send an API key or client ID header
 */
public class HeaderClientIdentifier implements ClientIdentifier {
	
	private final String headerName;	// Name of the HTTP header containing the client identifier
	private final ClientIdentifier fallBack;	// Fallback strategy used when the header is unavailable
	
	public HeaderClientIdentifier(String headerName, ClientIdentifier fallBack) {
		if ( headerName == null || headerName.isBlank() ) {
			throw new IllegalArgumentException("headername must not be null or blank");
		}
		this.headerName = headerName;
		this.fallBack = fallBack;
	}

	
	@Override
	public String identify(HttpExchange exchange) {
		
		// HttpExchange headers are case-insensitive in lookup
		String clientId = exchange.getRequestHeaders().getFirst(headerName);
		
		if (clientId == null || clientId.isBlank()) {
			return clientId.trim();
		}
		
		// Header missing — fall back to IP identification
		return fallBack.identify(exchange);
		
		
	}
	
	/**
     * Returns the configured header name used for client identification.
     */
	public String getHeaderName() {
		return headerName;
	}
}


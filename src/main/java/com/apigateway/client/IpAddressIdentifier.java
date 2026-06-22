package com.apigateway.client;

import com.sun.net.httpserver.HttpsExchange;


/**
 * Identifies clients by their remote IP address.
 * 
 * This is the simplest identification strategy.
 * Works well when the clients are distinguished by network id
 * 
 * Example output: "192.168.1.42"
 * 
 */
public class IpAddressIdentifier implements ClientIdentifier {
	
	@Override
	public String identify(HttpsExchange exchange) {
		// getRemoteAddress() returns InetSocketAddress
		// getAddress().getHostAddress() gives us just "192.168.1.42"
		return exchange.getRemoteAddress().getAddress().getHostAddress();
	}	
}

package com.apigateway.client;

import com.sun.net.httpserver.HttpExchange;


/**
 *  Strategy interface for extracting a client identity from incoming HTTPs request.
 *  
 *  
 */
public interface ClientIdentifier {

    /**
     * Extracts a unique client identifier from the HTTP request.
     *
     * @param exchange the incoming HTTP exchange
     * @return a non-null string identifying the client
     */
    String identify(HttpExchange exchange);
}
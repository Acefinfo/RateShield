package com.apigateway.exception;


/**
 *  Base exception for all RateShield gateway errors.
 */
public class GatewayException extends RuntimeException  {
	
	/**
     * Creates a new GatewayException with a custom error message.
     *
     * @param message Description of the exception.
     */
	public GatewayException(String message) {
		super(message);
	}
	
	/**
     * Creates a new GatewayException with a custom error message
     * and the underlying cause of the exception.
     *
     * @param message Description of the exception.
     * @param cause   The original exception that caused this error.
     */
	public GatewayException(String message, Throwable cause) {
		super(message, cause);
	}

}

package com.marketgrid.productservice.exception;

/**
 * Thrown when an external downstream service (e.g. vendor-service) is unavailable or unreachable.
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

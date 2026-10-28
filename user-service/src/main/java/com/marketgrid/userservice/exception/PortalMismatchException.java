package com.marketgrid.userservice.exception;

/**
 * Exception thrown when login portal hint does not match user role.
 */
public class PortalMismatchException extends RuntimeException {
    public PortalMismatchException(String message) {
        super(message);
    }
}

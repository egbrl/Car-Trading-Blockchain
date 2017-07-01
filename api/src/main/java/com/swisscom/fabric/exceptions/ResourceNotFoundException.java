package com.swisscom.fabric.exceptions;

/**
 * Created by shaun on 13.01.17.
 */
public class ResourceNotFoundException extends RuntimeException {
    private final static long serialVersionUID = 1L;

    public ResourceNotFoundException() {
        super();
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
package com.swisscom.fabric.exceptions;

/**
 * Created by shaun on 14.10.16.
 */
@SuppressWarnings("squid:UndocumentedApi")
public class InvalidIdentifierException extends ServiceException {
    private static final long serialVersionUID = 1L;

    public InvalidIdentifierException() {
        super("identifier is invalid");
    }
}

package ch.uzh.fabric.exceptions;

/**
 * Created by shaun on 13.01.17.
 */
public class TooManyRequestsException extends RuntimeException {
    private final static long serialVersionUID = 1L;

    public TooManyRequestsException() {
        super();
    }

    public TooManyRequestsException(String message) {
        super(message);
    }

    public TooManyRequestsException(String message, Throwable cause) {
        super(message, cause);
    }
}
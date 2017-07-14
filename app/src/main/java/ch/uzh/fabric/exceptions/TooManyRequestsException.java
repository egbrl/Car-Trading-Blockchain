package ch.uzh.fabric.exceptions;

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
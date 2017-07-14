package ch.uzh.fabric.exceptions;

@SuppressWarnings("squid:UndocumentedApi")
public class InvalidIdentifierException extends ServiceException {
    private static final long serialVersionUID = 1L;

    public InvalidIdentifierException() {
        super("identifier is invalid");
    }
}

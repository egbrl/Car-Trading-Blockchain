package ch.uzh.fabric.exceptions;

@SuppressWarnings("squid:UndocumentedApi")
public class PasswordNotSetException extends ServiceException {
    private static final long serialVersionUID = 1L;

    public PasswordNotSetException() {
        super("password has not been set");
    }
}

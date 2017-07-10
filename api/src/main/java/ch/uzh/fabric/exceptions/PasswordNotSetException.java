package ch.uzh.fabric.exceptions;

/**
 * Created by shaun on 14.10.16.
 */
@SuppressWarnings("squid:UndocumentedApi")
public class PasswordNotSetException extends ServiceException {
    private static final long serialVersionUID = 1L;

    public PasswordNotSetException() {
        super("password has not been set");
    }
}

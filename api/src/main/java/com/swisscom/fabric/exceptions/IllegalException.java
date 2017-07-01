package com.swisscom.fabric.exceptions;

@SuppressWarnings({"squid:UndocumentedApi", "squid:S2166"})
public class IllegalException extends ServiceException {
	private static final long serialVersionUID = 1L;
	public IllegalException() {
		super("Unavailable For Legal Reasons.");
	}
	public IllegalException(String message) {
		super(message);
	}
}

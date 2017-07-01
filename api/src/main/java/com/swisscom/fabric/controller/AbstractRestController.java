package com.swisscom.fabric.controller;

/**
 * Created by shaun on 13.01.17.
 */

import com.swisscom.fabric.config.ErrorInfo;
import com.swisscom.fabric.exceptions.ResourceNotFoundException;
import com.swisscom.fabric.exceptions.ServiceException;
import com.swisscom.fabric.exceptions.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;

@ResponseBody
public abstract class AbstractRestController {
    private final static Logger logger = LoggerFactory.getLogger(AbstractRestController.class);

    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    @ExceptionHandler({ HttpMediaTypeNotSupportedException.class })
    private ErrorInfo handleWrongContentType(HttpServletRequest request, Exception exception) {
        return new ErrorInfo(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), request.getRequestURI(),
                exception.getLocalizedMessage() + " (Accepted: 'application/json')");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({ ServiceException.class, MethodArgumentNotValidException.class })
    private ErrorInfo handleServiceException(HttpServletRequest request, Exception exception) {
        logger.warn("Request: " + request.getRequestURL() + " raised " + exception!=null?exception.getMessage():"");
        return new ErrorInfo(HttpStatus.BAD_REQUEST.value(), request.getRequestURI(), exception.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({ ResourceNotFoundException.class })
    private ErrorInfo handleResourceNotFoundException(HttpServletRequest request, ResourceNotFoundException exception) {
        return new ErrorInfo(HttpStatus.NOT_FOUND.value(), request.getRequestURI(), exception.getMessage());
    }

    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @ExceptionHandler({ TooManyRequestsException.class })
    private ErrorInfo handleTooManyRequestsException(HttpServletRequest request, TooManyRequestsException exception) {
        return new ErrorInfo(HttpStatus.TOO_MANY_REQUESTS.value(), request.getRequestURI(), exception.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler({ Throwable.class })
    private ErrorInfo handleInternalServerError(HttpServletRequest request, Exception exception) {
        logger.error("Request: " + request.getRequestURL() + " raised " + exception!=null?exception.getMessage():"", exception);
        return new ErrorInfo(HttpStatus.INTERNAL_SERVER_ERROR.value(), request.getRequestURI(), exception);
    }
}

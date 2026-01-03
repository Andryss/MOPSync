package ru.itmo.mopsync.ruleengine.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.itmo.mopsync.ruleengine.exception.BaseException;
import ru.itmo.mopsync.ruleengine.exception.Errors;
import ru.itmo.mopsync.ruleengine.generated.model.ErrorObject;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

/**
 * Global exception handler for REST API.
 * Converts exceptions to standardized ErrorObject responses.
 */
@Slf4j
@RestControllerAdvice
public class ControllerExceptionHandler {

    /**
     * Handles BaseException and returns ErrorObject.
     */
    @ExceptionHandler(BaseException.class)
    public ErrorObject handleBaseException(BaseException ex, HttpServletResponse response) {
        log.error("BaseException: code={}, message={}, humanMessage={}",
                ex.getCode(), ex.getMessage(), ex.getHumanMessage());

        response.setStatus(ex.getCode());
        return createErrorObject(ex);
    }

    /**
     * Handles HTTP message reading errors (e.g., invalid JSON).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorObject handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.error("HttpMessageNotReadableException: {}", ex.getMessage(), ex);
        String message = ex.getMessage();
        BaseException baseEx = message != null && message.contains("JSON")
                ? Errors.invalidJsonError()
                : Errors.invalidRequestBodyError(message);
        return createErrorObject(baseEx);
    }

    /**
     * Handles method argument validation errors (MethodArgumentNotValidException).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorObject handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.error("MethodArgumentNotValidException: {}", ex.getMessage(), ex);
        String message = ex.getBindingResult().getFieldError().getDefaultMessage();
        return createErrorObject(Errors.validationError(message));
    }

    /**
     * Handles method argument type mismatch errors.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorObject handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.error("MethodArgumentTypeMismatchException: {}", ex.getMessage(), ex);
        String expectedType = ex.getRequiredType().getSimpleName();
        return createErrorObject(Errors.validationError(
                String.format("Invalid parameter type '%s': expected %s", ex.getName(), expectedType)));
    }

    /**
     * Handles unsupported media type errors.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(UNSUPPORTED_MEDIA_TYPE)
    public ErrorObject handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.error("HttpMediaTypeNotSupportedException: {}", ex.getMessage(), ex);
        return createErrorObject(Errors.invalidRequestBodyError(
                "Unsupported media type: " + ex.getContentType()));
    }

    /**
     * Handles all other unhandled exceptions.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ErrorObject handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return createErrorObject(Errors.unhandledExceptionError());
    }

    /**
     * Creates ErrorObject from BaseException.
     */
    private static ErrorObject createErrorObject(BaseException ex) {
        return new ErrorObject()
                .code(ex.getCode())
                .message(ex.getMessage())
                .humanMessage(ex.getHumanMessage());
    }
}


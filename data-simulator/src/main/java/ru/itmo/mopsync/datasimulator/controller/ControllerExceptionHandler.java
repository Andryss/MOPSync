package ru.itmo.mopsync.datasimulator.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.itmo.mopsync.datasimulator.exception.BaseException;
import ru.itmo.mopsync.datasimulator.exception.Errors;
import ru.itmo.mopsync.datasimulator.generated.model.ErrorObject;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Global exception handler for REST API.
 * Converts exceptions to standardized ErrorObject responses.
 */
@Slf4j
@RestControllerAdvice
public class ControllerExceptionHandler {

    /**
     * Handles BaseException and returns ErrorObject.
     * Status code is handled by HttpServletResponse.
     */
    @ExceptionHandler(BaseException.class)
    public ErrorObject handleBaseException(BaseException ex, HttpServletResponse response) {
        log.error("BaseException: code={}, message={}, humanMessage={}",
                ex.getCode(), ex.getMessage(), ex.getHumanMessage());

        response.setStatus(ex.getCode());
        return createErrorObject(ex);
    }

    /**
     * Handles validation errors ConstraintViolationException.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorObject handleConstraintViolation(ConstraintViolationException ex) {
        log.error("ConstraintViolationException: {}", ex.getMessage(), ex);
        BaseException baseEx = Errors.validationError(ex.getMessage());
        return createErrorObject(baseEx);
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
        return createErrorObject(Errors.invalidParameterTypeError(ex.getName(), expectedType));
    }

    /**
     * Handles IllegalArgumentException and converts to BaseException.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorObject handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("IllegalArgumentException: {}", ex.getMessage(), ex);
        return createErrorObject(Errors.invalidInputError(ex.getMessage()));
    }

    /**
     * Handles IllegalStateException and converts to BaseException.
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorObject handleIllegalStateException(IllegalStateException ex) {
        log.error("IllegalStateException: {}", ex.getMessage(), ex);
        return createErrorObject(Errors.invalidInputError(ex.getMessage()));
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

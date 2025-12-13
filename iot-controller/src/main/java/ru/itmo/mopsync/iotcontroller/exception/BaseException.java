package ru.itmo.mopsync.iotcontroller.exception;

import lombok.Getter;

/**
 * Base exception for API error handling.
 * Contains error code, text identifier, and human-readable message.
 */
@Getter
public class BaseException extends RuntimeException {
    private final int code;
    private final String message;
    private final String humanMessage;

    /**
     * Constructor for BaseException.
     *
     * @param code        HTTP status code
     * @param message     error code identifier
     * @param humanMessage human-readable error message
     */
    public BaseException(int code, String message, String humanMessage) {
        super(humanMessage);
        this.code = code;
        this.message = message;
        this.humanMessage = humanMessage;
    }
}

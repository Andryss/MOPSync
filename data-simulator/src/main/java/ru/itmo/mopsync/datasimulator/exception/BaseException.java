package ru.itmo.mopsync.datasimulator.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Base exception for API error handling.
 * Contains error code, text identifier, and human-readable message.
 */
@Getter
@AllArgsConstructor
public class BaseException extends RuntimeException {
    private final int code;
    private final String message;
    private final String humanMessage;
}


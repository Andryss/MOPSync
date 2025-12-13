package ru.itmo.mopsync.iotcontroller.exception;

/**
 * Class describing all errors that occur in the application.
 */
public class Errors {
    /**
     * Unexpected unhandled error.
     */
    public static BaseException unhandledExceptionError() {
        return new BaseException(500, "internal.error", "Something went wrong...");
    }

    /**
     * Validation error (general).
     */
    public static BaseException validationError(String message) {
        return new BaseException(400, "validation.error",
                message != null ? message : "Validation error");
    }

    /**
     * Invalid JSON in request body.
     */
    public static BaseException invalidJsonError() {
        return new BaseException(400, "invalid.json.error", "Invalid JSON in request body");
    }

    /**
     * Invalid request body.
     */
    public static BaseException invalidRequestBodyError(String message) {
        return new BaseException(400, "invalid.request.body.error",
                message != null ? message : "Invalid request body");
    }
}

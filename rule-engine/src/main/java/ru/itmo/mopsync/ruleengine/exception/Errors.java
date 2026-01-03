package ru.itmo.mopsync.ruleengine.exception;

/**
 * Class describing all errors that occur in the application.
 */
public class Errors {
    /**
     * Device data not found error.
     *
     * @param deviceDataId device data identifier
     * @return BaseException for device data not found
     */
    public static BaseException deviceDataNotFoundError(String deviceDataId) {
        return new BaseException(400, "device.data.not.found",
                "Device data not found: " + deviceDataId);
    }

    /**
     * Rule not found error.
     *
     * @param ruleId rule identifier
     * @return BaseException for rule not found
     */
    public static BaseException ruleNotFoundError(String ruleId) {
        return new BaseException(400, "rule.not.found",
                "Rule not found: " + ruleId);
    }

    /**
     * Validation error.
     *
     * @param message error message
     * @return BaseException for validation error
     */
    public static BaseException validationError(String message) {
        return new BaseException(400, "validation.error", message);
    }

    /**
     * Invalid JSON error.
     *
     * @return BaseException for invalid JSON
     */
    public static BaseException invalidJsonError() {
        return new BaseException(400, "invalid.json", "Invalid JSON format");
    }

    /**
     * Invalid request body error.
     *
     * @param message error message
     * @return BaseException for invalid request body
     */
    public static BaseException invalidRequestBodyError(String message) {
        return new BaseException(400, "invalid.request.body", message != null ? message : "Invalid request body");
    }

    /**
     * Unhandled exception error.
     *
     * @return BaseException for unhandled exception
     */
    public static BaseException unhandledExceptionError() {
        return new BaseException(500, "internal.server.error", "Internal server error");
    }
}

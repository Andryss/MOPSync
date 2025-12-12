package ru.itmo.mopsync.datasimulator.exception;

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
     * Simulation is already running.
     */
    public static BaseException simulationAlreadyRunningError() {
        return new BaseException(400, "simulation.already.running.error",
                "Simulation is already running");
    }

    /**
     * No devices configured.
     */
    public static BaseException noDevicesConfiguredError() {
        return new BaseException(400, "no.devices.configured.error",
                "No devices configured. Set simulation spec first.");
    }

    /**
     * Unknown metrics template.
     */
    public static BaseException unknownMetricsTemplateError(String templateName) {
        return new BaseException(400, "unknown.metrics.template.error",
                String.format("Unknown metrics template: %s", templateName));
    }

    /**
     * Metrics template or metrics must be provided.
     */
    public static BaseException metricsRequiredError() {
        return new BaseException(400, "metrics.required.error",
                "Either metrics-template or metrics must be provided for device group");
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

    /**
     * Invalid parameter type.
     */
    public static BaseException invalidParameterTypeError(String parameterName, String expectedType) {
        return new BaseException(400, "invalid.parameter.type.error",
                String.format("Invalid parameter type '%s': expected %s", parameterName, expectedType));
    }

    /**
     * Invalid input error.
     */
    public static BaseException invalidInputError(String message) {
        return new BaseException(400, "invalid.input.error",
                message != null ? message : "Invalid input error");
    }
}


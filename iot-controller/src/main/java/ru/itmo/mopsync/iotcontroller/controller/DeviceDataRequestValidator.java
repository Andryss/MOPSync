package ru.itmo.mopsync.iotcontroller.controller;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import ru.itmo.mopsync.iotcontroller.generated.model.DeviceDataRequest;

import java.util.Map;

/**
 * Validator for DeviceDataRequest that performs complex validation checks.
 * Simple field validation (required, minLength, minimum) is handled by OpenAPI/JSR-303 annotations.
 */
@Component
public class DeviceDataRequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return DeviceDataRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        DeviceDataRequest request = (DeviceDataRequest) target;

        if (request.getMetrics() != null) {
            validateMetrics(request.getMetrics(), errors);
        }
    }

    /**
     * Validates that all metric values are either Number or String and not null.
     *
     * @param metrics metrics map to validate
     * @param errors  errors object to register validation failures
     */
    private void validateMetrics(Map<String, Object> metrics, Errors errors) {
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            String metricName = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                errors.rejectValue("metrics['" + metricName + "']", "invalid.metric.value",
                        "Metric '" + metricName + "' cannot be null");
            } else if (!(value instanceof Number) && !(value instanceof String)) {
                errors.rejectValue("metrics['" + metricName + "']", "invalid.metric.type",
                        "Metric '" + metricName + "' must be a number or string");
            }
        }
    }
}

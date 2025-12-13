package ru.itmo.mopsync.iotcontroller.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.mopsync.iotcontroller.exception.Errors;
import ru.itmo.mopsync.iotcontroller.generated.api.DeviceDataApi;
import ru.itmo.mopsync.iotcontroller.generated.model.DeviceDataRequest;
import ru.itmo.mopsync.iotcontroller.service.DeviceDataService;

/**
 * Controller for device data endpoints.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class DeviceDataApiImpl implements DeviceDataApi {

    private final DeviceDataService deviceDataService;
    private final DeviceDataRequestValidator validator;

    /**
     * Receives device data, validates it, saves to MongoDB, and sends notification to RabbitMQ.
     * Validation is performed by:
     * 1. JSR-303 annotations (required fields, minLength, minimum, minProperties) - handled automatically via @Valid
     * 2. Custom DeviceDataRequestValidator (complex metric validation) - invoked manually
     *
     * @param deviceDataRequest device data request
     */
    @Override
    public void receiveDeviceData(DeviceDataRequest deviceDataRequest) {
        // Perform custom validation for complex rules (metric value types)
        BindingResult errors = new BeanPropertyBindingResult(deviceDataRequest, "deviceDataRequest");
        validator.validate(deviceDataRequest, errors);

        if (errors.hasErrors()) {
            String message = errors.getFieldError().getDefaultMessage();
            throw Errors.validationError(message);
        }

        log.info("POST /api/v1/device-data - Receiving device data for device: {}",
                deviceDataRequest.getDeviceId());
        deviceDataService.processDeviceData(deviceDataRequest);
    }
}

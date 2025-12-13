package ru.itmo.mopsync.datasimulator.facade;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itmo.mopsync.datasimulator.generated.iotcontroller.api.DeviceDataApi;
import ru.itmo.mopsync.datasimulator.generated.iotcontroller.model.DeviceDataRequest;
import ru.itmo.mopsync.datasimulator.model.DeviceSnapshot;

/**
 * Facade for working with IoT controller API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IoTControllerFacade {

    private final DeviceDataApi deviceDataApi;

    /**
     * Sends device data to the IoT controller API.
     *
     * @param snapshot device snapshot containing data to send
     */
    public void sendDeviceData(DeviceSnapshot snapshot) {
        DeviceDataRequest request = new DeviceDataRequest()
                .deviceId(snapshot.getDeviceId())
                .timestamp(OffsetDateTime.ofInstant(snapshot.getTimestamp(), ZoneOffset.UTC))
                .seq((long) snapshot.getSequenceId())
                .metrics(snapshot.getMetrics());

        deviceDataApi.receiveDeviceData(request);
        log.debug("Sent data for device: {}", snapshot.getDeviceId());
    }
}


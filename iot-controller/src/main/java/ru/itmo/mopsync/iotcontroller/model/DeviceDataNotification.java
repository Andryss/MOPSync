package ru.itmo.mopsync.iotcontroller.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message object for device data notifications sent via RabbitMQ.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDataNotification {

    /**
     * MongoDB document ID of the device data.
     */
    private String deviceDataId;
}


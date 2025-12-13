package ru.itmo.mopsync.ruleengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * MongoDB document representing device data (shared collection with iot-controller).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "device_data")
public class DeviceDataDocument {

    @Id
    private String id;

    private String deviceId;
    private OffsetDateTime timestamp;
    private Long seq;
    private Map<String, Object> metrics;
    private Map<String, String> meta;
}

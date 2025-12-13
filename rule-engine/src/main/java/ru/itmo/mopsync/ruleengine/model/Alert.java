package ru.itmo.mopsync.ruleengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;

/**
 * MongoDB document representing an alert instance.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "alerts")
public class Alert {

    @Id
    private String id;

    private String ruleId;
    private String deviceDataId; // Reference to DeviceDataDocument that satisfied the rule
    private OffsetDateTime timestamp;
}

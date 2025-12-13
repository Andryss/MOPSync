package ru.itmo.mopsync.ruleengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

/**
 * MongoDB document representing a rule.
 * Rule content is stored as generic JSON (Map) that describes the rule logic.
 * Examples:
 * - Simple: {type:"gt", value:"3"}
 * - Repeatable: {type:"repeat", times:5, value:{type:"gt", value:"3"}}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "rules")
public class Rule {

    @Id
    private String id;

    private String deviceId;
    private String metricName;
    private Map<String, Object> ruleContent; // Generic JSON structure describing the rule
}

package ru.itmo.mopsync.ruleengine.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.mopsync.ruleengine.controller.BaseDbTest;
import ru.itmo.mopsync.ruleengine.model.DeviceDataDocument;
import ru.itmo.mopsync.ruleengine.model.Rule;
import ru.itmo.mopsync.ruleengine.repository.DeviceDataRepository;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEvaluationServiceTest extends BaseDbTest {

    @Autowired
    private RuleEvaluationService ruleEvaluationService;

    @Autowired
    private DeviceDataRepository deviceDataRepository;

    @BeforeEach
    void setUp() {
        deviceDataRepository.deleteAll();
    }

    @Test
    void testEvaluateRuleGreaterThanNumber() {
        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "gt",
                "value", 25.0
        ));

        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 30.0));

        boolean result = ruleEvaluationService.evaluateRule(rule, deviceData);
        assertThat(result).isTrue();
    }

    @Test
    void testEvaluateRuleLessThanNumber() {
        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "lt",
                "value", 25.0
        ));

        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 20.0));

        boolean result = ruleEvaluationService.evaluateRule(rule, deviceData);
        assertThat(result).isTrue();
    }

    @Test
    void testEvaluateRuleGreaterThanOrEqualNumber() {
        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "gte",
                "value", 25.0
        ));

        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 25.0));

        boolean result = ruleEvaluationService.evaluateRule(rule, deviceData);
        assertThat(result).isTrue();
    }

    @Test
    void testEvaluateRuleLessThanOrEqualNumber() {
        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "lte",
                "value", 25.0
        ));

        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 25.0));

        boolean result = ruleEvaluationService.evaluateRule(rule, deviceData);
        assertThat(result).isTrue();
    }

    @Test
    void testEvaluateRuleEqualNumber() {
        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "eq",
                "value", 25.0
        ));

        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 25.0));

        boolean result = ruleEvaluationService.evaluateRule(rule, deviceData);
        assertThat(result).isTrue();
    }

    @Test
    void testEvaluateRuleGreaterThanString() {
        Rule rule = createRule("device-1", "status", Map.of(
                "type", "gt",
                "value", "active"
        ));

        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("status", "online"));

        boolean result = ruleEvaluationService.evaluateRule(rule, deviceData);
        assertThat(result).isTrue();
    }

    @Test
    void testEvaluateRuleEqualString() {
        Rule rule = createRule("device-1", "status", Map.of(
                "type", "eq",
                "value", "active"
        ));

        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("status", "active"));

        boolean result = ruleEvaluationService.evaluateRule(rule, deviceData);
        assertThat(result).isTrue();
    }

    @Test
    void testEvaluateRuleWithInteger() {
        Rule rule = createRule("device-1", "count", Map.of(
                "type", "gt",
                "value", 10
        ));

        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("count", 15));

        boolean result = ruleEvaluationService.evaluateRule(rule, deviceData);
        assertThat(result).isTrue();
    }

    @Test
    void testEvaluateRuleWithStringThreshold() {
        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "gt",
                "value", "25.0"
        ));

        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 30.0));

        boolean result = ruleEvaluationService.evaluateRule(rule, deviceData);
        assertThat(result).isTrue();
    }

    @Test
    void testEvaluateRuleRepeat() {
        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "repeat",
                "times", 3,
                "value", Map.of(
                        "type", "gt",
                        "value", 25.0
                )
        ));

        // Create 3 consecutive packages with temperature > 25
        for (long seq = 1; seq <= 3; seq++) {
            DeviceDataDocument deviceData = createDeviceData("device-1", seq, Map.of("temperature", 30.0));
            deviceDataRepository.save(deviceData);
        }

        DeviceDataDocument latestData = createDeviceData("device-1", 3L, Map.of("temperature", 30.0));
        boolean result = ruleEvaluationService.evaluateRule(rule, latestData);
        assertThat(result).isTrue();
    }

    @Test
    void testEvaluateRuleRepeatNotEnoughPackages() {
        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "repeat",
                "times", 5,
                "value", Map.of(
                        "type", "gt",
                        "value", 25.0
                )
        ));

        // Create only 3 packages, need 5
        for (long seq = 1; seq <= 3; seq++) {
            DeviceDataDocument deviceData = createDeviceData("device-1", seq, Map.of("temperature", 30.0));
            deviceDataRepository.save(deviceData);
        }

        DeviceDataDocument latestData = createDeviceData("device-1", 3L, Map.of("temperature", 30.0));
        boolean result = ruleEvaluationService.evaluateRule(rule, latestData);
        assertThat(result).isFalse();
    }

    @Test
    void testEvaluateRuleRepeatNotSatisfied() {
        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "repeat",
                "times", 3,
                "value", Map.of(
                        "type", "gt",
                        "value", 25.0
                )
        ));

        // Create 3 packages, but one doesn't satisfy the condition
        DeviceDataDocument deviceData1 = createDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        DeviceDataDocument deviceData2 = createDeviceData("device-1", 2L, Map.of("temperature", 20.0)); // < 25
        DeviceDataDocument deviceData3 = createDeviceData("device-1", 3L, Map.of("temperature", 30.0));
        deviceDataRepository.save(deviceData1);
        deviceDataRepository.save(deviceData2);
        deviceDataRepository.save(deviceData3);

        DeviceDataDocument latestData = createDeviceData("device-1", 3L, Map.of("temperature", 30.0));
        boolean result = ruleEvaluationService.evaluateRule(rule, latestData);
        assertThat(result).isFalse();
    }

    @Test
    void testEvaluateRuleWithNullRuleContent() {
        Rule rule = new Rule();
        rule.setId("rule-1");
        rule.setDeviceId("device-1");
        rule.setMetricName("temperature");
        rule.setRuleContent(null);

        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 30.0));

        boolean result = ruleEvaluationService.evaluateRule(rule, deviceData);
        assertThat(result).isFalse();
    }

    @Test
    void testEvaluateRuleWithMissingMetric() {
        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "gt",
                "value", 25.0
        ));

        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("humidity", 60.0));

        boolean result = ruleEvaluationService.evaluateRule(rule, deviceData);
        assertThat(result).isFalse();
    }

    @Test
    void testEvaluateRuleWithUnsupportedType() {
        Rule rule = createRule("device-1", "data", Map.of(
                "type", "gt",
                "value", 25.0
        ));

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("data", new Object()); // Unsupported type
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, metrics);

        boolean result = ruleEvaluationService.evaluateRule(rule, deviceData);
        assertThat(result).isFalse();
    }

    private Rule createRule(String deviceId, String metricName, Map<String, Object> ruleContent) {
        Rule rule = new Rule();
        rule.setId("rule-" + deviceId + "-" + metricName);
        rule.setDeviceId(deviceId);
        rule.setMetricName(metricName);
        rule.setRuleContent(ruleContent);
        return rule;
    }

    private DeviceDataDocument createDeviceData(String deviceId, Long seq, Map<String, Object> metrics) {
        DeviceDataDocument deviceData = new DeviceDataDocument();
        deviceData.setDeviceId(deviceId);
        deviceData.setSeq(seq);
        deviceData.setTimestamp(OffsetDateTime.now());
        deviceData.setMetrics(metrics);
        return deviceData;
    }
}

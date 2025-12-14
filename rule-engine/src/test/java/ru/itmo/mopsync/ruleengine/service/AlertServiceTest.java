package ru.itmo.mopsync.ruleengine.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.mopsync.ruleengine.controller.BaseDbTest;
import ru.itmo.mopsync.ruleengine.model.Alert;
import ru.itmo.mopsync.ruleengine.model.DeviceDataDocument;
import ru.itmo.mopsync.ruleengine.model.Rule;
import ru.itmo.mopsync.ruleengine.repository.AlertRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AlertServiceTest extends BaseDbTest {

    @Autowired
    private AlertService alertService;

    @Autowired
    private AlertRepository alertRepository;

    @BeforeEach
    void setUp() {
        // Clean up is done per test, container might not be ready in @BeforeEach
        try {
            alertRepository.deleteAll();
        } catch (Exception e) {
            // Ignore if container not ready yet, will be cleaned in test
        }
    }

    @Test
    void testCreateAlert() {
        Rule rule = createRule("rule-1", "device-1", "temperature");
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 30.0));

        alertService.createAlert(rule, deviceData);

        List<Alert> alerts = alertRepository.findAll();
        assertThat(alerts).hasSize(1);
        Alert alert = alerts.get(0);
        assertThat(alert.getRuleId()).isEqualTo(rule.getId());
        assertThat(alert.getDeviceDataId()).isEqualTo(deviceData.getId());
        assertThat(alert.getTimestamp()).isNotNull();
        assertThat(alert.getId()).isNotNull();
    }

    @Test
    void testCreateMultipleAlerts() {
        Rule rule1 = createRule("rule-1", "device-1", "temperature");
        Rule rule2 = createRule("rule-2", "device-1", "humidity");
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 30.0));

        alertService.createAlert(rule1, deviceData);
        alertService.createAlert(rule2, deviceData);

        List<Alert> alerts = alertRepository.findAll();
        assertThat(alerts).hasSize(2);
    }

    private Rule createRule(String ruleId, String deviceId, String metricName) {
        Rule rule = new Rule();
        rule.setId(ruleId);
        rule.setDeviceId(deviceId);
        rule.setMetricName(metricName);
        rule.setRuleContent(Map.of("type", "gt", "value", 25.0));
        return rule;
    }

    private DeviceDataDocument createDeviceData(String deviceId, Long seq, Map<String, Object> metrics) {
        DeviceDataDocument deviceData = new DeviceDataDocument();
        deviceData.setId("data-" + deviceId + "-" + seq);
        deviceData.setDeviceId(deviceId);
        deviceData.setSeq(seq);
        deviceData.setTimestamp(OffsetDateTime.now());
        deviceData.setMetrics(metrics);
        return deviceData;
    }
}

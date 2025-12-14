package ru.itmo.mopsync.ruleengine.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.mopsync.ruleengine.controller.BaseDbTest;
import ru.itmo.mopsync.ruleengine.model.DeviceDataDocument;
import ru.itmo.mopsync.ruleengine.model.Rule;
import ru.itmo.mopsync.ruleengine.repository.AlertRepository;
import ru.itmo.mopsync.ruleengine.repository.DeviceDataRepository;
import ru.itmo.mopsync.ruleengine.repository.RuleRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceDataProcessingServiceTest extends BaseDbTest {

    @Autowired
    private DeviceDataProcessingService deviceDataProcessingService;

    @Autowired
    private DeviceDataRepository deviceDataRepository;

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private AlertRepository alertRepository;

    @BeforeEach
    void setUp() {
        deviceDataRepository.deleteAll();
        ruleRepository.deleteAll();
        alertRepository.deleteAll();
    }

    @Test
    void testProcessDeviceDataWithSatisfiedRule() {
        // Create device data
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        deviceData = deviceDataRepository.save(deviceData);

        // Create rule
        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "gt",
                "value", 25.0
        ));
        rule = ruleRepository.save(rule);

        // Process device data
        deviceDataProcessingService.processDeviceData(deviceData.getId());

        // Verify alert was created
        List<ru.itmo.mopsync.ruleengine.model.Alert> alerts = alertRepository.findAll();
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getRuleId()).isEqualTo(rule.getId());
        assertThat(alerts.get(0).getDeviceDataId()).isEqualTo(deviceData.getId());
    }

    @Test
    void testProcessDeviceDataWithNotSatisfiedRule() {
        // Create device data
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 20.0));
        deviceData = deviceDataRepository.save(deviceData);

        // Create rule
        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "gt",
                "value", 25.0
        ));
        rule = ruleRepository.save(rule);

        // Process device data
        deviceDataProcessingService.processDeviceData(deviceData.getId());

        // Verify no alert was created
        List<ru.itmo.mopsync.ruleengine.model.Alert> alerts = alertRepository.findAll();
        assertThat(alerts).isEmpty();
    }

    @Test
    void testProcessDeviceDataWithMultipleRules() {
        // Create device data
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of(
                "temperature", 30.0,
                "humidity", 70.0
        ));
        deviceData = deviceDataRepository.save(deviceData);

        // Create multiple rules
        Rule rule1 = createRule("device-1", "temperature", Map.of("type", "gt", "value", 25.0));
        Rule rule2 = createRule("device-1", "humidity", Map.of("type", "gt", "value", 60.0));
        rule1 = ruleRepository.save(rule1);
        rule2 = ruleRepository.save(rule2);

        // Process device data
        deviceDataProcessingService.processDeviceData(deviceData.getId());

        // Verify both alerts were created
        List<ru.itmo.mopsync.ruleengine.model.Alert> alerts = alertRepository.findAll();
        assertThat(alerts).hasSize(2);
    }

    @Test
    void testProcessDeviceDataWithNoRules() {
        // Create device data
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        deviceData = deviceDataRepository.save(deviceData);

        // Process device data (no rules exist)
        deviceDataProcessingService.processDeviceData(deviceData.getId());

        // Verify no alert was created
        List<ru.itmo.mopsync.ruleengine.model.Alert> alerts = alertRepository.findAll();
        assertThat(alerts).isEmpty();
    }

    @Test
    void testProcessDeviceDataWithNoMetrics() {
        // Create device data without metrics
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of());
        deviceData = deviceDataRepository.save(deviceData);

        // Create rule
        Rule rule = createRule("device-1", "temperature", Map.of("type", "gt", "value", 25.0));
        ruleRepository.save(rule);

        // Process device data
        deviceDataProcessingService.processDeviceData(deviceData.getId());

        // Verify no alert was created
        List<ru.itmo.mopsync.ruleengine.model.Alert> alerts = alertRepository.findAll();
        assertThat(alerts).isEmpty();
    }

    @Test
    void testProcessDeviceDataNotFound() {
        // Try to process non-existent device data
        assertThatThrownBy(() -> deviceDataProcessingService.processDeviceData("non-existent-id"))
                .isInstanceOf(ru.itmo.mopsync.ruleengine.exception.BaseException.class)
                .satisfies(exception -> {
                    ru.itmo.mopsync.ruleengine.exception.BaseException baseException =
                            (ru.itmo.mopsync.ruleengine.exception.BaseException) exception;
                    assertThat(baseException.getCode()).isEqualTo(404);
                    assertThat(baseException.getMessage()).isEqualTo("device.data.not.found");
                });
    }

    @Test
    void testProcessDeviceDataWithRepeatRule() {
        // Create 3 consecutive packages
        for (long seq = 1; seq <= 3; seq++) {
            DeviceDataDocument deviceData = createDeviceData("device-1", seq, Map.of("temperature", 30.0));
            deviceDataRepository.save(deviceData);
        }

        // Get the latest one
        DeviceDataDocument latestData = deviceDataRepository.findByDeviceIdOrderBySeqDesc("device-1",
                org.springframework.data.domain.PageRequest.of(0, 1)).get(0);

        // Create repeat rule
        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "repeat",
                "times", 3,
                "value", Map.of("type", "gt", "value", 25.0)
        ));
        rule = ruleRepository.save(rule);

        // Process device data
        deviceDataProcessingService.processDeviceData(latestData.getId());

        // Verify alert was created
        List<ru.itmo.mopsync.ruleengine.model.Alert> alerts = alertRepository.findAll();
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getRuleId()).isEqualTo(rule.getId());
    }

    private Rule createRule(String deviceId, String metricName, Map<String, Object> ruleContent) {
        Rule rule = new Rule();
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

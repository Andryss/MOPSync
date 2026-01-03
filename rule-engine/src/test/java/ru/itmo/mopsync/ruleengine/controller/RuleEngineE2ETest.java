package ru.itmo.mopsync.ruleengine.controller;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import ru.itmo.mopsync.ruleengine.config.RabbitQueueProperties;
import ru.itmo.mopsync.ruleengine.model.Alert;
import ru.itmo.mopsync.ruleengine.model.DeviceDataDocument;
import ru.itmo.mopsync.ruleengine.model.DeviceDataNotification;
import ru.itmo.mopsync.ruleengine.model.Rule;
import ru.itmo.mopsync.ruleengine.repository.AlertRepository;
import ru.itmo.mopsync.ruleengine.repository.DeviceDataRepository;
import ru.itmo.mopsync.ruleengine.repository.RuleRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end tests for rule-engine service.
 * Tests the full flow: RabbitMQ message -> Listener -> Processing -> Alert creation.
 */
class RuleEngineE2ETest extends BaseDbTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitQueueProperties rabbitQueueProperties;

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
    void testE2EWithSatisfiedRule() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        Rule rule = saveRule("device-1", "temperature", Map.of("type", "gt", "value", 25.0));

        sendAndWaitForAlert(deviceData.getId(), rule.getId(), 1);
    }

    @Test
    void testE2EWithNotSatisfiedRule() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("temperature", 20.0));
        saveRule("device-1", "temperature", Map.of("type", "gt", "value", 25.0));

        sendAndWaitForNoAlert(deviceData.getId());
    }

    @Test
    void testE2EWithMultipleRules() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of(
                "temperature", 30.0,
                "humidity", 70.0
        ));
        saveRule("device-1", "temperature", Map.of("type", "gt", "value", 25.0));
        saveRule("device-1", "humidity", Map.of("type", "gt", "value", 60.0));

        sendAndWaitForAlert(deviceData.getId(), null, 2);
    }

    @Test
    void testE2EWithRepeatRule() {
        createConsecutivePackages("device-1", 3, Map.of("temperature", 30.0));
        DeviceDataDocument latestData = getLatestDeviceData("device-1");
        Rule rule = saveRule("device-1", "temperature", Map.of(
                "type", "repeat",
                "times", 3,
                "value", Map.of("type", "gt", "value", 25.0)
        ));

        sendAndWaitForAlert(latestData.getId(), rule.getId(), 1);
    }

    @Test
    void testE2EWithNoRules() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        sendAndWaitForNoAlert(deviceData.getId());
    }

    @Test
    void testE2EWithLessThanRule() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("temperature", 20.0));
        Rule rule = saveRule("device-1", "temperature", Map.of("type", "lt", "value", 25.0));
        sendAndWaitForAlert(deviceData.getId(), rule.getId(), 1);
    }

    @Test
    void testE2EWithGreaterThanOrEqualRule() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("temperature", 25.0));
        Rule rule = saveRule("device-1", "temperature", Map.of("type", "gte", "value", 25.0));
        sendAndWaitForAlert(deviceData.getId(), rule.getId(), 1);
    }

    @Test
    void testE2EWithLessThanOrEqualRule() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("temperature", 25.0));
        Rule rule = saveRule("device-1", "temperature", Map.of("type", "lte", "value", 25.0));
        sendAndWaitForAlert(deviceData.getId(), rule.getId(), 1);
    }

    @Test
    void testE2EWithEqualRule() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("temperature", 25.0));
        Rule rule = saveRule("device-1", "temperature", Map.of("type", "eq", "value", 25.0));
        sendAndWaitForAlert(deviceData.getId(), rule.getId(), 1);
    }

    @Test
    void testE2EWithStringComparison() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("status", "active"));
        Rule rule = saveRule("device-1", "status", Map.of("type", "eq", "value", "active"));
        sendAndWaitForAlert(deviceData.getId(), rule.getId(), 1);
    }

    @Test
    void testE2EWithIntegerType() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("count", 100));
        Rule rule = saveRule("device-1", "count", Map.of("type", "gt", "value", 50));
        sendAndWaitForAlert(deviceData.getId(), rule.getId(), 1);
    }

    @Test
    void testE2EWithCrossTypeNumericComparison() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("temperature", 30));
        Rule rule = saveRule("device-1", "temperature", Map.of("type", "gt", "value", 25.0));
        sendAndWaitForAlert(deviceData.getId(), rule.getId(), 1);
    }

    @Test
    void testE2EWithNoMetrics() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of());
        saveRule("device-1", "temperature", Map.of("type", "gt", "value", 25.0));
        sendAndWaitForNoAlert(deviceData.getId());
    }

    @Test
    void testE2EWithMissingMetricInData() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("humidity", 50.0));
        saveRule("device-1", "temperature", Map.of("type", "gt", "value", 25.0));
        sendAndWaitForNoAlert(deviceData.getId());
    }

    @Test
    void testE2EWithRepeatRuleNotEnoughPackages() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        saveRule("device-1", "temperature", Map.of(
                "type", "repeat",
                "times", 3,
                "value", Map.of("type", "gt", "value", 25.0)
        ));
        sendAndWaitForNoAlert(deviceData.getId());
    }

    @Test
    void testE2EWithStringGreaterThan() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("status", "zebra"));
        Rule rule = saveRule("device-1", "status", Map.of("type", "gt", "value", "apple"));
        sendAndWaitForAlert(deviceData.getId(), rule.getId(), 1);
    }

    @Test
    void testE2EWithStringLessThan() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("status", "apple"));
        Rule rule = saveRule("device-1", "status", Map.of("type", "lt", "value", "zebra"));
        sendAndWaitForAlert(deviceData.getId(), rule.getId(), 1);
    }

    @Test
    void testE2EWithLongType() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("count", 1000L));
        Rule rule = saveRule("device-1", "count", Map.of("type", "gt", "value", 500L));
        sendAndWaitForAlert(deviceData.getId(), rule.getId(), 1);
    }

    @Test
    void testE2EWithStringNumericValue() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("temperature", "30"));
        Rule rule = saveRule("device-1", "temperature", Map.of("type", "gt", "value", 25.0));
        sendAndWaitForAlert(deviceData.getId(), rule.getId(), 1);
    }

    @Test
    void testE2EWithRepeatRuleStringTimes() {
        createConsecutivePackages("device-1", 3, Map.of("temperature", 30.0));
        DeviceDataDocument latestData = getLatestDeviceData("device-1");
        Rule rule = saveRule("device-1", "temperature", Map.of(
                "type", "repeat",
                "times", "3",
                "value", Map.of("type", "gt", "value", 25.0)
        ));
        sendAndWaitForAlert(latestData.getId(), rule.getId(), 1);
    }

    @Test
    void testE2EWithEqualInteger() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("count", 100));
        Rule rule = saveRule("device-1", "count", Map.of("type", "eq", "value", 100));
        sendAndWaitForAlert(deviceData.getId(), rule.getId(), 1);
    }

    @Test
    void testE2EWithEqualLong() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("count", 1000L));
        Rule rule = saveRule("device-1", "count", Map.of("type", "eq", "value", 1000L));
        sendAndWaitForAlert(deviceData.getId(), rule.getId(), 1);
    }

    @Test
    void testE2EWithInvalidDeviceDataId() {
        DeviceDataNotification notification = new DeviceDataNotification("non-existent-id");
        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), notification);
        sendAndWaitForNoAlert("non-existent-id");
    }

    @Test
    void testE2EWithNullRuleContent() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        Rule rule = new Rule();
        rule.setDeviceId("device-1");
        rule.setMetricName("temperature");
        rule.setRuleContent(null);
        ruleRepository.save(rule);
        sendAndWaitForNoAlert(deviceData.getId());
    }

    @Test
    void testE2EWithInvalidRuleType() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        saveRule("device-1", "temperature", Map.of("type", "invalid", "value", 25.0));
        sendAndWaitForNoAlert(deviceData.getId());
    }

    @Test
    void testE2EWithMissingValueInRule() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        saveRule("device-1", "temperature", Map.of("type", "gt"));
        sendAndWaitForNoAlert(deviceData.getId());
    }

    @Test
    void testE2EWithNonNumericStringInNumericComparison() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("temperature", "not-a-number"));
        saveRule("device-1", "temperature", Map.of("type", "gt", "value", 25.0));
        sendAndWaitForNoAlert(deviceData.getId());
    }

    @Test
    void testE2EWithRepeatRuleMissingValue() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        saveRule("device-1", "temperature", Map.of("type", "repeat", "times", 3));
        sendAndWaitForNoAlert(deviceData.getId());
    }

    @Test
    void testE2EWithRepeatRuleInvalidTimes() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        saveRule("device-1", "temperature", Map.of(
                "type", "repeat",
                "times", -1,
                "value", Map.of("type", "gt", "value", 25.0)
        ));
        sendAndWaitForNoAlert(deviceData.getId());
    }

    @Test
    void testE2EWithRepeatRuleMissingMetricInPackage() {
        for (long seq = 1; seq <= 3; seq++) {
            Map<String, Object> metrics = seq == 2 ? Map.of("humidity", 50.0) : Map.of("temperature", 30.0);
            saveDeviceData("device-1", seq, metrics);
        }
        DeviceDataDocument latestData = getLatestDeviceData("device-1");
        saveRule("device-1", "temperature", Map.of(
                "type", "repeat",
                "times", 3,
                "value", Map.of("type", "gt", "value", 25.0)
        ));
        sendAndWaitForNoAlert(latestData.getId());
    }

    @Test
    void testE2EWithRepeatRulePackageNotSatisfyingCondition() {
        for (long seq = 1; seq <= 3; seq++) {
            double temp = seq == 2 ? 20.0 : 30.0;
            saveDeviceData("device-1", seq, Map.of("temperature", temp));
        }
        DeviceDataDocument latestData = getLatestDeviceData("device-1");
        saveRule("device-1", "temperature", Map.of(
                "type", "repeat",
                "times", 3,
                "value", Map.of("type", "gt", "value", 25.0)
        ));
        sendAndWaitForNoAlert(latestData.getId());
    }

    @Test
    void testE2EWithDifferentDeviceId() {
        DeviceDataDocument deviceData = saveDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        saveRule("device-2", "temperature", Map.of("type", "gt", "value", 25.0));
        sendAndWaitForNoAlert(deviceData.getId());
    }

    private DeviceDataDocument saveDeviceData(String deviceId, Long seq, Map<String, Object> metrics) {
        DeviceDataDocument deviceData = new DeviceDataDocument();
        deviceData.setDeviceId(deviceId);
        deviceData.setSeq(seq);
        deviceData.setTimestamp(OffsetDateTime.now());
        deviceData.setMetrics(metrics);
        return deviceDataRepository.save(deviceData);
    }

    private Rule saveRule(String deviceId, String metricName, Map<String, Object> ruleContent) {
        Rule rule = new Rule();
        rule.setDeviceId(deviceId);
        rule.setMetricName(metricName);
        rule.setRuleContent(ruleContent);
        return ruleRepository.save(rule);
    }

    private void createConsecutivePackages(String deviceId, int count, Map<String, Object> metrics) {
        for (long seq = 1; seq <= count; seq++) {
            saveDeviceData(deviceId, seq, metrics);
        }
    }

    private DeviceDataDocument getLatestDeviceData(String deviceId) {
        return deviceDataRepository.findByDeviceIdOrderBySeqDesc(deviceId, PageRequest.of(0, 1)).get(0);
    }

    private void sendAndWaitForAlert(String deviceDataId, String expectedRuleId, int expectedAlertCount) {
        DeviceDataNotification notification = new DeviceDataNotification(deviceDataId);
        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), notification);
        await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(expectedAlertCount);
            if (expectedRuleId != null && expectedAlertCount == 1) {
                assertThat(alerts.get(0).getRuleId()).isEqualTo(expectedRuleId);
            }
        });
    }

    private void sendAndWaitForNoAlert(String deviceDataId) {
        DeviceDataNotification notification = new DeviceDataNotification(deviceDataId);
        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), notification);
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).isEmpty();
        });
    }
}

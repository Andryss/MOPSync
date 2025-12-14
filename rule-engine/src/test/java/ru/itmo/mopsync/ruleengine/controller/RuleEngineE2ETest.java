package ru.itmo.mopsync.ruleengine.controller;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.mopsync.ruleengine.config.RabbitQueueProperties;
import ru.itmo.mopsync.ruleengine.model.Alert;
import ru.itmo.mopsync.ruleengine.model.DeviceDataDocument;
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
        // Create device data
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        deviceData = deviceDataRepository.save(deviceData);

        // Create rule
        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "gt",
                "value", 25.0
        ));
        rule = ruleRepository.save(rule);

        // Send message to RabbitMQ (send as plain string, not JSON)
        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        // Wait for async processing and verify alert was created
        final String ruleId = rule.getId();
        final String deviceDataId = deviceData.getId();
        await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getRuleId()).isEqualTo(ruleId);
            assertThat(alerts.get(0).getDeviceDataId()).isEqualTo(deviceDataId);
        });
    }

    @Test
    void testE2EWithNotSatisfiedRule() {
        // Create device data
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 20.0));
        deviceData = deviceDataRepository.save(deviceData);

        // Create rule
        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "gt",
                "value", 25.0
        ));
        ruleRepository.save(rule);

        // Send message to RabbitMQ (send as plain string, not JSON)
        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        // Wait for async processing and verify no alert was created
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).isEmpty();
        });
    }

    @Test
    void testE2EWithMultipleRules() {
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

        // Send message to RabbitMQ (send as plain string, not JSON)
        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        // Wait for async processing and verify both alerts were created
        await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(2);
        });
    }

    @Test
    void testE2EWithRepeatRule() {
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

        // Send message to RabbitMQ
        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), latestData.getId());

        // Wait for async processing and verify alert was created
        final String ruleId = rule.getId();
        await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getRuleId()).isEqualTo(ruleId);
        });
    }

    @Test
    void testE2EWithNoRules() {
        // Create device data
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        deviceData = deviceDataRepository.save(deviceData);

        // Send message to RabbitMQ (no rules exist)
        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        // Wait for async processing and verify no alert was created
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).isEmpty();
        });
    }

    @Test
    void testE2EWithLessThanRule() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 20.0));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "temperature", Map.of("type", "lt", "value", 25.0));
        rule = ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        final String ruleId = rule.getId();
        await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getRuleId()).isEqualTo(ruleId);
        });
    }

    @Test
    void testE2EWithGreaterThanOrEqualRule() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 25.0));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "temperature", Map.of("type", "gte", "value", 25.0));
        rule = ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        final String ruleId = rule.getId();
        await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getRuleId()).isEqualTo(ruleId);
        });
    }

    @Test
    void testE2EWithLessThanOrEqualRule() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 25.0));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "temperature", Map.of("type", "lte", "value", 25.0));
        rule = ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        final String ruleId = rule.getId();
        await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getRuleId()).isEqualTo(ruleId);
        });
    }

    @Test
    void testE2EWithEqualRule() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 25.0));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "temperature", Map.of("type", "eq", "value", 25.0));
        rule = ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        final String ruleId = rule.getId();
        await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getRuleId()).isEqualTo(ruleId);
        });
    }

    @Test
    void testE2EWithStringComparison() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("status", "active"));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "status", Map.of("type", "eq", "value", "active"));
        rule = ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        final String ruleId = rule.getId();
        await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getRuleId()).isEqualTo(ruleId);
        });
    }

    @Test
    void testE2EWithIntegerType() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("count", 100));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "count", Map.of("type", "gt", "value", 50));
        rule = ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        final String ruleId = rule.getId();
        await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getRuleId()).isEqualTo(ruleId);
        });
    }

    @Test
    void testE2EWithCrossTypeNumericComparison() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 30));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "temperature", Map.of("type", "gt", "value", 25.0));
        rule = ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        final String ruleId = rule.getId();
        await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getRuleId()).isEqualTo(ruleId);
        });
    }

    @Test
    void testE2EWithNoMetrics() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of());
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "temperature", Map.of("type", "gt", "value", 25.0));
        ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).isEmpty();
        });
    }

    @Test
    void testE2EWithMissingMetricInData() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("humidity", 50.0));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "temperature", Map.of("type", "gt", "value", 25.0));
        ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).isEmpty();
        });
    }

    @Test
    void testE2EWithRepeatRuleNotEnoughPackages() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "repeat",
                "times", 3,
                "value", Map.of("type", "gt", "value", 25.0)
        ));
        ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).isEmpty();
        });
    }

    @Test
    void testE2EWithStringGreaterThan() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("status", "zebra"));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "status", Map.of("type", "gt", "value", "apple"));
        rule = ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        final String ruleId = rule.getId();
        await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getRuleId()).isEqualTo(ruleId);
        });
    }

    @Test
    void testE2EWithStringLessThan() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("status", "apple"));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "status", Map.of("type", "lt", "value", "zebra"));
        rule = ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        final String ruleId = rule.getId();
        await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getRuleId()).isEqualTo(ruleId);
        });
    }

    @Test
    void testE2EWithLongType() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("count", 1000L));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "count", Map.of("type", "gt", "value", 500L));
        rule = ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        final String ruleId = rule.getId();
        await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getRuleId()).isEqualTo(ruleId);
        });
    }

    @Test
    void testE2EWithStringNumericValue() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", "30"));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "temperature", Map.of("type", "gt", "value", 25.0));
        rule = ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        final String ruleId = rule.getId();
        await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getRuleId()).isEqualTo(ruleId);
        });
    }

    @Test
    void testE2EWithRepeatRuleStringTimes() {
        for (long seq = 1; seq <= 3; seq++) {
            DeviceDataDocument deviceData = createDeviceData("device-1", seq, Map.of("temperature", 30.0));
            deviceDataRepository.save(deviceData);
        }

        DeviceDataDocument latestData = deviceDataRepository.findByDeviceIdOrderBySeqDesc("device-1",
                org.springframework.data.domain.PageRequest.of(0, 1)).get(0);

        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "repeat",
                "times", "3",
                "value", Map.of("type", "gt", "value", 25.0)
        ));
        rule = ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), latestData.getId());

        final String ruleId = rule.getId();
        await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getRuleId()).isEqualTo(ruleId);
        });
    }

    @Test
    void testE2EWithEqualInteger() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("count", 100));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "count", Map.of("type", "eq", "value", 100));
        rule = ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        final String ruleId = rule.getId();
        await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getRuleId()).isEqualTo(ruleId);
        });
    }

    @Test
    void testE2EWithEqualLong() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("count", 1000L));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "count", Map.of("type", "eq", "value", 1000L));
        rule = ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        final String ruleId = rule.getId();
        await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getRuleId()).isEqualTo(ruleId);
        });
    }

    @Test
    void testE2EWithInvalidDeviceDataId() {
        // Send message with non-existent device data ID to trigger error handling
        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), "non-existent-id");

        // Wait to ensure message is processed (error should be logged but not thrown)
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).isEmpty();
        });
    }

    @Test
    void testE2EWithNullRuleContent() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = new Rule();
        rule.setDeviceId("device-1");
        rule.setMetricName("temperature");
        rule.setRuleContent(null);
        ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).isEmpty();
        });
    }

    @Test
    void testE2EWithInvalidRuleType() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "temperature", Map.of("type", "invalid", "value", 25.0));
        ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).isEmpty();
        });
    }

    @Test
    void testE2EWithMissingValueInRule() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "temperature", Map.of("type", "gt"));
        ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).isEmpty();
        });
    }

    @Test
    void testE2EWithNonNumericStringInNumericComparison() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", "not-a-number"));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "temperature", Map.of("type", "gt", "value", 25.0));
        ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).isEmpty();
        });
    }

    @Test
    void testE2EWithRepeatRuleMissingValue() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "temperature", Map.of("type", "repeat", "times", 3));
        ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).isEmpty();
        });
    }

    @Test
    void testE2EWithRepeatRuleInvalidTimes() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "repeat",
                "times", -1,
                "value", Map.of("type", "gt", "value", 25.0)
        ));
        ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).isEmpty();
        });
    }

    @Test
    void testE2EWithRepeatRuleMissingMetricInPackage() {
        for (long seq = 1; seq <= 3; seq++) {
            Map<String, Object> metrics = seq == 2 ? Map.of("humidity", 50.0) : Map.of("temperature", 30.0);
            DeviceDataDocument deviceData = createDeviceData("device-1", seq, metrics);
            deviceDataRepository.save(deviceData);
        }

        DeviceDataDocument latestData = deviceDataRepository.findByDeviceIdOrderBySeqDesc("device-1",
                org.springframework.data.domain.PageRequest.of(0, 1)).get(0);

        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "repeat",
                "times", 3,
                "value", Map.of("type", "gt", "value", 25.0)
        ));
        ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), latestData.getId());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).isEmpty();
        });
    }

    @Test
    void testE2EWithRepeatRulePackageNotSatisfyingCondition() {
        for (long seq = 1; seq <= 3; seq++) {
            double temp = seq == 2 ? 20.0 : 30.0; // Second package doesn't satisfy condition
            DeviceDataDocument deviceData = createDeviceData("device-1", seq, Map.of("temperature", temp));
            deviceDataRepository.save(deviceData);
        }

        DeviceDataDocument latestData = deviceDataRepository.findByDeviceIdOrderBySeqDesc("device-1",
                org.springframework.data.domain.PageRequest.of(0, 1)).get(0);

        Rule rule = createRule("device-1", "temperature", Map.of(
                "type", "repeat",
                "times", 3,
                "value", Map.of("type", "gt", "value", 25.0)
        ));
        ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), latestData.getId());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).isEmpty();
        });
    }

    @Test
    void testE2EWithDifferentDeviceId() {
        DeviceDataDocument deviceData = createDeviceData("device-1", 1L, Map.of("temperature", 30.0));
        deviceData = deviceDataRepository.save(deviceData);

        Rule rule = createRule("device-2", "temperature", Map.of("type", "gt", "value", 25.0));
        ruleRepository.save(rule);

        rabbitTemplate.convertAndSend("", rabbitQueueProperties.getDeviceData(), deviceData.getId());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Alert> alerts = alertRepository.findAll();
            assertThat(alerts).isEmpty();
        });
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

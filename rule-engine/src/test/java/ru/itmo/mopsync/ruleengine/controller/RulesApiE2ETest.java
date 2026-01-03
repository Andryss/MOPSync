package ru.itmo.mopsync.ruleengine.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.mopsync.ruleengine.generated.model.RuleRequest;
import ru.itmo.mopsync.ruleengine.model.Rule;
import ru.itmo.mopsync.ruleengine.repository.RuleRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for Rules API endpoints.
 */
@AutoConfigureMockMvc
class RulesApiE2ETest extends BaseDbTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RuleRepository ruleRepository;

    @BeforeEach
    void setUp() {
        ruleRepository.deleteAll();
    }

    @Test
    void testCreateRuleSuccess() throws Exception {
        RuleRequest request = new RuleRequest()
                .deviceId("device-123")
                .metricName("temperature")
                .ruleContent(Map.of("type", "gt", "value", 25.0));

        String expectedJson = """
                {
                  "deviceId": "device-123",
                  "metricName": "temperature",
                  "ruleContent": {
                    "type": "gt",
                    "value": 25.0
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.id").isString());

        List<Rule> saved = ruleRepository.findAll();
        assertThat(saved).hasSize(1);
        Rule rule = saved.get(0);
        assertThat(rule.getDeviceId()).isEqualTo("device-123");
        assertThat(rule.getMetricName()).isEqualTo("temperature");
        assertThat(rule.getRuleContent()).containsEntry("type", "gt");
        assertThat(rule.getRuleContent()).containsEntry("value", 25.0);
    }

    @Test
    void testCreateRuleWithComplexRuleContent() throws Exception {
        Map<String, Object> ruleContent = new HashMap<>();
        ruleContent.put("type", "repeat");
        ruleContent.put("times", 3);
        ruleContent.put("value", Map.of("type", "gt", "value", 25.0));

        RuleRequest request = new RuleRequest()
                .deviceId("device-456")
                .metricName("humidity")
                .ruleContent(ruleContent);

        String expectedJson = """
                {
                  "deviceId": "device-456",
                  "metricName": "humidity",
                  "ruleContent": {
                    "type": "repeat",
                    "times": 3,
                    "value": {
                      "type": "gt",
                      "value": 25.0
                    }
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void testCreateRuleMissingDeviceId() throws Exception {
        RuleRequest request = new RuleRequest()
                .metricName("temperature")
                .ruleContent(Map.of("type", "gt", "value", 25.0));

        String expectedJson = """
                {
                  "code": 400
                }
                """;

        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT));
    }

    @Test
    void testCreateRuleEmptyDeviceId() throws Exception {
        RuleRequest request = new RuleRequest()
                .deviceId("")
                .metricName("temperature")
                .ruleContent(Map.of("type", "gt", "value", 25.0));

        String expectedJson = """
                {
                  "code": 400
                }
                """;

        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT));
    }

    @Test
    void testCreateRuleMissingMetricName() throws Exception {
        RuleRequest request = new RuleRequest()
                .deviceId("device-123")
                .ruleContent(Map.of("type", "gt", "value", 25.0));

        String expectedJson = """
                {
                  "code": 400
                }
                """;

        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT));
    }

    @Test
    void testCreateRuleEmptyMetricName() throws Exception {
        RuleRequest request = new RuleRequest()
                .deviceId("device-123")
                .metricName("")
                .ruleContent(Map.of("type", "gt", "value", 25.0));

        String expectedJson = """
                {
                  "code": 400
                }
                """;

        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT));
    }

    @Test
    void testCreateRuleWithNullRuleContent() throws Exception {
        String requestJson = """
                {
                  "deviceId": "device-123",
                  "metricName": "temperature",
                  "ruleContent": null
                }
                """;

        String expectedJson = """
                {
                  "code": 400
                }
                """;

        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT));
    }

    @Test
    void testCreateRuleInvalidJson() throws Exception {
        String content = "{ invalid json }";
        String expectedJson = """
                {
                  "code": 400,
                  "message": "invalid.json",
                  "humanMessage": "Invalid JSON format"
                }
                """;

        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT));
    }

    @Test
    void testGetRuleSuccess() throws Exception {
        Rule rule = new Rule();
        rule.setDeviceId("device-123");
        rule.setMetricName("temperature");
        rule.setRuleContent(Map.of("type", "gt", "value", 25.0));
        Rule saved = ruleRepository.save(rule);

        String expectedJson = """
                {
                  "id": "%s",
                  "deviceId": "device-123",
                  "metricName": "temperature",
                  "ruleContent": {
                    "type": "gt",
                    "value": 25.0
                  }
                }
                """.formatted(saved.getId());

        mockMvc.perform(get("/api/v1/rules/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson));
    }

    @Test
    void testGetRuleNotFound() throws Exception {
        String expectedJson = """
                {
                  "code": 400,
                  "message": "rule.not.found",
                  "humanMessage": "Rule not found: non-existent-id"
                }
                """;

        mockMvc.perform(get("/api/v1/rules/non-existent-id"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(expectedJson));
    }

    @Test
    void testListRulesEmpty() throws Exception {
        String expectedJson = "[]";

        mockMvc.perform(get("/api/v1/rules"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson));
    }

    @Test
    void testListRulesAll() throws Exception {
        Rule rule1 = new Rule();
        rule1.setDeviceId("device-1");
        rule1.setMetricName("temperature");
        rule1.setRuleContent(Map.of("type", "gt", "value", 25.0));
        ruleRepository.save(rule1);

        Rule rule2 = new Rule();
        rule2.setDeviceId("device-2");
        rule2.setMetricName("humidity");
        rule2.setRuleContent(Map.of("type", "lt", "value", 60.0));
        ruleRepository.save(rule2);

        mockMvc.perform(get("/api/v1/rules"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testListRulesFilterByDeviceId() throws Exception {
        Rule rule1 = new Rule();
        rule1.setDeviceId("device-1");
        rule1.setMetricName("temperature");
        rule1.setRuleContent(Map.of("type", "gt", "value", 25.0));
        ruleRepository.save(rule1);

        Rule rule2 = new Rule();
        rule2.setDeviceId("device-2");
        rule2.setMetricName("temperature");
        rule2.setRuleContent(Map.of("type", "gt", "value", 30.0));
        ruleRepository.save(rule2);

        mockMvc.perform(get("/api/v1/rules")
                        .param("deviceId", "device-1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].deviceId").value("device-1"));
    }

    @Test
    void testListRulesFilterByMetricName() throws Exception {
        Rule rule1 = new Rule();
        rule1.setDeviceId("device-1");
        rule1.setMetricName("temperature");
        rule1.setRuleContent(Map.of("type", "gt", "value", 25.0));
        ruleRepository.save(rule1);

        Rule rule2 = new Rule();
        rule2.setDeviceId("device-1");
        rule2.setMetricName("humidity");
        rule2.setRuleContent(Map.of("type", "lt", "value", 60.0));
        ruleRepository.save(rule2);

        mockMvc.perform(get("/api/v1/rules")
                        .param("metricName", "temperature"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].metricName").value("temperature"));
    }

    @Test
    void testListRulesFilterByDeviceIdAndMetricName() throws Exception {
        Rule rule1 = new Rule();
        rule1.setDeviceId("device-1");
        rule1.setMetricName("temperature");
        rule1.setRuleContent(Map.of("type", "gt", "value", 25.0));
        ruleRepository.save(rule1);

        Rule rule2 = new Rule();
        rule2.setDeviceId("device-1");
        rule2.setMetricName("humidity");
        rule2.setRuleContent(Map.of("type", "lt", "value", 60.0));
        ruleRepository.save(rule2);

        Rule rule3 = new Rule();
        rule3.setDeviceId("device-2");
        rule3.setMetricName("temperature");
        rule3.setRuleContent(Map.of("type", "gt", "value", 30.0));
        ruleRepository.save(rule3);

        mockMvc.perform(get("/api/v1/rules")
                        .param("deviceId", "device-1")
                        .param("metricName", "temperature"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].deviceId").value("device-1"))
                .andExpect(jsonPath("$[0].metricName").value("temperature"));
    }

    @Test
    void testUpdateRuleSuccess() throws Exception {
        Rule rule = new Rule();
        rule.setDeviceId("device-123");
        rule.setMetricName("temperature");
        rule.setRuleContent(Map.of("type", "gt", "value", 25.0));
        Rule saved = ruleRepository.save(rule);

        RuleRequest updateRequest = new RuleRequest()
                .deviceId("device-456")
                .metricName("humidity")
                .ruleContent(Map.of("type", "lt", "value", 60.0));

        String expectedJson = """
                {
                  "id": "%s",
                  "deviceId": "device-456",
                  "metricName": "humidity",
                  "ruleContent": {
                    "type": "lt",
                    "value": 60.0
                  }
                }
                """.formatted(saved.getId());

        mockMvc.perform(put("/api/v1/rules/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));

        Rule updated = ruleRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getDeviceId()).isEqualTo("device-456");
        assertThat(updated.getMetricName()).isEqualTo("humidity");
        assertThat(updated.getRuleContent()).containsEntry("type", "lt");
        assertThat(updated.getRuleContent()).containsEntry("value", 60.0);
    }

    @Test
    void testUpdateRuleNotFound() throws Exception {
        RuleRequest updateRequest = new RuleRequest()
                .deviceId("device-456")
                .metricName("humidity")
                .ruleContent(Map.of("type", "lt", "value", 60.0));

        String expectedJson = """
                {
                  "code": 400,
                  "message": "rule.not.found",
                  "humanMessage": "Rule not found: non-existent-id"
                }
                """;

        mockMvc.perform(put("/api/v1/rules/non-existent-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(expectedJson));
    }

    @Test
    void testUpdateRuleInvalidRequest() throws Exception {
        Rule rule = new Rule();
        rule.setDeviceId("device-123");
        rule.setMetricName("temperature");
        rule.setRuleContent(Map.of("type", "gt", "value", 25.0));
        Rule saved = ruleRepository.save(rule);

        RuleRequest updateRequest = new RuleRequest()
                .deviceId("")
                .metricName("humidity")
                .ruleContent(Map.of("type", "lt", "value", 60.0));

        String expectedJson = """
                {
                  "code": 400
                }
                """;

        mockMvc.perform(put("/api/v1/rules/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT));
    }

    @Test
    void testDeleteRuleSuccess() throws Exception {
        Rule rule = new Rule();
        rule.setDeviceId("device-123");
        rule.setMetricName("temperature");
        rule.setRuleContent(Map.of("type", "gt", "value", 25.0));
        Rule saved = ruleRepository.save(rule);

        mockMvc.perform(delete("/api/v1/rules/{id}", saved.getId()))
                .andExpect(status().isOk());

        assertThat(ruleRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void testDeleteRuleNotFound() throws Exception {
        String expectedJson = """
                {
                  "code": 400,
                  "message": "rule.not.found",
                  "humanMessage": "Rule not found: non-existent-id"
                }
                """;

        mockMvc.perform(delete("/api/v1/rules/non-existent-id"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(expectedJson));
    }

    @Test
    void testCreateRuleInvalidContentType() throws Exception {
        RuleRequest request = new RuleRequest()
                .deviceId("device-123")
                .metricName("temperature")
                .ruleContent(Map.of("type", "gt", "value", 25.0));

        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testUpdateRuleInvalidContentType() throws Exception {
        Rule rule = new Rule();
        rule.setDeviceId("device-123");
        rule.setMetricName("temperature");
        rule.setRuleContent(Map.of("type", "gt", "value", 25.0));
        Rule saved = ruleRepository.save(rule);

        RuleRequest updateRequest = new RuleRequest()
                .deviceId("device-456")
                .metricName("humidity")
                .ruleContent(Map.of("type", "lt", "value", 60.0));

        mockMvc.perform(put("/api/v1/rules/{id}", saved.getId())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testCreateRuleMissingRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateRuleMissingRequestBody() throws Exception {
        Rule rule = new Rule();
        rule.setDeviceId("device-123");
        rule.setMetricName("temperature");
        rule.setRuleContent(Map.of("type", "gt", "value", 25.0));
        Rule saved = ruleRepository.save(rule);

        mockMvc.perform(put("/api/v1/rules/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}


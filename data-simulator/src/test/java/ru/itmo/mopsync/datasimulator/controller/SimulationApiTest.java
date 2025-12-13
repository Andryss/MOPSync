package ru.itmo.mopsync.datasimulator.controller;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.mopsync.datasimulator.generated.iotcontroller.api.DeviceDataApi;
import ru.itmo.mopsync.datasimulator.generated.iotcontroller.model.DeviceDataRequest;
import ru.itmo.mopsync.datasimulator.generated.model.DeviceGroup;
import ru.itmo.mopsync.datasimulator.generated.model.MetricDefinition;
import ru.itmo.mopsync.datasimulator.generated.model.SimulationSpecRequest;
import ru.itmo.mopsync.datasimulator.service.SimulationService;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SimulationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SimulationService simulationService;

    @MockitoBean
    private DeviceDataApi deviceDataApi;

    @BeforeEach
    void setUp() {
        Mockito.reset(deviceDataApi);
        // Stop any running simulation before each test
        simulationService.stopSimulation();
    }

    @Test
    void testSetSimulationSpecSuccess() throws Exception {
        SimulationSpecRequest request = new SimulationSpecRequest()
                .addGroupsItem(new DeviceGroup()
                        .count(2)
                        .frequency(10)
                        .metricsTemplate("teapot")
                );

        String expectedJson = """
                {
                  "groups": [
                    {
                      "frequency": 10,
                      "metrics": {
                        "temperature": {
                          "type": "number",
                          "min": 20.0,
                          "max": 100.0
                        },
                        "waterLevel": {
                          "type": "number",
                          "min": 0.0,
                          "max": 1.0
                        }
                      }
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT))
                .andExpect(jsonPath("$.groups[0].deviceIds").isArray())
                .andExpect(jsonPath("$.groups[0].deviceIds.length()").value(2));
    }

    @Test
    void testSetSimulationSpecWithManualMetrics() throws Exception {
        Map<String, MetricDefinition> metrics = Map.of(
                "temperature", new MetricDefinition().type(MetricDefinition.TypeEnum.NUMBER).min(20.0).max(100.0),
                "humidity", new MetricDefinition().type(MetricDefinition.TypeEnum.INTEGER).min(0.0).max(100.0)
        );

        SimulationSpecRequest request = new SimulationSpecRequest()
                .addGroupsItem(new DeviceGroup()
                        .count(1)
                        .frequency(5)
                        .metrics(metrics)
                );

        String expectedJson = """
                {
                  "groups": [
                    {
                      "frequency": 5,
                      "metrics": {
                        "temperature": {
                          "type": "number",
                          "min": 20.0,
                          "max": 100.0
                        },
                        "humidity": {
                          "type": "integer",
                          "min": 0.0,
                          "max": 100.0
                        }
                      }
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT))
                .andExpect(jsonPath("$.groups[0].deviceIds").isArray())
                .andExpect(jsonPath("$.groups[0].deviceIds.length()").value(1));
    }

    @Test
    void testSetSimulationSpecWithMultipleGroups() throws Exception {
        SimulationSpecRequest request = new SimulationSpecRequest()
                .addGroupsItem(new DeviceGroup()
                        .count(2)
                        .frequency(10)
                        .metricsTemplate("teapot")
                )
                .addGroupsItem(new DeviceGroup()
                        .count(3)
                        .frequency(20)
                        .metricsTemplate("lamp")
                );

        String expectedJson = """
                {
                  "groups": [
                    {
                      "frequency": 10,
                      "metrics": {
                        "temperature": {},
                        "waterLevel": {}
                      }
                    },
                    {
                      "frequency": 20,
                      "metrics": {
                        "brightness": {},
                        "color": {}
                      }
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT))
                .andExpect(jsonPath("$.groups[0].deviceIds").isArray())
                .andExpect(jsonPath("$.groups[0].deviceIds.length()").value(2))
                .andExpect(jsonPath("$.groups[1].deviceIds").isArray())
                .andExpect(jsonPath("$.groups[1].deviceIds.length()").value(3));
    }

    @Test
    void testSetSimulationSpecThrowsException() throws Exception {
        SimulationSpecRequest request = new SimulationSpecRequest()
                .addGroupsItem(new DeviceGroup()
                        .count(1)
                        .frequency(10)
                );

        String expectedJson = """
                {
                  "code": 400,
                  "message": "metrics.required.error",
                  "humanMessage": "Either metrics-template or metrics must be provided for device group"
                }
                """;

        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson));
    }

    @Test
    void testSetSimulationSpecInvalidJson() throws Exception {
        String content = "{ invalid json }";
        String expectedJson = """
                {
                  "code": 400,
                  "message": "invalid.json.error",
                  "humanMessage": "Invalid JSON in request body"
                }
                """;

        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson));
    }

    @Test
    void testStartSimulationSuccess() throws Exception {
        // First set up devices
        SimulationSpecRequest request = new SimulationSpecRequest()
                .addGroupsItem(new DeviceGroup()
                        .count(1)
                        .frequency(10)
                        .metricsTemplate("teapot")
                );

        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then start simulation
        mockMvc.perform(post("/api/v1/simulation/start"))
                .andExpect(status().isOk());

        // Verify that DeviceDataApi receives data (with timeout since tasks are scheduled)
        ArgumentCaptor<DeviceDataRequest> captor = ArgumentCaptor.forClass(DeviceDataRequest.class);
        verify(deviceDataApi, timeout(5000).atLeastOnce()).receiveDeviceData(captor.capture());
        
        DeviceDataRequest capturedRequest = captor.getValue();
        Assertions.assertThat(capturedRequest.getDeviceId()).isNotNull();
        Assertions.assertThat(capturedRequest.getMetrics()).containsKeys("temperature", "waterLevel");
    }

    @Test
    void testStartSimulationThrowsExceptionWhenAlreadyRunning() throws Exception {
        // First set up devices
        SimulationSpecRequest request = new SimulationSpecRequest()
                .addGroupsItem(new DeviceGroup()
                        .count(1)
                        .frequency(10)
                        .metricsTemplate("teapot")
                );

        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Start simulation first time
        mockMvc.perform(post("/api/v1/simulation/start"))
                .andExpect(status().isOk());

        // Try to start again - should fail
        String expectedJson = """
                {
                  "code": 400,
                  "message": "simulation.already.running.error",
                  "humanMessage": "Simulation is already running"
                }
                """;

        mockMvc.perform(post("/api/v1/simulation/start"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson));
    }

    @Test
    void testStartSimulationThrowsExceptionWhenNoDevices() throws Exception {
        String expectedJson = """
                {
                  "code": 400,
                  "message": "no.devices.configured.error",
                  "humanMessage": "No devices configured. Set simulation spec first."
                }
                """;

        mockMvc.perform(post("/api/v1/simulation/start"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson));
    }

    @Test
    void testStopSimulationSuccess() throws Exception {
        // First set up devices and start simulation
        SimulationSpecRequest request = new SimulationSpecRequest()
                .addGroupsItem(new DeviceGroup()
                        .count(1)
                        .frequency(10)
                        .metricsTemplate("teapot")
                );

        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/simulation/start"))
                .andExpect(status().isOk());

        // Then stop simulation
        mockMvc.perform(post("/api/v1/simulation/stop"))
                .andExpect(status().isOk());
    }

    @Test
    void testStopSimulationWhenNotRunning() throws Exception {
        // Stop when not running should succeed (idempotent)
        mockMvc.perform(post("/api/v1/simulation/stop"))
                .andExpect(status().isOk());
    }

    @Test
    void testSetSimulationSpecWithUnknownTemplate() throws Exception {
        SimulationSpecRequest request = new SimulationSpecRequest()
                .addGroupsItem(new DeviceGroup()
                        .count(1)
                        .frequency(10)
                        .metricsTemplate("unknown-template")
                );

        String expectedJson = """
                {
                  "code": 400,
                  "message": "unknown.metrics.template.error",
                  "humanMessage": "Unknown metrics template: unknown-template"
                }
                """;

        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson));
    }

    @Test
    void testSetSimulationSpecWithStringMetrics() throws Exception {
        Map<String, MetricDefinition> metrics = Map.of(
                "status", new MetricDefinition()
                        .type(MetricDefinition.TypeEnum.STRING)
                        .values(List.of("on", "off", "standby"))
        );

        SimulationSpecRequest request = new SimulationSpecRequest()
                .addGroupsItem(new DeviceGroup()
                        .count(1)
                        .frequency(5)
                        .metrics(metrics)
                );

        String expectedJson = """
                {
                  "groups": [
                    {
                      "frequency": 5,
                      "metrics": {
                        "status": {
                          "type": "string",
                          "values": ["on", "off", "standby"]
                        }
                      }
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT));
    }

    @Test
    void testSetSimulationSpecWithLampTemplate() throws Exception {
        SimulationSpecRequest request = new SimulationSpecRequest()
                .addGroupsItem(new DeviceGroup()
                        .count(1)
                        .frequency(15)
                        .metricsTemplate("lamp")
                );

        String expectedJson = """
                {
                  "groups": [
                    {
                      "frequency": 15,
                      "metrics": {
                        "brightness": {},
                        "color": {
                          "type": "string",
                          "values": ["white", "warm", "cold", "red", "green", "blue"]
                        }
                      }
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT))
                .andExpect(jsonPath("$.groups[0].deviceIds").isArray())
                .andExpect(jsonPath("$.groups[0].deviceIds.length()").value(1));
    }

    @Test
    void testSetSimulationSpecWithEmptyGroups() throws Exception {
        SimulationSpecRequest request = new SimulationSpecRequest();

        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "code": 400
                        }
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    void testSetSimulationSpecWithBothMetricsAndTemplate() throws Exception {
        // When both metrics and metricsTemplate are provided, template should be used
        Map<String, MetricDefinition> metrics = Map.of(
                "custom", new MetricDefinition().type(MetricDefinition.TypeEnum.NUMBER).min(0.0).max(10.0)
        );

        SimulationSpecRequest request = new SimulationSpecRequest()
                .addGroupsItem(new DeviceGroup()
                        .count(1)
                        .frequency(10)
                        .metricsTemplate("teapot")
                        .metrics(metrics)
                );

        String expectedJson = """
                {
                  "groups": [
                    {
                      "frequency": 10,
                      "metrics": {
                        "temperature": {},
                        "waterLevel": {}
                      }
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT))
                .andExpect(jsonPath("$.groups[0].metrics.custom").doesNotExist());
    }

    @Test
    void testStartStopStartSimulation() throws Exception {
        // Set up devices
        SimulationSpecRequest request = new SimulationSpecRequest()
                .addGroupsItem(new DeviceGroup()
                        .count(1)
                        .frequency(10)
                        .metricsTemplate("teapot")
                );

        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Start simulation
        mockMvc.perform(post("/api/v1/simulation/start"))
                .andExpect(status().isOk());

        // Stop simulation (clears devices)
        mockMvc.perform(post("/api/v1/simulation/stop"))
                .andExpect(status().isOk());

        // Set up devices again (since stopSimulation clears them)
        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Start again - should succeed
        mockMvc.perform(post("/api/v1/simulation/start"))
                .andExpect(status().isOk());
    }

    @Test
    void testSetSimulationSpecReplacesPreviousSpec() throws Exception {
        // Set first spec
        SimulationSpecRequest firstRequest = new SimulationSpecRequest()
                .addGroupsItem(new DeviceGroup()
                        .count(2)
                        .frequency(10)
                        .metricsTemplate("teapot")
                );

        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[0].deviceIds").isArray())
                .andExpect(jsonPath("$.groups[0].deviceIds.length()").value(2));

        // Set second spec - should replace the first one
        SimulationSpecRequest secondRequest = new SimulationSpecRequest()
                .addGroupsItem(new DeviceGroup()
                        .count(3)
                        .frequency(20)
                        .metricsTemplate("lamp")
                );

        String expectedJson = """
                {
                  "groups": [
                    {
                      "frequency": 20,
                      "metrics": {
                        "brightness": {},
                        "color": {}
                      }
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT))
                .andExpect(jsonPath("$.groups[0].deviceIds").isArray())
                .andExpect(jsonPath("$.groups[0].deviceIds.length()").value(3));
    }

    @Test
    void testSetSimulationSpecInvalidContentType() throws Exception {
        SimulationSpecRequest request = new SimulationSpecRequest()
                .addGroupsItem(new DeviceGroup()
                        .count(1)
                        .frequency(10)
                        .metricsTemplate("teapot")
                );

        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testSetSimulationSpecMissingRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/simulation/spec")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}

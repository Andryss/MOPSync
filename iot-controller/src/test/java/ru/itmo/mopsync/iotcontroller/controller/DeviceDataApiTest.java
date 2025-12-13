package ru.itmo.mopsync.iotcontroller.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.mopsync.iotcontroller.model.DeviceDataDocument;
import ru.itmo.mopsync.iotcontroller.repository.DeviceDataRepository;
import ru.itmo.mopsync.iotcontroller.service.RabbitMqMessageSender;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeviceDataApiTest extends BaseDbTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceDataRepository deviceDataRepository;

    @MockitoBean
    private RabbitMqMessageSender rabbitMqMessageSender;

    @BeforeEach
    void setUp() {
        deviceDataRepository.deleteAll();
        Mockito.reset(rabbitMqMessageSender);
    }

    @Test
    void testReceiveDeviceDataSuccess() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now();
        String requestJson = String.format("""
                {
                  "device_id": "device-123",
                  "timestamp": "%s",
                  "seq": 100,
                  "metrics": {
                    "temperature": 25.5,
                    "humidity": 60,
                    "status": "active"
                  },
                  "meta": {
                    "firmware": "1.0.3"
                  }
                }
                """, timestamp);

        mockMvc.perform(post("/api/v1/device-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        List<DeviceDataDocument> saved = deviceDataRepository.findAll();
        assertThat(saved).hasSize(1);
        DeviceDataDocument document = saved.get(0);
        assertThat(document.getDeviceId()).isEqualTo("device-123");
        assertThat(document.getTimestamp()).isEqualTo(timestamp);
        assertThat(document.getSeq()).isEqualTo(100L);
        assertThat(document.getMetrics()).containsEntry("temperature", 25.5);
        assertThat(document.getMetrics()).containsEntry("humidity", 60);
        assertThat(document.getMetrics()).containsEntry("status", "active");
        assertThat(document.getMeta()).containsEntry("firmware", "1.0.3");
        assertThat(document.getId()).isNotNull();

        verify(rabbitMqMessageSender).sendDeviceDataNotification(document.getId());
    }

    @Test
    void testReceiveDeviceDataWithNumberMetrics() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now();
        String requestJson = String.format("""
                {
                  "device_id": "device-456",
                  "timestamp": "%s",
                  "seq": 200,
                  "metrics": {
                    "temperature": 30.7,
                    "pressure": 1013.25
                  }
                }
                """, timestamp);

        mockMvc.perform(post("/api/v1/device-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        List<DeviceDataDocument> saved = deviceDataRepository.findAll();
        assertThat(saved).hasSize(1);
        DeviceDataDocument document = saved.get(0);
        assertThat(document.getDeviceId()).isEqualTo("device-456");
        assertThat(document.getTimestamp()).isEqualTo(timestamp);
        assertThat(document.getSeq()).isEqualTo(200L);
        assertThat(document.getMetrics()).containsEntry("temperature", 30.7);
        assertThat(document.getMetrics()).containsEntry("pressure", 1013.25);
        assertThat(document.getMeta()).isNull();

        verify(rabbitMqMessageSender).sendDeviceDataNotification(document.getId());
    }

    @Test
    void testReceiveDeviceDataWithStringMetrics() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now();
        String requestJson = String.format("""
                {
                  "device_id": "device-789",
                  "timestamp": "%s",
                  "seq": 300,
                  "metrics": {
                    "status": "online",
                    "mode": "auto"
                  }
                }
                """, timestamp);

        mockMvc.perform(post("/api/v1/device-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        List<DeviceDataDocument> saved = deviceDataRepository.findAll();
        assertThat(saved).hasSize(1);
        DeviceDataDocument document = saved.get(0);
        assertThat(document.getDeviceId()).isEqualTo("device-789");
        assertThat(document.getTimestamp()).isEqualTo(timestamp);
        assertThat(document.getSeq()).isEqualTo(300L);
        assertThat(document.getMetrics()).containsEntry("status", "online");
        assertThat(document.getMetrics()).containsEntry("mode", "auto");

        verify(rabbitMqMessageSender).sendDeviceDataNotification(document.getId());
    }

    @Test
    void testReceiveDeviceDataMissingDeviceId() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now();
        String requestJson = String.format("""
                {
                  "timestamp": "%s",
                  "seq": 100,
                  "metrics": {
                    "temperature": 25.5
                  }
                }
                """, timestamp);

        String expectedJson = """
                {
                  "code": 400
                }
                """;

        mockMvc.perform(post("/api/v1/device-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT));
    }

    @Test
    void testReceiveDeviceDataEmptyDeviceId() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now();
        String requestJson = String.format("""
                {
                  "device_id": "",
                  "timestamp": "%s",
                  "seq": 100,
                  "metrics": {
                    "temperature": 25.5
                  }
                }
                """, timestamp);

        String expectedJson = """
                {
                  "code": 400
                }
                """;

        mockMvc.perform(post("/api/v1/device-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT));
    }

    @Test
    void testReceiveDeviceDataMissingTimestamp() throws Exception {
        String requestJson = """
                {
                  "device_id": "device-123",
                  "seq": 100,
                  "metrics": {
                    "temperature": 25.5
                  }
                }
                """;

        String expectedJson = """
                {
                  "code": 400
                }
                """;

        mockMvc.perform(post("/api/v1/device-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT));
    }

    @Test
    void testReceiveDeviceDataMissingSeq() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now();
        String requestJson = String.format("""
                {
                  "device_id": "device-123",
                  "timestamp": "%s",
                  "metrics": {
                    "temperature": 25.5
                  }
                }
                """, timestamp);

        String expectedJson = """
                {
                  "code": 400
                }
                """;

        mockMvc.perform(post("/api/v1/device-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT));
    }

    @Test
    void testReceiveDeviceDataNegativeSeq() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now();
        String requestJson = String.format("""
                {
                  "device_id": "device-123",
                  "timestamp": "%s",
                  "seq": -1,
                  "metrics": {
                    "temperature": 25.5
                  }
                }
                """, timestamp);

        String expectedJson = """
                {
                  "code": 400
                }
                """;

        mockMvc.perform(post("/api/v1/device-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT));
    }

    @Test
    void testReceiveDeviceDataMissingMetrics() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now();
        String requestJson = String.format("""
                {
                  "device_id": "device-123",
                  "timestamp": "%s",
                  "seq": 100
                }
                """, timestamp);

        String expectedJson = """
                {
                  "code": 400
                }
                """;

        mockMvc.perform(post("/api/v1/device-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT));
    }

    @Test
    void testReceiveDeviceDataEmptyMetrics() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now();
        String requestJson = String.format("""
                {
                  "device_id": "device-123",
                  "timestamp": "%s",
                  "seq": 100,
                  "metrics": {}
                }
                """, timestamp);

        String expectedJson = """
                {
                  "code": 400
                }
                """;

        mockMvc.perform(post("/api/v1/device-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT));
    }

    @Test
    void testReceiveDeviceDataWithNullMetricValue() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now();
        String requestJson = String.format("""
                {
                  "device_id": "device-123",
                  "timestamp": "%s",
                  "seq": 100,
                  "metrics": {
                    "temperature": null
                  }
                }
                """, timestamp);

        String expectedJson = """
                {
                  "code": 400,
                  "message": "validation.error"
                }
                """;

        mockMvc.perform(post("/api/v1/device-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT));
    }

    @Test
    void testReceiveDeviceDataWithInvalidMetricType() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now();
        String requestJson = String.format("""
                {
                  "device_id": "device-123",
                  "timestamp": "%s",
                  "seq": 100,
                  "metrics": {
                    "temperature": {
                      "invalid": "nested"
                    }
                  }
                }
                """, timestamp);

        String expectedJson = """
                {
                  "code": 400,
                  "message": "validation.error"
                }
                """;

        mockMvc.perform(post("/api/v1/device-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson, JsonCompareMode.LENIENT));
    }

    @Test
    void testReceiveDeviceDataInvalidJson() throws Exception {
        String content = "{ invalid json }";
        String expectedJson = """
                {
                  "code": 400,
                  "message": "invalid.json.error",
                  "humanMessage": "Invalid JSON in request body"
                }
                """;

        mockMvc.perform(post("/api/v1/device-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson));
    }

    @Test
    void testReceiveDeviceDataInvalidContentType() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now();
        String requestJson = String.format("""
                {
                  "device_id": "device-123",
                  "timestamp": "%s",
                  "seq": 100,
                  "metrics": {
                    "temperature": 25.5
                  }
                }
                """, timestamp);

        mockMvc.perform(post("/api/v1/device-data")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(requestJson))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testReceiveDeviceDataMissingRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/device-data")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testReceiveDeviceDataWithOptionalMeta() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now();
        String requestJson = String.format("""
                {
                  "device_id": "device-123",
                  "timestamp": "%s",
                  "seq": 100,
                  "metrics": {
                    "temperature": 25.5
                  },
                  "meta": {
                    "firmware": "1.0.3",
                    "version": "2.0"
                  }
                }
                """, timestamp);

        mockMvc.perform(post("/api/v1/device-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        List<DeviceDataDocument> saved = deviceDataRepository.findAll();
        assertThat(saved).hasSize(1);
        DeviceDataDocument document = saved.get(0);
        assertThat(document.getDeviceId()).isEqualTo("device-123");
        assertThat(document.getMeta()).containsEntry("firmware", "1.0.3");
        assertThat(document.getMeta()).containsEntry("version", "2.0");

        verify(rabbitMqMessageSender).sendDeviceDataNotification(document.getId());
    }

    @Test
    void testReceiveDeviceDataWithoutMeta() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.now();
        String requestJson = String.format("""
                {
                  "device_id": "device-123",
                  "timestamp": "%s",
                  "seq": 100,
                  "metrics": {
                    "temperature": 25.5
                  }
                }
                """, timestamp);

        mockMvc.perform(post("/api/v1/device-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        List<DeviceDataDocument> saved = deviceDataRepository.findAll();
        assertThat(saved).hasSize(1);
        DeviceDataDocument document = saved.get(0);
        assertThat(document.getDeviceId()).isEqualTo("device-123");
        assertThat(document.getMeta()).isNull();

        verify(rabbitMqMessageSender).sendDeviceDataNotification(document.getId());
    }
}

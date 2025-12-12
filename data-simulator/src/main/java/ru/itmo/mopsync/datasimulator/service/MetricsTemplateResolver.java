package ru.itmo.mopsync.datasimulator.service;

import ru.itmo.mopsync.datasimulator.exception.Errors;
import ru.itmo.mopsync.datasimulator.generated.model.MetricDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

/**
 * Service for resolving metrics templates to actual metrics definitions.
 */
@Service
public class MetricsTemplateResolver {

    private static final Map<String, Map<String, MetricDefinition>> TEMPLATES = Map.of(
            "teapot", Map.of(
                    "temperature", new MetricDefinition()
                            .type(MetricDefinition.TypeEnum.NUMBER)
                            .min(20.0)
                            .max(100.0),
                    "waterLevel", new MetricDefinition()
                            .type(MetricDefinition.TypeEnum.NUMBER)
                            .min(0.0)
                            .max(1.0)
            ),
            "lamp", Map.of(
                    "brightness", new MetricDefinition()
                            .type(MetricDefinition.TypeEnum.NUMBER)
                            .min(0.0)
                            .max(100.0),
                    "color", new MetricDefinition()
                            .type(MetricDefinition.TypeEnum.STRING)
                            .values(List.of("white", "warm", "cold", "red", "green", "blue"))
            )
    );

    /**
     * Resolves a metrics template name to actual metrics definition.
     *
     * @param templateName name of the template
     * @return resolved metrics definition
     * @throws IllegalArgumentException if template not found
     */
    public Map<String, MetricDefinition> resolveTemplate(String templateName) {
        return Optional.ofNullable(TEMPLATES.get(templateName))
                .orElseThrow(() -> Errors.unknownMetricsTemplateError(templateName));
    }
}


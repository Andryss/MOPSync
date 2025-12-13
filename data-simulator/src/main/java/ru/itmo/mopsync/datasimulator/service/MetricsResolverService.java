package ru.itmo.mopsync.datasimulator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.mopsync.datasimulator.exception.Errors;
import ru.itmo.mopsync.datasimulator.generated.model.DeviceGroup;
import ru.itmo.mopsync.datasimulator.generated.model.MetricDefinition;

import java.util.Map;

/**
 * Service for resolving metrics from device groups.
 * Handles both template-based and manual metrics resolution.
 */
@Component
@RequiredArgsConstructor
public class MetricsResolverService {

    private final MetricsTemplateResolver templateResolver;

    /**
     * Resolves metrics for a device group.
     * Template takes precedence over manual metrics if both are provided.
     *
     * @param group device group with metrics configuration
     * @return resolved metrics map
     * @throws BaseException if neither template nor metrics are provided
     */
    public Map<String, MetricDefinition> resolveMetrics(DeviceGroup group) {
        String template = group.getMetricsTemplate();
        if (template != null && !template.isEmpty()) {
            return templateResolver.resolveTemplate(template);
        }

        Map<String, MetricDefinition> manualMetrics = group.getMetrics();
        if (manualMetrics != null && !manualMetrics.isEmpty()) {
            return manualMetrics;
        }

        throw Errors.metricsRequiredError();
    }
}

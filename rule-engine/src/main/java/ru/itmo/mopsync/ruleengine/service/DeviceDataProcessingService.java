package ru.itmo.mopsync.ruleengine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itmo.mopsync.ruleengine.exception.Errors;
import ru.itmo.mopsync.ruleengine.model.DeviceDataDocument;
import ru.itmo.mopsync.ruleengine.model.Rule;
import ru.itmo.mopsync.ruleengine.repository.DeviceDataRepository;
import ru.itmo.mopsync.ruleengine.repository.RuleRepository;

import java.util.List;

/**
 * Service for processing device data and evaluating rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataProcessingService {

    private final DeviceDataRepository deviceDataRepository;
    private final RuleRepository ruleRepository;
    private final RuleEvaluationService ruleEvaluationService;
    private final AlertService alertService;

    /**
     * Processes device data: finds applicable rules and evaluates them.
     *
     * @param deviceDataId MongoDB document ID of the device data
     */
    public void processDeviceData(String deviceDataId) {
        log.debug("Processing device data with id: {}", deviceDataId);

        DeviceDataDocument deviceData = deviceDataRepository.findById(deviceDataId)
                .orElseThrow(() -> {
                    log.warn("Device data not found with id: {}", deviceDataId);
                    return Errors.deviceDataNotFoundError(deviceDataId);
                });

        String deviceId = deviceData.getDeviceId();
        if (deviceData.getMetrics() == null || deviceData.getMetrics().isEmpty()) {
            log.debug("Device data {} has no metrics, skipping rule evaluation", deviceDataId);
            return;
        }

        List<String> metricNames = deviceData.getMetrics().keySet().stream().toList();

        for (String metricName : metricNames) {
            List<Rule> rules = ruleRepository.findByDeviceIdAndMetricName(deviceId, metricName);

            if (rules.isEmpty()) {
                log.debug("No rules found for device {} and metric {}", deviceId, metricName);
                continue;
            }

            for (Rule rule : rules) {
                if (ruleEvaluationService.evaluateRule(rule, deviceData)) {
                    log.info("Rule {} satisfied for device {} and metric {}", rule.getId(), deviceId, metricName);
                    alertService.createAlert(rule, deviceData);
                }
            }
        }

        log.debug("Finished processing device data with id: {}", deviceDataId);
    }
}

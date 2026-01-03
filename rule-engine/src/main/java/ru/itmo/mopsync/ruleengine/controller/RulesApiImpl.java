package ru.itmo.mopsync.ruleengine.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.mopsync.ruleengine.generated.api.RulesApi;
import ru.itmo.mopsync.ruleengine.generated.model.RuleRequest;
import ru.itmo.mopsync.ruleengine.generated.model.RuleResponse;
import ru.itmo.mopsync.ruleengine.service.RuleService;

import java.util.List;

/**
 * Controller for rules endpoints.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class RulesApiImpl implements RulesApi {

    private final RuleService ruleService;

    /**
     * Creates a new rule.
     *
     * @param ruleRequest rule request
     * @return created rule response
     */
    @Override
    public RuleResponse createRule(RuleRequest ruleRequest) {
        log.info("POST /api/v1/rules - Creating rule for device: {}, metric: {}",
                ruleRequest.getDeviceId(), ruleRequest.getMetricName());
        return ruleService.createRule(ruleRequest);
    }

    /**
     * Gets a rule by ID.
     *
     * @param id rule ID
     * @return rule response
     */
    @Override
    public RuleResponse getRule(String id) {
        log.info("GET /api/v1/rules/{} - Getting rule", id);
        return ruleService.getRule(id);
    }

    /**
     * Lists all rules, optionally filtered by device ID and/or metric name.
     *
     * @param deviceId   optional device ID filter
     * @param metricName optional metric name filter
     * @return list of rule responses
     */
    @Override
    public List<RuleResponse> listRules(String deviceId, String metricName) {
        log.info("GET /api/v1/rules - Listing rules with deviceId: {}, metricName: {}", deviceId, metricName);
        return ruleService.listRules(deviceId, metricName);
    }

    /**
     * Updates a rule.
     *
     * @param id          rule ID
     * @param ruleRequest rule request
     * @return updated rule response
     */
    @Override
    public RuleResponse updateRule(String id, RuleRequest ruleRequest) {
        log.info("PUT /api/v1/rules/{} - Updating rule", id);
        return ruleService.updateRule(id, ruleRequest);
    }

    /**
     * Deletes a rule.
     *
     * @param id rule ID
     */
    @Override
    public void deleteRule(String id) {
        log.info("DELETE /api/v1/rules/{} - Deleting rule", id);
        ruleService.deleteRule(id);
    }
}


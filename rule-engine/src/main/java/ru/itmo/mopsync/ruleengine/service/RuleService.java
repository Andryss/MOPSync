package ru.itmo.mopsync.ruleengine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itmo.mopsync.ruleengine.converter.RuleConverter;
import ru.itmo.mopsync.ruleengine.exception.Errors;
import ru.itmo.mopsync.ruleengine.generated.model.RuleRequest;
import ru.itmo.mopsync.ruleengine.generated.model.RuleResponse;
import ru.itmo.mopsync.ruleengine.model.Rule;
import ru.itmo.mopsync.ruleengine.repository.RuleRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleRepository ruleRepository;
    private final RuleConverter ruleConverter;

    /**
     * Creates a new rule.
     *
     * @param ruleRequest rule request
     * @return created rule response
     */
    public RuleResponse createRule(RuleRequest ruleRequest) {
        log.debug("Creating rule for device: {}, metric: {}", ruleRequest.getDeviceId(), ruleRequest.getMetricName());
        Rule rule = new Rule();
        rule.setDeviceId(ruleRequest.getDeviceId());
        rule.setMetricName(ruleRequest.getMetricName());
        rule.setRuleContent(ruleRequest.getRuleContent());

        Rule savedRule = ruleRepository.save(rule);
        log.debug("Rule created with id: {}", savedRule.getId());
        return ruleConverter.toRuleResponse(savedRule);
    }

    /**
     * Gets a rule by ID.
     *
     * @param id rule ID
     * @return rule response
     * @throws ru.itmo.mopsync.ruleengine.exception.BaseException if rule not found
     */
    public RuleResponse getRule(String id) {
        log.debug("Getting rule with id: {}", id);
        Rule rule = ruleRepository.findById(id)
                .orElseThrow(() -> Errors.ruleNotFoundError(id));
        return ruleConverter.toRuleResponse(rule);
    }

    /**
     * Lists all rules, optionally filtered by device ID and/or metric name.
     *
     * @param deviceId   optional device ID filter
     * @param metricName optional metric name filter
     * @return list of rule responses
     */
    public List<RuleResponse> listRules(String deviceId, String metricName) {
        log.debug("Listing rules with deviceId: {}, metricName: {}", deviceId, metricName);
        List<Rule> rules;

        if (deviceId != null && metricName != null) {
            rules = ruleRepository.findByDeviceIdAndMetricName(deviceId, metricName);
        } else if (deviceId != null) {
            rules = ruleRepository.findByDeviceId(deviceId);
        } else if (metricName != null) {
            rules = ruleRepository.findByMetricName(metricName);
        } else {
            rules = ruleRepository.findAll();
        }

        return rules.stream()
                .map(ruleConverter::toRuleResponse)
                .collect(Collectors.toList());
    }

    /**
     * Updates a rule.
     *
     * @param id          rule ID
     * @param ruleRequest rule request
     * @return updated rule response
     * @throws ru.itmo.mopsync.ruleengine.exception.BaseException if rule not found
     */
    public RuleResponse updateRule(String id, RuleRequest ruleRequest) {
        log.debug("Updating rule with id: {}", id);
        Rule rule = ruleRepository.findById(id)
                .orElseThrow(() -> Errors.ruleNotFoundError(id));

        rule.setDeviceId(ruleRequest.getDeviceId());
        rule.setMetricName(ruleRequest.getMetricName());
        rule.setRuleContent(ruleRequest.getRuleContent());

        Rule updatedRule = ruleRepository.save(rule);
        log.debug("Rule updated with id: {}", updatedRule.getId());
        return ruleConverter.toRuleResponse(updatedRule);
    }

    /**
     * Deletes a rule.
     *
     * @param id rule ID
     * @throws ru.itmo.mopsync.ruleengine.exception.BaseException if rule not found
     */
    public void deleteRule(String id) {
        log.debug("Deleting rule with id: {}", id);
        Rule rule = ruleRepository.findById(id)
                .orElseThrow(() -> Errors.ruleNotFoundError(id));
        ruleRepository.delete(rule);
        log.debug("Rule deleted with id: {}", id);
    }
}


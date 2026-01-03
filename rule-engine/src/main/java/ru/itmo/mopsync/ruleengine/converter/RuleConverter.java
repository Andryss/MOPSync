package ru.itmo.mopsync.ruleengine.converter;

import org.springframework.stereotype.Component;
import ru.itmo.mopsync.ruleengine.generated.model.RuleResponse;
import ru.itmo.mopsync.ruleengine.model.Rule;

/**
 * Converter for Rule entities and DTOs.
 */
@Component
public class RuleConverter {

    /**
     * Converts Rule entity to RuleResponse.
     *
     * @param rule rule entity
     * @return rule response
     */
    public RuleResponse toRuleResponse(Rule rule) {
        RuleResponse response = new RuleResponse();
        response.setId(rule.getId());
        response.setDeviceId(rule.getDeviceId());
        response.setMetricName(rule.getMetricName());
        response.setRuleContent(rule.getRuleContent());
        return response;
    }
}


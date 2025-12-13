package ru.itmo.mopsync.ruleengine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.itmo.mopsync.ruleengine.model.DeviceDataDocument;
import ru.itmo.mopsync.ruleengine.model.Rule;
import ru.itmo.mopsync.ruleengine.repository.DeviceDataRepository;

import java.util.List;
import java.util.Map;

/**
 * Service for evaluating rules against device data.
 * Supports recursive rule evaluation with JSON-based rule content.
 * Handles String, Integer, and Number metric types.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEvaluationService {

    private final DeviceDataRepository deviceDataRepository;

    /**
     * Evaluates if a rule is satisfied for the given device data.
     *
     * @param rule         rule to evaluate
     * @param deviceData   device data package to check
     * @return true if rule is satisfied, false otherwise
     */
    public boolean evaluateRule(Rule rule, DeviceDataDocument deviceData) {
        if (rule.getRuleContent() == null) {
            log.warn("Rule {} has null ruleContent", rule.getId());
            return false;
        }

        Object metricValue = deviceData.getMetrics().get(rule.getMetricName());
        if (metricValue == null) {
            log.debug("Metric {} not found in device data for device {}",
                    rule.getMetricName(), deviceData.getDeviceId());
            return false;
        }

        // Support String, Integer, and Number types
        if (!(metricValue instanceof String || metricValue instanceof Number)) {
            log.debug("Metric {} has unsupported type {} for device {}",
                    rule.getMetricName(), metricValue.getClass().getSimpleName(), deviceData.getDeviceId());
            return false;
        }

        return evaluateRuleContent(rule.getRuleContent(), metricValue, deviceData, rule.getMetricName());
    }

    /**
     * Recursively evaluates rule content against a metric value.
     *
     * @param ruleContent rule content map (JSON structure)
     * @param metricValue current metric value to evaluate (String, Integer, or Number)
     * @param deviceData  device data package (needed for repeat rules)
     * @param metricName  metric name (needed for repeat rules)
     * @return true if rule is satisfied
     */
    private boolean evaluateRuleContent(Map<String, Object> ruleContent, Object metricValue,
                                        DeviceDataDocument deviceData, String metricName) {
        Object typeObj = ruleContent.get("type");
        if (!(typeObj instanceof String type)) {
            log.warn("Rule content missing or invalid 'type' field: {}", ruleContent);
            return false;
        }

        return switch (type) {
            case "gt" -> evaluateComparison(metricValue, ruleContent, this::compareGreaterThan);
            case "lt" -> evaluateComparison(metricValue, ruleContent, this::compareLessThan);
            case "gte" -> evaluateComparison(metricValue, ruleContent, this::compareGreaterThanOrEqual);
            case "lte" -> evaluateComparison(metricValue, ruleContent, this::compareLessThanOrEqual);
            case "eq" -> evaluateComparison(metricValue, ruleContent, this::compareEqual);
            case "repeat" -> evaluateRepeatRule(ruleContent, deviceData, metricName);
            default -> {
                log.warn("Unknown rule type: {}", type);
                yield false;
            }
        };
    }

    /**
     * Evaluates a comparison rule (gt, lt, gte, lte, eq) with type-aware comparison.
     *
     * @param metricValue metric value to compare
     * @param ruleContent rule content map
     * @param comparator  comparison function
     * @return true if comparison is satisfied
     */
    private boolean evaluateComparison(Object metricValue, Map<String, Object> ruleContent,
                                       TypeAwareComparator comparator) {
        Object valueObj = ruleContent.get("value");
        if (valueObj == null) {
            log.warn("Rule content missing 'value' field");
            return false;
        }

        return comparator.compare(metricValue, valueObj);
    }

    /**
     * Compares two values with type-aware logic.
     * Supports String (lexicographic), Integer/Long (numeric), and Number (numeric with epsilon for equality).
     *
     * @param metricValue metric value
     * @param threshold   threshold value from rule
     * @return comparison result
     */
    private boolean compareGreaterThan(Object metricValue, Object threshold) {
        if (metricValue instanceof String && threshold instanceof String) {
            return ((String) metricValue).compareTo((String) threshold) > 0;
        } else if (metricValue instanceof Integer && threshold instanceof Integer) {
            return ((Integer) metricValue) > ((Integer) threshold);
        } else if (metricValue instanceof Long && threshold instanceof Long) {
            return ((Long) metricValue) > ((Long) threshold);
        } else if (metricValue instanceof Number && threshold instanceof Number) {
            return ((Number) metricValue).doubleValue() > ((Number) threshold).doubleValue();
        } else if (metricValue instanceof String && threshold instanceof Number) {
            // Try to parse string as number
            try {
                double metricNum = Double.parseDouble((String) metricValue);
                return metricNum > ((Number) threshold).doubleValue();
            } catch (NumberFormatException e) {
                log.debug("Cannot compare string metric as number: {}", metricValue);
                return false;
            }
        } else if (metricValue instanceof Number && threshold instanceof String) {
            // Try to parse threshold as number
            try {
                double thresholdNum = Double.parseDouble((String) threshold);
                return ((Number) metricValue).doubleValue() > thresholdNum;
            } catch (NumberFormatException e) {
                log.debug("Cannot compare string threshold as number: {}", threshold);
                return false;
            }
        } else {
            log.debug("Incompatible types for comparison: {} vs {}", metricValue.getClass(), threshold.getClass());
            return false;
        }
    }

    private boolean compareLessThan(Object metricValue, Object threshold) {
        if (metricValue instanceof String && threshold instanceof String) {
            return ((String) metricValue).compareTo((String) threshold) < 0;
        } else if (metricValue instanceof Integer && threshold instanceof Integer) {
            return ((Integer) metricValue) < ((Integer) threshold);
        } else if (metricValue instanceof Long && threshold instanceof Long) {
            return ((Long) metricValue) < ((Long) threshold);
        } else if (metricValue instanceof Number && threshold instanceof Number) {
            return ((Number) metricValue).doubleValue() < ((Number) threshold).doubleValue();
        } else if (metricValue instanceof String && threshold instanceof Number) {
            try {
                double metricNum = Double.parseDouble((String) metricValue);
                return metricNum < ((Number) threshold).doubleValue();
            } catch (NumberFormatException e) {
                log.debug("Cannot compare string metric as number: {}", metricValue);
                return false;
            }
        } else if (metricValue instanceof Number && threshold instanceof String) {
            try {
                double thresholdNum = Double.parseDouble((String) threshold);
                return ((Number) metricValue).doubleValue() < thresholdNum;
            } catch (NumberFormatException e) {
                log.debug("Cannot compare string threshold as number: {}", threshold);
                return false;
            }
        } else {
            log.debug("Incompatible types for comparison: {} vs {}", metricValue.getClass(), threshold.getClass());
            return false;
        }
    }

    private boolean compareGreaterThanOrEqual(Object metricValue, Object threshold) {
        if (metricValue instanceof String && threshold instanceof String) {
            return ((String) metricValue).compareTo((String) threshold) >= 0;
        } else if (metricValue instanceof Integer && threshold instanceof Integer) {
            return ((Integer) metricValue) >= ((Integer) threshold);
        } else if (metricValue instanceof Long && threshold instanceof Long) {
            return ((Long) metricValue) >= ((Long) threshold);
        } else if (metricValue instanceof Number && threshold instanceof Number) {
            return ((Number) metricValue).doubleValue() >= ((Number) threshold).doubleValue();
        } else if (metricValue instanceof String && threshold instanceof Number) {
            try {
                double metricNum = Double.parseDouble((String) metricValue);
                return metricNum >= ((Number) threshold).doubleValue();
            } catch (NumberFormatException e) {
                log.debug("Cannot compare string metric as number: {}", metricValue);
                return false;
            }
        } else if (metricValue instanceof Number && threshold instanceof String) {
            try {
                double thresholdNum = Double.parseDouble((String) threshold);
                return ((Number) metricValue).doubleValue() >= thresholdNum;
            } catch (NumberFormatException e) {
                log.debug("Cannot compare string threshold as number: {}", threshold);
                return false;
            }
        } else {
            log.debug("Incompatible types for comparison: {} vs {}", metricValue.getClass(), threshold.getClass());
            return false;
        }
    }

    private boolean compareLessThanOrEqual(Object metricValue, Object threshold) {
        if (metricValue instanceof String && threshold instanceof String) {
            return ((String) metricValue).compareTo((String) threshold) <= 0;
        } else if (metricValue instanceof Integer && threshold instanceof Integer) {
            return ((Integer) metricValue) <= ((Integer) threshold);
        } else if (metricValue instanceof Long && threshold instanceof Long) {
            return ((Long) metricValue) <= ((Long) threshold);
        } else if (metricValue instanceof Number && threshold instanceof Number) {
            return ((Number) metricValue).doubleValue() <= ((Number) threshold).doubleValue();
        } else if (metricValue instanceof String && threshold instanceof Number) {
            try {
                double metricNum = Double.parseDouble((String) metricValue);
                return metricNum <= ((Number) threshold).doubleValue();
            } catch (NumberFormatException e) {
                log.debug("Cannot compare string metric as number: {}", metricValue);
                return false;
            }
        } else if (metricValue instanceof Number && threshold instanceof String) {
            try {
                double thresholdNum = Double.parseDouble((String) threshold);
                return ((Number) metricValue).doubleValue() <= thresholdNum;
            } catch (NumberFormatException e) {
                log.debug("Cannot compare string threshold as number: {}", threshold);
                return false;
            }
        } else {
            log.debug("Incompatible types for comparison: {} vs {}", metricValue.getClass(), threshold.getClass());
            return false;
        }
    }

    private boolean compareEqual(Object metricValue, Object threshold) {
        if (metricValue instanceof String && threshold instanceof String) {
            return metricValue.equals(threshold);
        } else if (metricValue instanceof Integer && threshold instanceof Integer) {
            return metricValue.equals(threshold);
        } else if (metricValue instanceof Long && threshold instanceof Long) {
            return metricValue.equals(threshold);
        } else if (metricValue instanceof Number && threshold instanceof Number) {
            // Use epsilon for floating point comparison
            return Math.abs(((Number) metricValue).doubleValue() - ((Number) threshold).doubleValue()) < 0.0001;
        } else if (metricValue instanceof String && threshold instanceof Number) {
            try {
                double metricNum = Double.parseDouble((String) metricValue);
                return Math.abs(metricNum - ((Number) threshold).doubleValue()) < 0.0001;
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (metricValue instanceof Number && threshold instanceof String) {
            try {
                double thresholdNum = Double.parseDouble((String) threshold);
                return Math.abs(((Number) metricValue).doubleValue() - thresholdNum) < 0.0001;
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Evaluates a repeat rule (checks if condition is satisfied for N consecutive packages).
     *
     * @param ruleContent rule content map with type="repeat"
     * @param deviceData  current device data package
     * @param metricName  metric name to check
     * @return true if rule is satisfied for consecutiveCount packages
     */
    private boolean evaluateRepeatRule(Map<String, Object> ruleContent,
                                       DeviceDataDocument deviceData, String metricName) {
        Object timesObj = ruleContent.get("times");
        if (timesObj == null) {
            log.warn("Repeat rule missing 'times' field");
            return false;
        }

        int times;
        if (timesObj instanceof Number) {
            times = ((Number) timesObj).intValue();
        } else if (timesObj instanceof String) {
            try {
                times = Integer.parseInt((String) timesObj);
            } catch (NumberFormatException e) {
                log.warn("Repeat rule 'times' field is not a valid integer: {}", timesObj);
                return false;
            }
        } else {
            log.warn("Repeat rule 'times' field has invalid type: {}", timesObj.getClass());
            return false;
        }

        if (times <= 0) {
            log.warn("Repeat rule has invalid 'times' value: {}", times);
            return false;
        }

        Object valueObj = ruleContent.get("value");
        if (!(valueObj instanceof Map)) {
            log.warn("Repeat rule missing or invalid 'value' field");
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> innerRule = (Map<String, Object>) valueObj;

        // Get limited packages for this device, ordered by sequence descending (most recent first)
        // Limit to 'times' to avoid querying too much data
        List<DeviceDataDocument> recentPackages = deviceDataRepository
                .findByDeviceIdOrderBySeqDesc(deviceData.getDeviceId(), PageRequest.of(0, times));

        if (recentPackages.size() < times) {
            log.debug("Not enough packages for repeat rule. Required: {}, Available: {}", times, recentPackages.size());
            return false;
        }

        // Check the last 'times' packages (they are already sorted by seq descending)
        // We assume they have correct consecutive seq numbers
        for (DeviceDataDocument packageData : recentPackages) {
            Object packageMetricValue = packageData.getMetrics().get(metricName);
            if (packageMetricValue == null) {
                log.debug("Package doesn't have metric value for {}", metricName);
                return false; // Missing metric value breaks the consecutive chain
            }

            // Support String, Integer, and Number types
            if (!(packageMetricValue instanceof String || packageMetricValue instanceof Number)) {
                log.debug("Package has unsupported metric type for {}", metricName);
                return false; // Invalid metric type breaks the consecutive chain
            }

            if (!evaluateRuleContent(innerRule, packageMetricValue, packageData, metricName)) {
                log.debug("Package doesn't satisfy inner rule condition");
                return false; // Condition not satisfied breaks the consecutive chain
            }
        }

        return true; // All consecutive packages satisfy the condition
    }

    /**
     * Functional interface for type-aware comparison operations.
     */
    @FunctionalInterface
    private interface TypeAwareComparator {
        boolean compare(Object value, Object threshold);
    }
}

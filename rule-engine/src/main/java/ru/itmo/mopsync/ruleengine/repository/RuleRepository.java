package ru.itmo.mopsync.ruleengine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.itmo.mopsync.ruleengine.model.Rule;

import java.util.List;

/**
 * MongoDB repository for rules.
 */
public interface RuleRepository extends MongoRepository<Rule, String> {

    /**
     * Finds all rules for a specific device and metric.
     *
     * @param deviceId   device identifier
     * @param metricName metric name
     * @return list of rules
     */
    List<Rule> findByDeviceIdAndMetricName(String deviceId, String metricName);
}

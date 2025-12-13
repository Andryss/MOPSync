package ru.itmo.mopsync.ruleengine.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.itmo.mopsync.ruleengine.model.Alert;

/**
 * MongoDB repository for alerts.
 */
public interface AlertRepository extends MongoRepository<Alert, String> {
}

package ru.itmo.mopsync.iotcontroller.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.itmo.mopsync.iotcontroller.model.DeviceDataDocument;

/**
 * MongoDB repository for device data documents.
 */
public interface DeviceDataRepository extends MongoRepository<DeviceDataDocument, String> {
}

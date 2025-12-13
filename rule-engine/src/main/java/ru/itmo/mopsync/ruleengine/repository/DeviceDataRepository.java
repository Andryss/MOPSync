package ru.itmo.mopsync.ruleengine.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import ru.itmo.mopsync.ruleengine.model.DeviceDataDocument;

import java.util.List;

/**
 * MongoDB repository for device data documents.
 */
public interface DeviceDataRepository extends MongoRepository<DeviceDataDocument, String> {

    /**
     * Finds device data for a specific device, ordered by sequence number descending, with limit.
     *
     * @param deviceId device identifier
     * @param pageable pageable with limit
     * @return list of device data documents ordered by seq descending, limited by pageable
     */
    List<DeviceDataDocument> findByDeviceIdOrderBySeqDesc(String deviceId, Pageable pageable);
}

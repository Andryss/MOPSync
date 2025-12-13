package ru.itmo.mopsync.ruleengine.exception;

/**
 * Class describing all errors that occur in the application.
 */
public class Errors {
    /**
     * Device data not found error.
     *
     * @param deviceDataId device data identifier
     * @return BaseException for device data not found
     */
    public static BaseException deviceDataNotFoundError(String deviceDataId) {
        return new BaseException(404, "device.data.not.found",
                "Device data not found: " + deviceDataId);
    }
}

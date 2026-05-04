package id.icapps.savera.service.devices.cycling_sensor.support;

import org.slf4j.Logger;

import id.icapps.savera.service.btle.AbstractBTLEDeviceSupport;

public class CyclingSensorBaseSupport extends AbstractBTLEDeviceSupport {
    public CyclingSensorBaseSupport(Logger logger) {
        super(logger);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public boolean getImplicitCallbackModify() {
        return true;
    }

    @Override
    public boolean getSendWriteRequestResponse() {
        return false;
    }
}

package id.icapps.savera.devices.garmin.watches.forerunner;

import java.util.regex.Pattern;

import id.icapps.savera.R;
import id.icapps.savera.devices.garmin.GarminCoordinator;

public class GarminForerunner255Coordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Forerunner 255$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_forerunner_255;
    }
}

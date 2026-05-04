package id.icapps.savera.devices.garmin.watches.instinct;

import id.icapps.savera.R;
import id.icapps.savera.devices.garmin.GarminCoordinator;
import id.icapps.savera.impl.GBDevice;

import java.util.regex.Pattern;

public class GarminInstinct2SolarCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Instinct 2 Solar$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_instinct_2_solar;
    }
}

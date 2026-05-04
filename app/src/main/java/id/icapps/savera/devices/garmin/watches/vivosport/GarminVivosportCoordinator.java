package id.icapps.savera.devices.garmin.watches.vivosport;

import java.util.regex.Pattern;

import id.icapps.savera.R;
import id.icapps.savera.devices.garmin.GarminCoordinator;

public class GarminVivosportCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^vívosport$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_vivosport;
    }
}

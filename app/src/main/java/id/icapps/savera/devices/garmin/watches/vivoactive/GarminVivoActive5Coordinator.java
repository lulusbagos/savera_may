package id.icapps.savera.devices.garmin.watches.vivoactive;

import java.util.regex.Pattern;

import id.icapps.savera.R;
import id.icapps.savera.devices.garmin.GarminCoordinator;

public class GarminVivoActive5Coordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^vívoactive 5$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_vivoactive_5;
    }
}
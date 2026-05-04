package id.icapps.savera.devices.garmin.watches.instinct;

import java.util.regex.Pattern;

import id.icapps.savera.R;
import id.icapps.savera.devices.garmin.GarminCoordinator;

public class GarminInstinctCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Instinct$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_instinct;
    }
}

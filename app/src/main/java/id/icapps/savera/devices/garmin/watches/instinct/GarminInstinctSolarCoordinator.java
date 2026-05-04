package id.icapps.savera.devices.garmin.watches.instinct;

import java.util.regex.Pattern;

import id.icapps.savera.R;
import id.icapps.savera.devices.garmin.GarminCoordinator;
import id.icapps.savera.impl.GBDevice;

public class GarminInstinctSolarCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Instinct Solar$");
    }

    @Override
    public int getCannedRepliesSlotCount(final GBDevice device) {
        return 16;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_instinct_solar;
    }
}

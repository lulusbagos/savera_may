package id.icapps.savera.devices.garmin.watches.swim;

import java.util.regex.Pattern;

import id.icapps.savera.R;
import id.icapps.savera.devices.garmin.GarminCoordinator;

public class GarminSwim2Coordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Swim 2$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_swim_2;
    }
}

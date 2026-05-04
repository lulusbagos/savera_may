package id.icapps.savera.devices.garmin.watches.forerunner;

import java.util.regex.Pattern;

import id.icapps.savera.R;
import id.icapps.savera.devices.garmin.GarminCoordinator;

public class GarminForerunner265SCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        // #4131 - including variants of the name just in case
        return Pattern.compile("^(Garmin )?Forerunner 265[sS]$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_forerunner_265s;
    }
}

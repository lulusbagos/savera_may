package id.icapps.savera.devices.garmin.watches.epix;

import java.util.regex.Pattern;

import id.icapps.savera.R;
import id.icapps.savera.devices.garmin.GarminCoordinator;

public class GarminEpixProCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^EPIX PRO - \\d+mm$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_epix_pro;
    }
}

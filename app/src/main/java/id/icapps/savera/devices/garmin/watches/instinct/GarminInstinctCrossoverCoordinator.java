package id.icapps.savera.devices.garmin.watches.instinct;

import id.icapps.savera.R;
import id.icapps.savera.devices.garmin.GarminCoordinator;

import java.util.regex.Pattern;

public class GarminInstinctCrossoverCoordinator extends GarminCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Instinct Crossover$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_garmin_instinct_crossover;
    }
}

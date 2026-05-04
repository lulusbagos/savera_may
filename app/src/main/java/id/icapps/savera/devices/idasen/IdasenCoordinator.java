package id.icapps.savera.devices.idasen;

import android.app.Activity;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import id.icapps.savera.GBException;
import id.icapps.savera.R;
import id.icapps.savera.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import id.icapps.savera.devices.AbstractBLEDeviceCoordinator;
import id.icapps.savera.entities.DaoSession;
import id.icapps.savera.entities.Device;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.service.DeviceSupport;
import id.icapps.savera.service.devices.idasen.IdasenDeviceSupport;

public class IdasenCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }

    @Override
    public String getManufacturer() {
        return "IKEA";
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Desk 1000", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_idasen;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return IdasenDeviceSupport.class;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new IdasenSettingsCustomizer();
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_BOND;
    }

    @Override
    public boolean supportsAppsManagement(final GBDevice device) {
        return true;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return IdasenControlActivity.class;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_idasen
        };
    }
}

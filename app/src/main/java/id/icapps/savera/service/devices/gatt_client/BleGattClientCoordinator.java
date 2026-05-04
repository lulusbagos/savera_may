package id.icapps.savera.service.devices.gatt_client;

import androidx.annotation.NonNull;

import id.icapps.savera.GBException;
import id.icapps.savera.R;
import id.icapps.savera.devices.AbstractBLEDeviceCoordinator;
import id.icapps.savera.entities.DaoSession;
import id.icapps.savera.entities.Device;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.impl.GBDeviceCandidate;
import id.icapps.savera.service.DeviceSupport;

public class BleGattClientCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    public String getManufacturer() {
        return "Generic";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return BleGattClientSupport.class;
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        // can only add through debug settings
        return false;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_ble_gatt_client;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_scannable;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_scannable_disabled;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }
}

package id.icapps.savera.devices.cycling_sensor.coordinator;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import id.icapps.savera.GBException;
import id.icapps.savera.R;
import id.icapps.savera.devices.AbstractBLEDeviceCoordinator;
import id.icapps.savera.devices.InstallHandler;
import id.icapps.savera.devices.TimeSampleProvider;
import id.icapps.savera.devices.cycling_sensor.activity.CyclingLiveDataActivity;
import id.icapps.savera.devices.cycling_sensor.db.CyclingSampleProvider;
import id.icapps.savera.entities.CyclingSample;
import id.icapps.savera.entities.CyclingSampleDao;
import id.icapps.savera.entities.DaoSession;
import id.icapps.savera.entities.Device;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.impl.GBDeviceCandidate;
import id.icapps.savera.service.DeviceSupport;
import id.icapps.savera.service.devices.cycling_sensor.support.CyclingSensorSupport;

public class CyclingSensorCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
        final Long deviceId = device.getId();

        session.getCyclingSampleDao().queryBuilder()
                .where(CyclingSampleDao.Properties.DeviceId.eq(deviceId))
                .buildDelete().executeDeleteWithoutDetachingEntities();
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        return candidate.supportsService(CyclingSensorSupport.UUID_CYCLING_SENSOR_SERVICE);
    }

    @Override
    public boolean supportsCyclingData() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public TimeSampleProvider<CyclingSample> getCyclingSampleProvider(GBDevice device, DaoSession session) {
        return new CyclingSampleProvider(device, session);
    }

    @Override
    public boolean supportsSleepMeasurement() {
        return false;
    }

    @Override
    public boolean supportsStepCounter() {
        return false;
    }

    @Override
    public boolean supportsSpeedzones() {
        return false;
    }

    @Override
    public boolean supportsActivityTabs() {
        return false;
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null;
    }

    @Override
    public String getManufacturer() {
        return "Generic";
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return CyclingLiveDataActivity.class;
    }

    @Override
    public boolean supportsRealtimeData() {
        return false;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_cycling_sensor
        };
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return CyclingSensorSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_cycling_sensor;
    }

    @Override
    public boolean supportsAppsManagement(GBDevice device) {
        return true;
    }


}

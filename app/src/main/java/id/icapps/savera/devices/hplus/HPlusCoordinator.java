/*  Copyright (C) 2016-2024 Andreas Shimokawa, Carsten Pfeiffer, Damien
    Gaignon, Daniel Dakhno, Daniele Gobbetti, João Paulo Barraca, José Rebelo,
    Lesur Frederic, Petr Vaněk

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package id.icapps.savera.devices.hplus;

import android.bluetooth.le.ScanFilter;
import android.content.SharedPreferences;
import android.os.ParcelUuid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;

import de.greenrobot.dao.query.QueryBuilder;
import id.icapps.savera.GBApplication;
import id.icapps.savera.GBException;
import id.icapps.savera.R;
import id.icapps.savera.activities.SettingsActivity;
import id.icapps.savera.activities.devicesettings.DeviceSettingsPreferenceConst;
import id.icapps.savera.devices.AbstractBLEDeviceCoordinator;
import id.icapps.savera.devices.SampleProvider;
import id.icapps.savera.entities.DaoSession;
import id.icapps.savera.entities.Device;
import id.icapps.savera.entities.HPlusHealthActivitySampleDao;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.model.ActivitySample;
import id.icapps.savera.model.ActivityUser;
import id.icapps.savera.service.DeviceSupport;
import id.icapps.savera.service.devices.hplus.HPlusSupport;
import id.icapps.savera.util.Prefs;

import static id.icapps.savera.GBApplication.getContext;

public class HPlusCoordinator extends AbstractBLEDeviceCoordinator {
    protected static final Logger LOG = LoggerFactory.getLogger(HPlusCoordinator.class);
    protected static Prefs prefs = GBApplication.getPrefs();

    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid hpService = new ParcelUuid(HPlusConstants.UUID_SERVICE_HP);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(hpService).build();
        return Collections.singletonList(filter);
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("HPLUS.*");
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public boolean supportsRealtimeData() {
        return true;
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new HPlusHealthSampleProvider(device, session);
    }

    @Override
    public int getAlarmSlotCount(GBDevice device) {
        return 3; // FIXME - check the real value
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public String getManufacturer() {
        return "Zeblaze";
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
        Long deviceId = device.getId();
        QueryBuilder<?> qb = session.getHPlusHealthActivitySampleDao().queryBuilder();
        qb.where(HPlusHealthActivitySampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
    }

    public static byte getLanguage(String address) {
        String language = prefs.getString("language", "default");
        Locale locale;

        if (language.equals("default")) {
            locale = Locale.getDefault();
        } else {
            locale = new Locale(language);
        }

        if (locale.getLanguage().equals(new Locale("cn").getLanguage())) {
            return HPlusConstants.ARG_LANGUAGE_CN;
        } else {
            return HPlusConstants.ARG_LANGUAGE_EN;
        }
    }

    public static byte getUnit(String address) {
        String units = prefs.getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, getContext().getString(R.string.p_unit_metric));

        if (units.equals(getContext().getString(R.string.p_unit_metric))) {
            return HPlusConstants.ARG_UNIT_METRIC;
        } else {
            return HPlusConstants.ARG_UNIT_IMPERIAL;
        }
    }

    public static byte getUserWeight() {
        ActivityUser activityUser = new ActivityUser();

        return (byte) (activityUser.getWeightKg() & 0xFF);
    }

    public static byte getUserHeight() {
        ActivityUser activityUser = new ActivityUser();

        return (byte) (activityUser.getHeightCm() & 0xFF);
    }

    public static byte getUserAge() {
        ActivityUser activityUser = new ActivityUser();

        return (byte) (activityUser.getAge() & 0xFF);
    }

    public static byte getUserGender() {
        ActivityUser activityUser = new ActivityUser();

        if (activityUser.getGender() == ActivityUser.GENDER_MALE)
            return HPlusConstants.ARG_GENDER_MALE;

        return HPlusConstants.ARG_GENDER_FEMALE;
    }

    public static int getGoal() {
        ActivityUser activityUser = new ActivityUser();

        return activityUser.getStepsGoal();
    }

    public static byte getSocial(String address) {
        //TODO: Figure what this is. Returning the default value

        return (byte) 255;
    }

    //FIXME: unused
    public static byte getUserWrist(String deviceAddress) {
        SharedPreferences sharedPreferences = GBApplication.getDeviceSpecificSharedPrefs(deviceAddress);
        String value = sharedPreferences.getString(DeviceSettingsPreferenceConst.PREF_WEARLOCATION, "left");

        if ("left".equals(value)) {
            return HPlusConstants.ARG_WRIST_LEFT;
        } else {
            return HPlusConstants.ARG_WRIST_RIGHT;
        }
    }

    public static int getSITStartTime(String address) {
        return prefs.getInt(HPlusConstants.PREF_HPLUS_SIT_START_TIME, 0);
    }

    public static int getSITEndTime(String address) {
        return prefs.getInt(HPlusConstants.PREF_HPLUS_SIT_END_TIME, 0);
    }

    public static void setDisplayIncomingMessageIcon(String address, boolean state) {
        SharedPreferences.Editor editor = prefs.getPreferences().edit();
        editor.putBoolean(HPlusConstants.PREF_HPLUS_DISPLAY_NOTIFICATION_ICON + "_" + address, state);
        editor.apply();
    }

    public static boolean getDisplayIncomingMessageIcon(String address) {
        return (prefs.getBoolean(HPlusConstants.PREF_HPLUS_DISPLAY_NOTIFICATION_ICON + "_" + address, false));
    }

    public static void setUnicodeSupport(String address, boolean state) {
        SharedPreferences.Editor editor = prefs.getPreferences().edit();
        editor.putBoolean(HPlusConstants.PREF_HPLUS_UNICODE + "_" + address, state);
        editor.apply();
    }

    public static boolean getUnicodeSupport(String address) {
        return (prefs.getBoolean(HPlusConstants.PREF_HPLUS_UNICODE + "_" + address, false));
    }

    public static void setNotificationLinesNumber(String address, int lineNumber) {
        SharedPreferences.Editor editor = prefs.getPreferences().edit();
        editor.putInt(HPlusConstants.PREF_HPLUS_NOTIFICATION_LINES + "_" + address, lineNumber);
        editor.apply();
    }

    public static int getNotificationLinesNumber(String address) {
        return (prefs.getInt(HPlusConstants.PREF_HPLUS_NOTIFICATION_LINES + "_" + address, 5));
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                //R.xml.devicesettings_wearlocation, // disabled, since it is never used in code
                R.xml.devicesettings_timeformat,
                R.xml.devicesettings_hplus,
                R.xml.devicesettings_transliteration
        };
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return HPlusSupport.class;
    }


    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_hplus;
    }


    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_hplus;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_hplus_disabled;
    }
}
    

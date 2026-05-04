package id.icapps.savera.devices.pebble;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import id.icapps.savera.R;
import id.icapps.savera.activities.devicesettings.DeviceSettingsPreferenceConst;
import id.icapps.savera.activities.devicesettings.DeviceSettingsUtils;
import id.icapps.savera.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import id.icapps.savera.activities.devicesettings.DeviceSpecificSettingsHandler;
import id.icapps.savera.devices.garmin.GarminPreferences;
import id.icapps.savera.devices.garmin.GarminRealtimeSettingsActivity;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.service.devices.garmin.agps.GarminAgpsStatus;
import id.icapps.savera.util.GB;
import id.icapps.savera.util.Prefs;

import static id.icapps.savera.util.GB.toast;

public class PebbleSettingsCustomizer implements DeviceSpecificSettingsCustomizer {
    public static final Creator<PebbleSettingsCustomizer> CREATOR = new Creator<PebbleSettingsCustomizer>() {
        @Override
        public PebbleSettingsCustomizer createFromParcel(final Parcel in) {
            return new PebbleSettingsCustomizer();
        }

        @Override
        public PebbleSettingsCustomizer[] newArray(final int size) {
            return new PebbleSettingsCustomizer[size];
        }
    };
    private static final Logger LOG = LoggerFactory.getLogger(PebbleSettingsCustomizer.class);
    private final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    public void onPreferenceChange(final Preference preference, final DeviceSpecificSettingsHandler handler) {
    }

    @Override
    public void customizeSettings(final DeviceSpecificSettingsHandler handler, final Prefs prefs, final String rootKey) {
        final EditTextPreference pref = handler.findPreference("pebble_mtu_limit");
        if (pref == null) {
            return;
        }
        pref.setOnBindEditTextListener(p -> {
            p.setInputType(InputType.TYPE_CLASS_NUMBER);
            p.setSelection(p.getText().length());
        });
    }

    @Override
    public Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
    }
}

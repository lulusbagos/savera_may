package id.icapps.savera.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.PreferenceFragmentCompat;

import id.icapps.savera.GBApplication;
import id.icapps.savera.R;
import id.icapps.savera.util.GB;

public class PairingOptions extends AbstractSettingsActivityV2 {
    @Override
    protected String fragmentTag() {
        return DiscoveryPairingPreferenceFragment.FRAGMENT_TAG;
    }

    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new DiscoveryPairingPreferenceFragment();
    }

    public static class DiscoveryPairingPreferenceFragment extends AbstractPreferenceFragment {
        static final String FRAGMENT_TAG = "DISCOVERY_PAIRING_PREFERENCES_FRAGMENT";

        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.discovery_pairing_preferences, rootKey);

            findPreference("prefs_general_key_auto_reconnect_scan").setOnPreferenceChangeListener((preference, newValue) -> {
                GB.toast(GBApplication.getContext().getString(R.string.prompt_restart_gadgetbridge), Toast.LENGTH_LONG, GB.INFO);
                return true;
            });
        }
    }
}

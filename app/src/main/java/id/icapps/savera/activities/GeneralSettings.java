package id.icapps.savera.activities;

import android.os.Bundle;
import android.text.InputType;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import id.icapps.savera.GBApplication;
import id.icapps.savera.R;

public class GeneralSettings extends AbstractSettingsActivityV2 {
    @Override
    protected String fragmentTag() {
        return SettingsFragment.FRAGMENT_TAG;
    }

    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new SettingsFragment();
    }

    public static class SettingsFragment extends AbstractPreferenceFragment {
        static final String FRAGMENT_TAG = "SETTINGS_FRAGMENT";

        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
            setPreferencesFromResource(R.xml.general_settings, rootKey);

            setInputTypeFor("location_latitude", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            setInputTypeFor("location_longitude", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            setInputTypeFor("auto_fetch_interval_limit", InputType.TYPE_CLASS_NUMBER);

            Preference pref = findPreference("auto_fetch_interval_limit");
            if (pref != null) {
                pref.setOnPreferenceChangeListener((preference, autoFetchInterval) -> {
                    String summary = String.format(
                            requireContext().getApplicationContext().getString(R.string.pref_auto_fetch_limit_fetches_summary),
                            Integer.valueOf((String) autoFetchInterval));
                    preference.setSummary(summary);
                    return true;
                });

                int autoFetchInterval = GBApplication.getPrefs().getInt("auto_fetch_interval_limit", 0);
                String summary = String.format(
                        requireContext().getApplicationContext().getString(R.string.pref_auto_fetch_limit_fetches_summary),
                        autoFetchInterval);
                pref.setSummary(summary);
            }
        }
    }
}

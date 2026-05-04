package id.icapps.savera.externalevents.sleepasandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.icapps.savera.GBApplication;
import id.icapps.savera.model.DeviceService;

public class SleepAsAndroidReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(SleepAsAndroidReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (GBApplication.getPrefs().getBoolean("pref_key_sleepasandroid_enable", false)) {
            GBApplication.deviceService().onSleepAsAndroidAction(action, intent.getExtras());
        }
    }
}
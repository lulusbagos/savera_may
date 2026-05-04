package id.icapps.savera.service;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.Location;
import android.net.Uri;

import java.util.ArrayList;
import java.util.UUID;

import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.model.Alarm;
import id.icapps.savera.model.CalendarEventSpec;
import id.icapps.savera.model.CallSpec;
import id.icapps.savera.model.CannedMessagesSpec;
import id.icapps.savera.model.MusicSpec;
import id.icapps.savera.model.MusicStateSpec;
import id.icapps.savera.model.NotificationSpec;
import id.icapps.savera.model.Reminder;
import id.icapps.savera.model.WeatherSpec;
import id.icapps.savera.model.WorldClock;

class TestDeviceSupport extends AbstractDeviceSupport {

    TestDeviceSupport() {
    }

    @Override
    public void setContext(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context) {
        super.setContext(gbDevice, btAdapter, context);
    }

    @Override
    public boolean connect() {
        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());
        return true;
    }

    @Override
    public void dispose() {

    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }
}

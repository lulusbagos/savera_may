/*  Copyright (C) 2022-2024 Noodlez

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
package id.icapps.savera.service.devices.asteroidos;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.UUID;

import id.icapps.savera.deviceevents.GBDeviceEventBatteryInfo;
import id.icapps.savera.deviceevents.GBDeviceEventMusicControl;
import id.icapps.savera.devices.asteroidos.AsteroidOSConstants;
import id.icapps.savera.devices.asteroidos.AsteroidOSMediaCommand;
import id.icapps.savera.devices.asteroidos.AsteroidOSNotification;
import id.icapps.savera.devices.asteroidos.AsteroidOSWeather;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.model.CallSpec;
import id.icapps.savera.model.MusicSpec;
import id.icapps.savera.model.MusicStateSpec;
import id.icapps.savera.model.NotificationSpec;
import id.icapps.savera.model.WeatherSpec;
import id.icapps.savera.service.btle.AbstractBTLEDeviceSupport;
import id.icapps.savera.service.btle.BLETypeConversions;
import id.icapps.savera.service.btle.GattService;
import id.icapps.savera.service.btle.TransactionBuilder;
import id.icapps.savera.service.btle.actions.SetDeviceStateAction;
import id.icapps.savera.service.btle.profiles.IntentListener;
import id.icapps.savera.service.btle.profiles.battery.BatteryInfoProfile;
import id.icapps.savera.service.devices.lefun.requests.SetTimeRequest;

public class AsteroidOSDeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AsteroidOSDeviceSupport.class);
    private final BatteryInfoProfile<AsteroidOSDeviceSupport> batteryInfoProfile;
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();

    public AsteroidOSDeviceSupport() {
        super(LOG);
        addSupportedService(AsteroidOSConstants.SERVICE_UUID);
        addSupportedService(AsteroidOSConstants.TIME_SERVICE_UUID);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(AsteroidOSConstants.WEATHER_SERVICE_UUID);
        addSupportedService(AsteroidOSConstants.NOTIFICATION_SERVICE_UUID);
        addSupportedService(AsteroidOSConstants.MEDIA_SERVICE_UUID);

        IntentListener mListener = intent -> {
            String action = intent.getAction();
            if (BatteryInfoProfile.ACTION_BATTERY_INFO.equals(action)) {
                handleBatteryInfo(Objects.requireNonNull(intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO)));
            }
        };

        batteryInfoProfile = new BatteryInfoProfile<>(this);
        batteryInfoProfile.addListener(mListener);
        addSupportedProfile(batteryInfoProfile);
    }

    private void handleBatteryInfo(@NonNull id.icapps.savera.service.btle.profiles.battery.BatteryInfo info) {
        batteryCmd.level = info.getPercentCharged();
        handleGBDeviceEvent(batteryCmd);
    }

    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        UUID characteristicUUID = characteristic.getUuid();

        if (characteristicUUID.equals(AsteroidOSConstants.MEDIA_COMMANDS_CHAR)) {
            handleMediaCommand(characteristic);
            return true;
        }

        LOG.info("Characteristic changed UUID: " + characteristicUUID);
        LOG.info("Characteristic changed value: " + Arrays.toString(characteristic.getValue()));
        return false;
    }


    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        builder.notify(getCharacteristic(AsteroidOSConstants.MEDIA_COMMANDS_CHAR), true);
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        batteryInfoProfile.requestBatteryInfo(builder);
        batteryInfoProfile.enableNotify(builder, true);
        // Gadgetbridge doesn't seem to do this itself, so we force it to set its time
        onSetTime();
        return builder;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        AsteroidOSNotification notif = new AsteroidOSNotification(notificationSpec);
        TransactionBuilder builder = new TransactionBuilder("send notification");
        safeWriteToCharacteristic(builder, AsteroidOSConstants.NOTIFICATION_UPDATE_CHAR, notif.toString().getBytes(StandardCharsets.UTF_8));
        builder.queue(getQueue());
    }

    @Override
    public void onDeleteNotification(int id) {
        AsteroidOSNotification notif = new AsteroidOSNotification(id);
        TransactionBuilder builder = new TransactionBuilder("delete notification");
        safeWriteToCharacteristic(builder, AsteroidOSConstants.NOTIFICATION_UPDATE_CHAR, notif.toString().getBytes(StandardCharsets.UTF_8));
        builder.queue(getQueue());
    }

    @Override
    public void onSetTime() {
        GregorianCalendar now = BLETypeConversions.createCalendar();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write((byte) now.get(Calendar.YEAR) - 1900);
        baos.write((byte) now.get(Calendar.MONTH));
        baos.write((byte) now.get(Calendar.DAY_OF_MONTH));
        baos.write((byte) now.get(Calendar.HOUR_OF_DAY));
        baos.write((byte) now.get(Calendar.MINUTE));
        baos.write((byte) now.get(Calendar.SECOND));
        TransactionBuilder builder = new TransactionBuilder("set time");
        safeWriteToCharacteristic(builder, AsteroidOSConstants.TIME_SET_CHAR, baos.toByteArray());
        builder.queue(getQueue());
    }


    @Override
    public void onSetCallState(CallSpec callSpec) {
        AsteroidOSNotification call = new AsteroidOSNotification(callSpec);
        TransactionBuilder builder = new TransactionBuilder("send call");
        safeWriteToCharacteristic(builder, AsteroidOSConstants.NOTIFICATION_UPDATE_CHAR, call.toString().getBytes(StandardCharsets.UTF_8));
        builder.queue(getQueue());
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        TransactionBuilder builder = new TransactionBuilder("set music state");
        if (stateSpec.state == MusicStateSpec.STATE_PLAYING) {
            safeWriteToCharacteristic(builder, AsteroidOSConstants.MEDIA_PLAYING_CHAR, new byte[]{1});
        } else {
            safeWriteToCharacteristic(builder, AsteroidOSConstants.MEDIA_PLAYING_CHAR, new byte[]{0});
        }
        builder.queue(getQueue());
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        TransactionBuilder builder = new TransactionBuilder("send music information");
        // Send title
        {
            byte[] track_bytes;
            if (musicSpec.track != null)
                track_bytes = musicSpec.track.getBytes(StandardCharsets.UTF_8);
            else
                track_bytes = "\"\"".getBytes(StandardCharsets.UTF_8);
            safeWriteToCharacteristic(builder, AsteroidOSConstants.MEDIA_TITLE_CHAR, track_bytes);
        }
        // Send album
        {
            byte[] album_bytes;
            if (musicSpec.album != null)
                album_bytes = musicSpec.album.getBytes(StandardCharsets.UTF_8);
            else
                album_bytes = "\"\"".getBytes(StandardCharsets.UTF_8);
            safeWriteToCharacteristic(builder, AsteroidOSConstants.MEDIA_ALBUM_CHAR, album_bytes);
        }
        // Send artist
        {
            byte[] artist_bytes;
            if (musicSpec.artist != null)
                artist_bytes = musicSpec.artist.getBytes(StandardCharsets.UTF_8);
            else
                artist_bytes = "\"\"".getBytes(StandardCharsets.UTF_8);
            safeWriteToCharacteristic(builder, AsteroidOSConstants.MEDIA_ARTIST_CHAR, artist_bytes);
        }
        builder.queue(getQueue());
    }

    @Override
    public void onSetPhoneVolume(float volume) {
        TransactionBuilder builder = new TransactionBuilder("send volume information");
        byte volByte = (byte) Math.round(volume);
        safeWriteToCharacteristic(builder, AsteroidOSConstants.MEDIA_VOLUME_CHAR, new byte[]{volByte});
        builder.queue(getQueue());
    }

    @Override
    public void onFindDevice(boolean start) {
        final CallSpec callSpec = new CallSpec();
        callSpec.command = start ? CallSpec.CALL_INCOMING : CallSpec.CALL_END;
        callSpec.name = "Saverawatch";
        onSetCallState(callSpec);
    }

    @Override
    public void onSendWeather(ArrayList<WeatherSpec> weatherSpecs) {
        WeatherSpec weatherSpec = weatherSpecs.get(0);
        AsteroidOSWeather asteroidOSWeather = new AsteroidOSWeather(weatherSpec);
        TransactionBuilder builder = new TransactionBuilder("send weather info");
        // Send city name
        safeWriteToCharacteristic(builder, AsteroidOSConstants.WEATHER_CITY_CHAR, asteroidOSWeather.getCityName());
        // Send conditions
        safeWriteToCharacteristic(builder, AsteroidOSConstants.WEATHER_IDS_CHAR, asteroidOSWeather.getWeatherConditions());
        // Send min temps
        safeWriteToCharacteristic(builder, AsteroidOSConstants.WEATHER_MIN_TEMPS_CHAR, asteroidOSWeather.getMinTemps());
        // Send max temps
        safeWriteToCharacteristic(builder, AsteroidOSConstants.WEATHER_MAX_TEMPS_CHAR, asteroidOSWeather.getMaxTemps());
        // Flush queue
        builder.queue(getQueue());
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    /**
     * This will check if the characteristic exists and can be written
     * <p>
     * Keeps backwards compatibility with firmware that can't take all the information
     */
    private void safeWriteToCharacteristic(TransactionBuilder builder, UUID uuid, byte[] data) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(uuid);
        if (characteristic != null &&
                (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
            builder.write(characteristic, data);
        } else {
            LOG.warn("Tried to write to a characteristic that did not exist or was not writable!");
        }
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic, int status) {
        if (super.onCharacteristicRead(gatt, characteristic, status)) {
            return true;
        }
        UUID characteristicUUID = characteristic.getUuid();

        LOG.info("Unhandled characteristic read: " + characteristicUUID);
        return false;
    }


    /**
     * Handles a media command sent from the AsteroidOS device
     *
     * @param characteristic The Characteristic information
     */
    public void handleMediaCommand(BluetoothGattCharacteristic characteristic) {
        LOG.info("handle media command");
        AsteroidOSMediaCommand command = new AsteroidOSMediaCommand(characteristic.getValue()[0]);
        GBDeviceEventMusicControl event = command.toMusicControlEvent();
        evaluateGBDeviceEvent(event);
    }

    @Override
    public boolean getImplicitCallbackModify() {
        return true;
    }

    @Override
    public boolean getSendWriteRequestResponse() {
        return false;
    }
}

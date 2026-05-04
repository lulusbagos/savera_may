/*  Copyright (C) 2021-2024 Daniel Dakhno, José Rebelo

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
package id.icapps.savera.service.devices.sony.headphones.protocol.impl;

import java.util.List;

import id.icapps.savera.deviceevents.GBDeviceEvent;
import id.icapps.savera.devices.sony.headphones.SonyHeadphonesCoordinator;
import id.icapps.savera.devices.sony.headphones.prefs.AdaptiveVolumeControl;
import id.icapps.savera.devices.sony.headphones.prefs.AmbientSoundControl;
import id.icapps.savera.devices.sony.headphones.prefs.AmbientSoundControlButtonMode;
import id.icapps.savera.devices.sony.headphones.prefs.AudioUpsampling;
import id.icapps.savera.devices.sony.headphones.prefs.AutomaticPowerOff;
import id.icapps.savera.devices.sony.headphones.prefs.ButtonModes;
import id.icapps.savera.devices.sony.headphones.prefs.EqualizerCustomBands;
import id.icapps.savera.devices.sony.headphones.prefs.EqualizerPreset;
import id.icapps.savera.devices.sony.headphones.prefs.PauseWhenTakenOff;
import id.icapps.savera.devices.sony.headphones.prefs.QuickAccess;
import id.icapps.savera.devices.sony.headphones.prefs.SoundPosition;
import id.icapps.savera.devices.sony.headphones.prefs.SpeakToChatConfig;
import id.icapps.savera.devices.sony.headphones.prefs.SpeakToChatEnabled;
import id.icapps.savera.devices.sony.headphones.prefs.SurroundMode;
import id.icapps.savera.devices.sony.headphones.prefs.TouchSensor;
import id.icapps.savera.devices.sony.headphones.prefs.VoiceNotifications;
import id.icapps.savera.devices.sony.headphones.prefs.WideAreaTap;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.service.devices.sony.headphones.protocol.Request;
import id.icapps.savera.service.devices.sony.headphones.protocol.MessageType;
import id.icapps.savera.service.devices.sony.headphones.protocol.impl.v1.params.BatteryType;
import id.icapps.savera.util.DeviceHelper;

public abstract class AbstractSonyProtocolImpl {
    private final GBDevice device;

    public AbstractSonyProtocolImpl(final GBDevice device) {
        this.device = device;
    }

    protected GBDevice getDevice() {
        return this.device;
    }

    protected SonyHeadphonesCoordinator getCoordinator() {
        return (SonyHeadphonesCoordinator) getDevice().getDeviceCoordinator();
    }

    public abstract Request getAmbientSoundControl();

    public abstract Request setAmbientSoundControl(final AmbientSoundControl config);


    public abstract Request setAdaptiveVolumeControl(final AdaptiveVolumeControl config);

    public abstract Request getAdaptiveVolumeControl();

    public abstract Request setSpeakToChatEnabled(final SpeakToChatEnabled config);

    public abstract Request getSpeakToChatEnabled();

    public abstract Request setSpeakToChatConfig(final SpeakToChatConfig config);

    public abstract Request getSpeakToChatConfig();

    public abstract Request getNoiseCancellingOptimizerState();

    public abstract Request getAudioCodec();

    public abstract Request getBattery(final BatteryType batteryType);

    public abstract Request getFirmwareVersion();

    public abstract Request getAudioUpsampling();

    public abstract Request setAudioUpsampling(final AudioUpsampling config);

    public abstract Request getAutomaticPowerOff();

    public abstract Request setAutomaticPowerOff(final AutomaticPowerOff config);

    public abstract Request setWideAreaTap(final WideAreaTap config);

    public abstract Request getWideAreaTap();

    public abstract Request getButtonModes();

    public abstract Request setButtonModes(final ButtonModes config);

    public abstract Request getQuickAccess();

    public abstract Request setQuickAccess(final QuickAccess quickAccess);

    public abstract Request getAmbientSoundControlButtonMode();

    public abstract Request setAmbientSoundControlButtonMode(final AmbientSoundControlButtonMode ambientSoundControlButtonMode);

    public abstract Request getPauseWhenTakenOff();

    public abstract Request setPauseWhenTakenOff(final PauseWhenTakenOff config);

    public abstract Request getEqualizer();

    public abstract Request setEqualizerPreset(final EqualizerPreset config);

    public abstract Request setEqualizerCustomBands(final EqualizerCustomBands config);

    public abstract Request getSoundPosition();

    public abstract Request setSoundPosition(final SoundPosition config);

    public abstract Request getSurroundMode();

    public abstract Request setSurroundMode(final SurroundMode config);

    public abstract Request getTouchSensor();

    public abstract Request setTouchSensor(final TouchSensor config);

    public abstract Request getVoiceNotifications();

    public abstract Request setVoiceNotifications(final VoiceNotifications config);

    public abstract Request startNoiseCancellingOptimizer(final boolean start);

    public abstract Request powerOff();

    public abstract Request getVolume();

    public abstract Request setVolume(final int volume);

    public abstract List<? extends GBDeviceEvent> handlePayload(final MessageType messageType, final byte[] payload);
}

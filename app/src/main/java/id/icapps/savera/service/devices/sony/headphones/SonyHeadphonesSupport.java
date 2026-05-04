/*  Copyright (C) 2021-2024 Arjan Schrijver, José Rebelo

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
package id.icapps.savera.service.devices.sony.headphones;

import id.icapps.savera.deviceevents.GBDeviceEvent;
import id.icapps.savera.service.AbstractHeadphoneDeviceSupport;
import id.icapps.savera.service.devices.sony.headphones.deviceevents.SonyHeadphonesEnqueueRequestEvent;
import id.icapps.savera.service.serial.GBDeviceIoThread;
import id.icapps.savera.service.serial.GBDeviceProtocol;

public class SonyHeadphonesSupport extends AbstractHeadphoneDeviceSupport {
    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new SonyHeadphonesProtocol(getDevice());
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new SonyHeadphonesIoThread(getDevice(), getContext(), (SonyHeadphonesProtocol) getDeviceProtocol(), SonyHeadphonesSupport.this, getBluetoothAdapter());
    }

    @Override
    public synchronized SonyHeadphonesIoThread getDeviceIOThread() {
        return (SonyHeadphonesIoThread) super.getDeviceIOThread();
    }

    @Override
    public void evaluateGBDeviceEvent(GBDeviceEvent deviceEvent) {
        final SonyHeadphonesProtocol sonyProtocol = (SonyHeadphonesProtocol) getDeviceProtocol();

        if (deviceEvent instanceof SonyHeadphonesEnqueueRequestEvent) {
            final SonyHeadphonesEnqueueRequestEvent enqueueRequestEvent = (SonyHeadphonesEnqueueRequestEvent) deviceEvent;
            sonyProtocol.enqueueRequests(enqueueRequestEvent.getRequests());

            if (sonyProtocol.getPendingAcks() == 0) {
                // There are no pending acks, send one request from the queue
                // TODO: A more elegant way of scheduling these?
                SonyHeadphonesIoThread deviceIOThread = getDeviceIOThread();
                deviceIOThread.write(sonyProtocol.getFromQueue());
            }
        }

        super.evaluateGBDeviceEvent(deviceEvent);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }
}

package id.icapps.savera.service.devices.garmin.messages;


import java.util.Collections;
import java.util.List;

import id.icapps.savera.deviceevents.GBDeviceEvent;
import id.icapps.savera.deviceevents.GBDeviceEventFindPhone;

public class FindMyPhoneCancelMessage extends GFDIMessage {
    public FindMyPhoneCancelMessage(GarminMessage garminMessage) {
        this.garminMessage = garminMessage;

        this.statusMessage = getStatusMessage();
    }

    public static FindMyPhoneCancelMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        return new FindMyPhoneCancelMessage(garminMessage);
    }

    @Override
    public List<GBDeviceEvent> getGBDeviceEvent() {
        final GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
        findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
        return Collections.singletonList(findPhoneEvent);
    }

    @Override
    protected boolean generateOutgoing() {
        return false;
    }
}

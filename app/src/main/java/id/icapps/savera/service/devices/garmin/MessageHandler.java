package id.icapps.savera.service.devices.garmin;

import id.icapps.savera.service.devices.garmin.messages.GFDIMessage;

public interface MessageHandler {
    GFDIMessage handle(GFDIMessage message);
}

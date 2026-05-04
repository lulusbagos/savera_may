package id.icapps.savera.service.devices.garmin.deviceevents;

import java.util.Set;

import id.icapps.savera.deviceevents.GBDeviceEvent;
import id.icapps.savera.devices.vivomovehr.GarminCapability;

public class CapabilitiesDeviceEvent extends GBDeviceEvent {
    public Set<GarminCapability> capabilities;

    public CapabilitiesDeviceEvent(final Set<GarminCapability> capabilities) {
        this.capabilities = capabilities;
    }
}

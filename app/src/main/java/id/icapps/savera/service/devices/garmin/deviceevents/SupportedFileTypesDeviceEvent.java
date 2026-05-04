package id.icapps.savera.service.devices.garmin.deviceevents;

import java.util.List;

import id.icapps.savera.deviceevents.GBDeviceEvent;
import id.icapps.savera.service.devices.garmin.FileType;

public class SupportedFileTypesDeviceEvent extends GBDeviceEvent {

    private final List<FileType> supportedFileTypes;

    public SupportedFileTypesDeviceEvent(List<FileType> fileTypes) {
        this.supportedFileTypes = fileTypes;
    }

    public List<FileType> getSupportedFileTypes() {
        return supportedFileTypes;
    }
}

package id.icapps.savera.service.devices.garmin.deviceevents;

import id.icapps.savera.deviceevents.GBDeviceEvent;
import id.icapps.savera.service.devices.garmin.FileTransferHandler;

public class FileDownloadedDeviceEvent extends GBDeviceEvent {
    public FileTransferHandler.DirectoryEntry directoryEntry;
    public String localPath;
}

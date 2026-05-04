package id.icapps.savera.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import id.icapps.savera.service.devices.garmin.fit.RecordData;
import id.icapps.savera.service.devices.garmin.fit.RecordDefinition;
import id.icapps.savera.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See id.icapps.savera.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitSleepStats extends RecordData {
    public FitSleepStats(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 346) {
            throw new IllegalArgumentException("FitSleepStats expects global messages of " + 346 + ", got " + globalNumber);
        }
    }
}

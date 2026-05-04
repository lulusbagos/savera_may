package id.icapps.savera.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import id.icapps.savera.service.devices.garmin.fit.RecordData;
import id.icapps.savera.service.devices.garmin.fit.RecordDefinition;
import id.icapps.savera.service.devices.garmin.fit.RecordHeader;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionGoalSource.Source;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionGoalType.Type;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See id.icapps.savera.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitGoals extends RecordData {
    public FitGoals(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 15) {
            throw new IllegalArgumentException("FitGoals expects global messages of " + 15 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Type getType() {
        return (Type) getFieldByNumber(4);
    }

    @Nullable
    public Long getTargetValue() {
        return (Long) getFieldByNumber(7);
    }

    @Nullable
    public Source getSource() {
        return (Source) getFieldByNumber(11);
    }
}

package id.icapps.savera.service.devices.garmin.fit.fieldDefinitions;

import id.icapps.savera.service.devices.garmin.fit.FieldDefinition;
import id.icapps.savera.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionHrTimeInZone extends FieldDefinition {
    public FieldDefinitionHrTimeInZone(final int localNumber, final int size, final BaseType baseType, final String name) {
        super(localNumber, size, baseType, name, 1000, 0);
    }
}

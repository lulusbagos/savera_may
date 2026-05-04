package id.icapps.savera.service.devices.garmin.fit.fieldDefinitions;

import id.icapps.savera.service.devices.garmin.fit.FieldDefinition;
import id.icapps.savera.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionArray extends FieldDefinition {
    public FieldDefinitionArray(final int localNumber, final int size, final BaseType baseType, final String name, int scale, int offset) {
        super(localNumber, size, baseType, name, scale, offset);
    }
}

package id.icapps.savera.service.devices.garmin.fit.fieldDefinitions;

import id.icapps.savera.service.devices.garmin.fit.FieldDefinition;
import id.icapps.savera.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionTemperature extends FieldDefinition {

    public FieldDefinitionTemperature(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, -273);
    }

}

package id.icapps.savera.service.devices.garmin.fit;

import id.icapps.savera.service.devices.garmin.fit.baseTypes.BaseType;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionAlarm;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionArray;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionCoordinate;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionDayOfWeek;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionFileType;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionGoalSource;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionGoalType;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionHrTimeInZone;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionHrZoneHighBoundary;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionHrvStatus;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionLanguage;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionMeasurementSystem;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionSleepStage;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionTemperature;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionTimestamp;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherAqi;
import id.icapps.savera.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionWeatherCondition;

public class FieldDefinitionFactory {
    public static FieldDefinition create(int localNumber, int size, FIELD field, BaseType baseType, String name, int scale, int offset) {
        if (null == field) {
            return new FieldDefinition(localNumber, size, baseType, name, scale, offset);
        }
        switch (field) {
            case ALARM:
                return new FieldDefinitionAlarm(localNumber, size, baseType, name);
            case ARRAY:
                return new FieldDefinitionArray(localNumber, size, baseType, name, scale, offset);
            case DAY_OF_WEEK:
                return new FieldDefinitionDayOfWeek(localNumber, size, baseType, name);
            case FILE_TYPE:
                return new FieldDefinitionFileType(localNumber, size, baseType, name);
            case GOAL_SOURCE:
                return new FieldDefinitionGoalSource(localNumber, size, baseType, name);
            case GOAL_TYPE:
                return new FieldDefinitionGoalType(localNumber, size, baseType, name);
            case HRV_STATUS:
                return new FieldDefinitionHrvStatus(localNumber, size, baseType, name);
            case HR_TIME_IN_ZONE:
                return new FieldDefinitionHrTimeInZone(localNumber, size, baseType, name);
            case HR_ZONE_HIGH_BOUNDARY:
                return new FieldDefinitionHrZoneHighBoundary(localNumber, size, baseType, name);
            case MEASUREMENT_SYSTEM:
                return new FieldDefinitionMeasurementSystem(localNumber, size, baseType, name);
            case TEMPERATURE:
                return new FieldDefinitionTemperature(localNumber, size, baseType, name);
            case TIMESTAMP:
                return new FieldDefinitionTimestamp(localNumber, size, baseType, name);
            case WEATHER_CONDITION:
                return new FieldDefinitionWeatherCondition(localNumber, size, baseType, name);
            case LANGUAGE:
                return new FieldDefinitionLanguage(localNumber, size, baseType, name);
            case SLEEP_STAGE:
                return new FieldDefinitionSleepStage(localNumber, size, baseType, name);
            case WEATHER_AQI:
                return new FieldDefinitionWeatherAqi(localNumber, size, baseType, name);
            case COORDINATE:
                return new FieldDefinitionCoordinate(localNumber, size, baseType, name);
            default:
                return new FieldDefinition(localNumber, size, baseType, name);
        }
    }

    public enum FIELD {
        ALARM,
        ARRAY,
        DAY_OF_WEEK,
        FILE_TYPE,
        GOAL_SOURCE,
        GOAL_TYPE,
        HRV_STATUS,
        HR_TIME_IN_ZONE,
        HR_ZONE_HIGH_BOUNDARY,
        MEASUREMENT_SYSTEM,
        TEMPERATURE,
        TIMESTAMP,
        WEATHER_CONDITION,
        LANGUAGE,
        SLEEP_STAGE,
        WEATHER_AQI,
        COORDINATE
    }
}

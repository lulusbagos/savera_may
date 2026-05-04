package id.icapps.savera.activities.workouts.entries;

import id.icapps.savera.activities.workouts.WorkoutValueFormatter;
import id.icapps.savera.model.ActivitySummaryEntries;

public class ActivitySummaryValue {
    private final Object value;
    private final String unit;

    public ActivitySummaryValue(final Object value, final String unit) {
        this.value = value;
        this.unit = unit;
    }

    public ActivitySummaryValue(final String value) {
        this(value, ActivitySummaryEntries.UNIT_STRING);
    }

    public String format(final WorkoutValueFormatter formatter) {
        return formatter.formatValue(value, unit);
    }
}

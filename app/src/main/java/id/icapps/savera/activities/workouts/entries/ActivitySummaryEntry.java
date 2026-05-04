package id.icapps.savera.activities.workouts.entries;

import android.widget.LinearLayout;

import id.icapps.savera.activities.workouts.WorkoutValueFormatter;

public abstract class ActivitySummaryEntry {
    private final String group;

    public ActivitySummaryEntry(final String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    public abstract int getColumnSpan();

    public abstract void populate(final String key,
                                  final LinearLayout linearLayout,
                                  final WorkoutValueFormatter workoutValueFormatter);
}

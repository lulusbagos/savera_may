package id.icapps.savera.activities.dashboard;

import id.icapps.savera.model.Vo2MaxSample;

public interface DashboardVO2MaxWidgetInterface {
    Vo2MaxSample.Type getVO2MaxType();

    String getWidgetKey();
}

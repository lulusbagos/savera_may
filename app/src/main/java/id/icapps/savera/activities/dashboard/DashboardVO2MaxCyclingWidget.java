package id.icapps.savera.activities.dashboard;

import android.os.Bundle;

import id.icapps.savera.R;
import id.icapps.savera.activities.DashboardFragment;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.model.Vo2MaxSample;

public class DashboardVO2MaxCyclingWidget extends AbstractDashboardVO2MaxWidget {

    public DashboardVO2MaxCyclingWidget() {
        super(R.string.vo2max_cycling, "vo2max");
    }

    public static DashboardVO2MaxCyclingWidget newInstance(final DashboardFragment.DashboardData dashboardData) {
        final DashboardVO2MaxCyclingWidget fragment = new DashboardVO2MaxCyclingWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    public Vo2MaxSample.Type getVO2MaxType() {
        return Vo2MaxSample.Type.CYCLING;
    }

    public String getWidgetKey() {
        return "vo2max_cycling";
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsVO2MaxCycling();
    }
}

package id.icapps.savera.activities.dashboard;

import android.os.Bundle;

import id.icapps.savera.R;
import id.icapps.savera.activities.DashboardFragment;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.model.Vo2MaxSample;

public class DashboardVO2MaxRunningWidget extends AbstractDashboardVO2MaxWidget {

    public DashboardVO2MaxRunningWidget() {
        super(R.string.vo2max_running, "vo2max");
    }

    public static DashboardVO2MaxRunningWidget newInstance(final DashboardFragment.DashboardData dashboardData) {
        final DashboardVO2MaxRunningWidget fragment = new DashboardVO2MaxRunningWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    public Vo2MaxSample.Type getVO2MaxType() {
        return Vo2MaxSample.Type.RUNNING;
    }

    public String getWidgetKey() {
        return "vo2max_running";
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsVO2MaxRunning();
    }
}

package id.icapps.savera.activities.dashboard;

import android.os.Bundle;

import id.icapps.savera.R;
import id.icapps.savera.activities.DashboardFragment;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.model.Vo2MaxSample;

public class DashboardVO2MaxAnyWidget extends AbstractDashboardVO2MaxWidget {

    public DashboardVO2MaxAnyWidget() {
        super(R.string.menuitem_vo2_max, "vo2max");
    }

    public static DashboardVO2MaxAnyWidget newInstance(final DashboardFragment.DashboardData dashboardData) {
        final DashboardVO2MaxAnyWidget fragment = new DashboardVO2MaxAnyWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    public Vo2MaxSample.Type getVO2MaxType() {
        return Vo2MaxSample.Type.ANY;
    }

    public String getWidgetKey() {
        return "vo2max";
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsVO2Max();
    }
}

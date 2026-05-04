/*  Copyright (C) 2024 José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package id.icapps.savera.activities.dashboard;

import android.os.Bundle;

import id.icapps.savera.GBApplication;
import id.icapps.savera.R;
import id.icapps.savera.activities.DashboardFragment;
import id.icapps.savera.activities.charts.StressChartFragment;
import id.icapps.savera.activities.dashboard.data.DashboardStressData;
import id.icapps.savera.impl.GBDevice;

public class DashboardStressSimpleWidget extends AbstractGaugeWidget {
    public DashboardStressSimpleWidget() {
        super(R.string.menuitem_stress, "stress");
    }

    public static DashboardStressSimpleWidget newInstance(final DashboardFragment.DashboardData dashboardData) {
        final DashboardStressSimpleWidget fragment = new DashboardStressSimpleWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsStressMeasurement();
    }

    @Override
    protected void populateData(final DashboardFragment.DashboardData dashboardData) {
        dashboardData.computeIfAbsent("stress", () -> DashboardStressData.compute(dashboardData));
    }

    @Override
    protected void draw(final DashboardFragment.DashboardData dashboardData) {
        final DashboardStressData stressData = (DashboardStressData) dashboardData.get("stress");
        if (stressData == null) {
            drawSimpleGauge(0, -1);
            return;
        }

        final int color = StressChartFragment.StressType.fromStress(
                stressData.value,
                stressData.ranges
        ).getColor(GBApplication.getContext());

        final float value = stressData.value / 100f;
        final String valueText = String.valueOf(stressData.value);

        setText(valueText);
        drawSimpleGauge(color, value);
    }
}

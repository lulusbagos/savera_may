/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Dikay900, José Rebelo, Pavel Elagin

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package id.icapps.savera.activities.charts;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import id.icapps.savera.R;
import id.icapps.savera.database.DBHandler;
import id.icapps.savera.devices.DeviceCoordinator;
import id.icapps.savera.devices.TimeSampleProvider;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.model.ActivitySample;
import id.icapps.savera.model.Spo2Sample;
import id.icapps.savera.model.StressSample;


public class ActivitySleepChartFragment extends AbstractActivityChartFragment<DefaultChartsData<LineData>> {
    protected static final Logger LOG = LoggerFactory.getLogger(ActivitySleepChartFragment.class);

    private LineChart mChart;

    private int mSmartAlarmFrom = -1;
    private int mSmartAlarmTo = -1;
    private int mTimestampFrom = -1;
    private int mSmartAlarmGoneOff = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_charts, container, false);

        mChart = (LineChart) rootView.findViewById(R.id.activitysleepchart);

        setupChart();

        return rootView;
    }

    @Override
    public String getTitle() {
        return getString(R.string.activity_sleepchart_activity_and_sleep);
    }

    private void setupChart() {
        mChart.setBackgroundColor(BACKGROUND_COLOR);
        mChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        configureBarLineChartDefaults(mChart);


        XAxis x = mChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);

        YAxis y = mChart.getAxisLeft();
        y.setDrawGridLines(false);
//        y.setDrawLabels(false);
        // TODO: make fixed max value optional
        y.setAxisMaximum(1f);
        y.setAxisMinimum(0);
        y.setDrawTopYLabelEntry(false);
        y.setTextColor(CHART_TEXT_COLOR);

//        y.setLabelCount(5);
        y.setEnabled(true);

        YAxis yAxisRight = mChart.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(true);
        yAxisRight.setDrawTopYLabelEntry(true);
        yAxisRight.setTextColor(CHART_TEXT_COLOR);
        yAxisRight.setAxisMaximum(105f);
        yAxisRight.setAxisMinimum(0f);

        // refresh immediately instead of use refreshIfVisible(), for perceived performance
        refresh();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(ChartsHost.REFRESH)) {
            // TODO: use LimitLines to visualize smart alarms?
            mSmartAlarmFrom = intent.getIntExtra("smartalarm_from", -1);
            mSmartAlarmTo = intent.getIntExtra("smartalarm_to", -1);
            mTimestampFrom = intent.getIntExtra("recording_base_timestamp", -1);
            mSmartAlarmGoneOff = intent.getIntExtra("alarm_gone_off", -1);
            refresh();
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    protected DefaultChartsData<LineData> refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        List<? extends ActivitySample> samples = getSamples(db, device);
        DefaultChartsData<LineData> chartData = refresh(device, samples);

        if (!samples.isEmpty()) {
            try {
                DeviceCoordinator coordinator = device.getDeviceCoordinator();
                int tsOffset = samples.get(0).getTimestamp();
                int tsFrom = tsOffset;
                int tsTo = samples.get(samples.size() - 1).getTimestamp();

                List<Entry> spo2Entries = new ArrayList<>();
                List<Entry> stressEntries = new ArrayList<>();

                if (coordinator.supportsSpo2(device)) {
                    TimeSampleProvider<? extends Spo2Sample> spo2Provider = coordinator.getSpo2SampleProvider(device, db.getDaoSession());
                    if (spo2Provider != null) {
                        List<? extends Spo2Sample> spo2Samples = spo2Provider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);
                        for (Spo2Sample spo2Sample : spo2Samples) {
                            int x = (int) (spo2Sample.getTimestamp() / 1000) - tsOffset;
                            int spo2 = spo2Sample.getSpo2();
                            if (spo2 > 0) {
                                spo2Entries.add(new Entry(x, spo2));
                            }
                        }
                    }
                }
                if (coordinator.supportsStressMeasurement()) {
                    TimeSampleProvider<? extends StressSample> stressProvider = coordinator.getStressSampleProvider(device, db.getDaoSession());
                    if (stressProvider != null) {
                        List<? extends StressSample> stressSamples = stressProvider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);
                        for (StressSample stressSample : stressSamples) {
                            int x = (int) (stressSample.getTimestamp() / 1000) - tsOffset;
                            int stress = stressSample.getStress();
                            if (stress > 0) {
                                stressEntries.add(new Entry(x, stress));
                            }
                        }
                    }
                }

                if (spo2Entries.isEmpty()) {
                    addIntEntriesFromActivitySamples(samples, tsOffset, spo2Entries, "getSpo2");
                }
                if (stressEntries.isEmpty()) {
                    addIntEntriesFromActivitySamples(samples, tsOffset, stressEntries, "getStress");
                }

                if (!spo2Entries.isEmpty()) {
                    chartData.getData().addDataSet(createOverlayDataSet(spo2Entries, "SpO2 (%)", Color.GREEN));
                }
                if (!stressEntries.isEmpty()) {
                    chartData.getData().addDataSet(createOverlayDataSet(stressEntries, "Stress", Color.MAGENTA));
                }
            } catch (Exception e) {
                LOG.warn("Could not load SpO2/stress data for sleep chart", e);
            }
        }

        return chartData;
    }

    private LineDataSet createOverlayDataSet(List<Entry> entries, String label, int color) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setLineWidth(2f);
        dataSet.setColor(color);
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        dataSet.setCubicIntensity(0.1f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setValueTextColor(CHART_TEXT_COLOR);
        dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        return dataSet;
    }

    private void addIntEntriesFromActivitySamples(List<? extends ActivitySample> samples, int tsOffset, List<Entry> entries, String methodName) {
        for (ActivitySample sample : samples) {
            try {
                java.lang.reflect.Method method = sample.getClass().getMethod(methodName);
                Object value = method.invoke(sample);
                if (value instanceof Number) {
                    int intValue = ((Number) value).intValue();
                    if (intValue > 0) {
                        entries.add(new Entry(sample.getTimestamp() - tsOffset, intValue));
                    }
                }
            } catch (Exception ignored) {
                // Some activity sample classes do not expose SpO2/stress fields.
            }
        }
    }

    @Override
    protected void updateChartsnUIThread(DefaultChartsData<LineData> dcd) {
        mChart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
        mChart.setData(null); // workaround for https://github.com/PhilJay/MPAndroidChart/issues/2317
        mChart.getXAxis().setValueFormatter(dcd.getXValueFormatter());
        mChart.setData((LineData) dcd.getData());
    }

    @Override
    protected void renderCharts() {
        mChart.animateX(ANIM_TIME, Easing.EaseInOutQuart);
//        mChart.invalidate();
    }

    @Override
    protected void setupLegend(Chart<?> chart) {
        List<LegendEntry> legendEntries = new ArrayList<>(6);

        LegendEntry spo2Entry = new LegendEntry();
        spo2Entry.label = "SpO2 (%)";
        spo2Entry.formColor = Color.GREEN;
        legendEntries.add(spo2Entry);

        LegendEntry stressEntry = new LegendEntry();
        stressEntry.label = "Stress";
        stressEntry.formColor = Color.MAGENTA;
        legendEntries.add(stressEntry);

        LegendEntry lightSleepEntry = new LegendEntry();
        lightSleepEntry.label = akLightSleep.label;
        lightSleepEntry.formColor = akLightSleep.color;
        legendEntries.add(lightSleepEntry);

        LegendEntry deepSleepEntry = new LegendEntry();
        deepSleepEntry.label = akDeepSleep.label;
        deepSleepEntry.formColor = akDeepSleep.color;
        legendEntries.add(deepSleepEntry);

        if (supportsRemSleep(getChartsHost().getDevice())) {
            LegendEntry remSleepEntry = new LegendEntry();
            remSleepEntry.label = akRemSleep.label;
            remSleepEntry.formColor = akRemSleep.color;
            legendEntries.add(remSleepEntry);
        }

        LegendEntry notWornEntry = new LegendEntry();
        notWornEntry.label = akNotWorn.label;
        notWornEntry.formColor = akNotWorn.color;
        legendEntries.add(notWornEntry);

        if (supportsHeartrate(getChartsHost().getDevice())) {
            LegendEntry hrEntry = new LegendEntry();
            hrEntry.label = HEARTRATE_LABEL;
            hrEntry.formColor = HEARTRATE_COLOR;
            legendEntries.add(hrEntry);
        }
        chart.getLegend().setCustom(legendEntries);
        chart.getLegend().setWordWrapEnabled(true);
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
    }

    @Override
    protected List<? extends ActivitySample> getSamples(DBHandler db, GBDevice device, int tsFrom, int tsTo) {
        return getAllSamples(db, device, tsFrom, tsTo);
    }

    @Override
    protected LineDataSet createDataSet(List<Entry> values, Integer color, String label) {
        if ("Activity".equals(label)) {
            // Hide activity band so sleep stages remain visible.
            LineDataSet hidden = new LineDataSet(new ArrayList<>(), "");
            hidden.setVisible(false);
            hidden.setDrawValues(false);
            return hidden;
        }
        return super.createDataSet(values, color, label);
    }
}

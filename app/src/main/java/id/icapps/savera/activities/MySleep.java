package id.icapps.savera.activities;

import static id.icapps.savera.model.DailyTotals.getTotalsSleepForActivityAmounts;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import id.icapps.savera.GBApplication;
import id.icapps.savera.R;
import id.icapps.savera.activities.charts.ActivityAnalysis;
import id.icapps.savera.activities.charts.ChartsData;
import id.icapps.savera.activities.charts.DefaultChartsData;
import id.icapps.savera.activities.charts.SampleXLabelFormatter;
import id.icapps.savera.activities.charts.StepAnalysis;
import id.icapps.savera.activities.charts.TimestampTranslation;
import id.icapps.savera.activities.charts.TimestampValueFormatter;
import id.icapps.savera.activities.dashboard.AbstractDashboardWidget;
import id.icapps.savera.activities.dashboard.DashboardCalendarActivity;
import id.icapps.savera.database.DBHandler;
import id.icapps.savera.devices.DeviceCoordinator;
import id.icapps.savera.devices.SampleProvider;
import id.icapps.savera.devices.TimeSampleProvider;
import id.icapps.savera.devices.xiaomi.XiaomiDailySummarySampleProvider;
import id.icapps.savera.entities.AbstractActivitySample;
import id.icapps.savera.entities.BaseActivitySummary;
import id.icapps.savera.entities.BaseActivitySummaryDao;
import id.icapps.savera.entities.XiaomiDailySummarySample;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.model.ActivityAmounts;
import id.icapps.savera.model.ActivityKind;
import id.icapps.savera.model.ActivitySample;
import id.icapps.savera.model.ActivitySession;
import id.icapps.savera.model.ActivityUser;
import id.icapps.savera.model.DailyTotals;
import id.icapps.savera.model.Spo2Sample;
import id.icapps.savera.model.StressSample;
import id.icapps.savera.util.DateTimeUtils;
import id.icapps.savera.util.Prefs;

public class MySleep extends Fragment {
    private static final Logger LOG = LoggerFactory.getLogger(MySleep.class);
    private static Calendar now = GregorianCalendar.getInstance();
    private TextView textDate, arrowLeft, arrowRight;
    private ImageView imgClock1, imgClock2, imgArrow1, imgArrow2;
    private LineChart activityChart;
    private final Map<String, AbstractDashboardWidget> widgetMap = new HashMap<>();
    private MyData myData1 = new MyData();
    private MyData myData2 = new MyData();
    private boolean isConfigChanged = false;
    private boolean mode_24h;

    public static final String ACTION_CONFIG_CHANGE = "id.icapps.savera.activities.dashboardfragment.action.config_change";
    public static final float Y_VALUE_DEEP_SLEEP = 0.01f;

    private ActivityResultLauncher<Intent> calendarLauncher;
    private final ActivityResultCallback<ActivityResult> calendarCallback = result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            long timeMillis = result.getData().getLongExtra(DashboardCalendarActivity.EXTRA_TIMESTAMP, 0);
            if (timeMillis != 0) {
                now.setTimeInMillis(timeMillis);
                fullRefresh();
            }
        }
    };

    final int color_unknown = Color.argb(50, 128, 128, 128);
    final int color_not_worn = Color.argb(90, 169, 149, 201);
    final int color_worn = Color.argb(90, 95, 79, 162);
    final int color_activity = Color.rgb(244, 120, 30);
    final int color_exercise = Color.rgb(255, 128, 0);
    final int color_light_sleep = Color.rgb(139, 65, 218);
    final int color_deep_sleep = Color.rgb(98, 64, 215);
    final int color_rem_sleep = Color.rgb(77, 43, 196);
    final int color_awake_sleep = Color.rgb(244, 117, 117);
    final int color_text = Color.rgb(0x00, 0x00, 0x00);
    final int color_stripe = Color.argb(50, 0x00, 0x00, 0x00);

    protected static final class ActivityConfig {
        public final ActivityKind type;
        public final String label;
        public final Integer color;

        public ActivityConfig(ActivityKind kind, String label, Integer color) {
            this.type = kind;
            this.label = label;
            this.color = color;
        }
    }

    protected ActivityConfig akActivity;
    protected ActivityConfig akLightSleep;
    protected ActivityConfig akDeepSleep;
    protected ActivityConfig akRemSleep;
    protected ActivityConfig akAwakeSleep;
    protected ActivityConfig akNotWorn;

    protected int BACKGROUND_COLOR;
    protected int DESCRIPTION_COLOR;
    protected int CHART_TEXT_COLOR;
    protected int LEGEND_TEXT_COLOR;
    protected int HEARTRATE_COLOR;
    protected int HEARTRATE_FILL_COLOR;
    protected int AK_ACTIVITY_COLOR;
    protected int AK_DEEP_SLEEP_COLOR;
    protected int AK_REM_SLEEP_COLOR;
    protected int AK_AWAKE_SLEEP_COLOR;
    protected int AK_LIGHT_SLEEP_COLOR;
    protected int AK_NOT_WORN_COLOR;

    protected String HEARTRATE_LABEL;
    protected String HEARTRATE_AVERAGE_LABEL;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case GBApplication.ACTION_NEW_DATA:
                    final GBDevice dev = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    if (dev != null) {
                        if (myData1.showAllDevices || myData1.showDeviceList.contains(dev.getAddress())) {
                            refresh();
                        }
                    }
                    break;
                case ACTION_CONFIG_CHANGE:
                    isConfigChanged = true;
                    break;
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.my_sleep, container, false);

        Prefs prefs = GBApplication.getPrefs();
        mode_24h = prefs.getBoolean("dashboard_widget_today_24h", true);

        calendarLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                calendarCallback
        );

        // Initialize legend
        TextView legend = view.findViewById(R.id.dashboard_piechart_legend);
        SpannableString l_not_worn = new SpannableString("■ " + "Tidak dipakai");
        l_not_worn.setSpan(new ForegroundColorSpan(color_not_worn), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_worn = new SpannableString("■ " + "Dipakai");
        l_worn.setSpan(new ForegroundColorSpan(color_worn), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_activity = new SpannableString("■ " + getString(R.string.activity_type_activity));
        l_activity.setSpan(new ForegroundColorSpan(color_activity), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_exercise = new SpannableString("■ " + getString(R.string.activity_type_exercise));
        l_exercise.setSpan(new ForegroundColorSpan(color_exercise), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_deep_sleep = new SpannableString("■ " + getString(R.string.activity_type_deep_sleep));
        l_deep_sleep.setSpan(new ForegroundColorSpan(color_deep_sleep), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_light_sleep = new SpannableString("■ " + getString(R.string.activity_type_light_sleep));
        l_light_sleep.setSpan(new ForegroundColorSpan(color_light_sleep), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_rem_sleep = new SpannableString("■ " + getString(R.string.activity_type_rem_sleep));
        l_rem_sleep.setSpan(new ForegroundColorSpan(color_rem_sleep), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString l_awake_sleep = new SpannableString("\u25A0 " + getString(R.string.abstract_chart_fragment_kind_awake_sleep));
        l_awake_sleep.setSpan(new ForegroundColorSpan(color_awake_sleep), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableStringBuilder legendBuilder = new SpannableStringBuilder();
        legend.setText(legendBuilder.append(l_worn).append(" ").append(l_not_worn).append(" ").append(l_activity).append("\n").append(l_light_sleep).append(" ").append(l_deep_sleep).append(" ").append(l_rem_sleep).append(" ").append(l_awake_sleep));

        imgClock1 = view.findViewById(R.id.sleepChart1);
        imgClock2 = view.findViewById(R.id.sleepChart2);
        imgArrow1 = view.findViewById(R.id.imgArrow1);
        imgArrow2 = view.findViewById(R.id.imgArrow2);

        textDate = view.findViewById(R.id.dashboard_date);
        activityChart = view.findViewById(R.id.sleepchart);

        assert getActivity() != null;
        now = ((HomeActivity) getActivity()).getDateNow();

        arrowLeft = view.findViewById(R.id.arrow_left);
        arrowLeft.setOnClickListener(v -> {
            now.add(Calendar.DAY_OF_MONTH, -1);
            refresh();
        });
        arrowRight = view.findViewById(R.id.arrow_right);
        arrowRight.setOnClickListener(v -> {
            Calendar today = GregorianCalendar.getInstance();
            if (!DateTimeUtils.isSameDay(today, now)) {
                now.add(Calendar.DAY_OF_MONTH, 1);
                refresh();
            }
        });

        TypedValue runningColor = new TypedValue();
        BACKGROUND_COLOR = GBApplication.getBackgroundColor(getContext());
        LEGEND_TEXT_COLOR = DESCRIPTION_COLOR = GBApplication.getTextColor(getContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(getContext());
        HEARTRATE_COLOR = ContextCompat.getColor(getContext(), R.color.chart_heartrate);
        HEARTRATE_FILL_COLOR = ContextCompat.getColor(getContext(), R.color.chart_heartrate_fill);

        getContext().getTheme().resolveAttribute(R.attr.chart_activity, runningColor, true);
        AK_ACTIVITY_COLOR = runningColor.data;
        getContext().getTheme().resolveAttribute(R.attr.chart_deep_sleep, runningColor, true);
        AK_DEEP_SLEEP_COLOR = runningColor.data;
        getContext().getTheme().resolveAttribute(R.attr.chart_light_sleep, runningColor, true);
        AK_LIGHT_SLEEP_COLOR = runningColor.data;
        getContext().getTheme().resolveAttribute(R.attr.chart_rem_sleep, runningColor, true);
        AK_REM_SLEEP_COLOR = runningColor.data;
        getContext().getTheme().resolveAttribute(R.attr.chart_awake_sleep, runningColor, true);
        AK_AWAKE_SLEEP_COLOR = runningColor.data;
        getContext().getTheme().resolveAttribute(R.attr.chart_not_worn, runningColor, true);
        AK_NOT_WORN_COLOR = runningColor.data;

        HEARTRATE_LABEL = getContext().getString(R.string.charts_legend_heartrate);
        HEARTRATE_AVERAGE_LABEL = getContext().getString(R.string.charts_legend_heartrate_average);

        akActivity = new ActivityConfig(ActivityKind.ACTIVITY, getString(R.string.abstract_chart_fragment_kind_activity), AK_ACTIVITY_COLOR);
        akLightSleep = new ActivityConfig(ActivityKind.LIGHT_SLEEP, getString(R.string.abstract_chart_fragment_kind_light_sleep), AK_LIGHT_SLEEP_COLOR);
        akDeepSleep = new ActivityConfig(ActivityKind.DEEP_SLEEP, getString(R.string.abstract_chart_fragment_kind_deep_sleep), AK_DEEP_SLEEP_COLOR);
        akRemSleep = new ActivityConfig(ActivityKind.REM_SLEEP, getString(R.string.abstract_chart_fragment_kind_rem_sleep), AK_REM_SLEEP_COLOR);
        akAwakeSleep = new ActivityConfig(ActivityKind.AWAKE_SLEEP, getString(R.string.abstract_chart_fragment_kind_awake_sleep), AK_AWAKE_SLEEP_COLOR);
        akNotWorn = new ActivityConfig(ActivityKind.NOT_WORN, getString(R.string.abstract_chart_fragment_kind_not_worn), AK_NOT_WORN_COLOR);

        setupActivityChart();

        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                assert getActivity() != null;
                ((HomeActivity) getActivity()).changeViewPagerPostition(0);
            }
        });

        ImageButton btnCalendar = view.findViewById(R.id.btnCalendar);
        btnCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(requireActivity(), DashboardCalendarActivity.class);
                intent.putExtra(DashboardCalendarActivity.EXTRA_TIMESTAMP, now.getTimeInMillis());
                calendarLauncher.launch(intent);
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey("dashboard_data") && myData1.isEmpty()) {
            myData1 = (MyData) savedInstanceState.getSerializable("dashboard_data");
        } else if (myData1.isEmpty()) {
            reloadPreferences();
        }

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        filterLocal.addAction(GBApplication.ACTION_NEW_DATA);
        filterLocal.addAction(ACTION_CONFIG_CHANGE);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mReceiver, filterLocal);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isConfigChanged) {
            isConfigChanged = false;
            fullRefresh();
        } else if (myData1.isEmpty() || !widgetMap.containsKey("today")) {
            refresh();
        }
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("dashboard_data", myData1);
    }

    private void fullRefresh() {
        widgetMap.clear();
        refresh();
    }

    private void refresh() {
        now.set(Calendar.HOUR_OF_DAY, 23);
        now.set(Calendar.MINUTE, 59);
        now.set(Calendar.SECOND, 59);
        myData1.clear();
        myData2.clear();
        reloadPreferences();
        draw();
        assert getActivity() != null;
        ((HomeActivity) getActivity()).setDateNow(now);
        refreshChart();
    }

    private void reloadPreferences() {
        Prefs prefs = GBApplication.getPrefs();

        myData1.showAllDevices = prefs.getBoolean("dashboard_devices_all", true);
        myData1.showDeviceList = prefs.getStringSet("dashboard_devices_multiselect", new HashSet<>());
        myData1.hrIntervalSecs = prefs.getInt("dashboard_widget_today_hr_interval", 1) * 60;
        myData1.timeTo = (int) (now.getTimeInMillis() / 1000);
        myData1.timeFrom = DateTimeUtils.shiftDays(myData1.timeTo, -1);

        myData2.showAllDevices = myData1.showAllDevices;
        myData2.showDeviceList = myData1.showDeviceList;
        myData2.hrIntervalSecs = myData1.hrIntervalSecs;
        myData2.timeTo = myData1.timeTo - (24 * 3600);
        myData2.timeFrom = myData1.timeFrom - (24 * 3600);

        if (imgClock1 != null) {
            ClockChart clock1 = new ClockChart(imgClock1, myData1);
            clock1.execute();
        }

        if (imgClock2 != null) {
            ClockChart clock2 = new ClockChart(imgClock2, myData2);
            clock2.execute();
        }
    }

    @SuppressLint("SetTextI18n")
    private void draw() {
        Calendar today = GregorianCalendar.getInstance();
        int hour = today.get(Calendar.HOUR_OF_DAY);

        textDate.setText(new SimpleDateFormat("E, dd MMM yyyy", Locale.getDefault()).format(now.getTime()));
        if (DateTimeUtils.isSameDay(today, now)) {
            arrowRight.setAlpha(0.5f);
        } else {
            arrowRight.setAlpha(1);
        }

        imgArrow1.animate().rotation(hour * 15).setDuration(5000).start();
        if (hour < 12) {
            imgArrow2.animate().rotation(6 * 15).setDuration(5000).start();
        } else {
            imgArrow2.animate().rotation(18 * 15).setDuration(5000).start();
        }
    }

    /**
     * This class serves as a data collection object for all data points used by the various
     * dashboard widgets. Since retrieving this data can be costly, this class makes sure it will
     * only be done once. It will be passed to every widget, making sure they have the necessary
     * data available.
     */
    private static class MyData implements Serializable {
        private boolean showAllDevices;
        private Set<String> showDeviceList;
        private int hrIntervalSecs;
        private int timeFrom;
        private int timeTo;
        private int sleepFrom;
        private int sleepTo;
        private String sleepType;
        private final List<GeneralizedActivity> generalizedActivities = Collections.synchronizedList(new ArrayList<>());
        private int stepsTotal;
        private float stepsGoalFactor;
        private long sleepTotalMinutes;
        private float sleepGoalFactor;
        private float distanceTotalMeters;
        private float distanceGoalFactor;
        private long activeMinutesTotal;
        private float activeMinutesGoalFactor;
        private long lightSleepTotalMinutes;
        private long deepSleepTotalMinutes;
        private long remSleepTotalMinutes;
        private long awakeSleepTotalMinutes;
        private long sleepToday;
        private long sleepYesterday;
        private long sleepRest;
        private int heartRate;
        private int calories;
        private int stress;
        private int bloodOxygen;
        private int bloodPressure;
        private final Map<String, Serializable> genericData = new ConcurrentHashMap<>();

        private void clear() {
            stepsTotal = 0;
            stepsGoalFactor = 0;
            sleepTotalMinutes = 0;
            sleepGoalFactor = 0;
            sleepType = "night";
            distanceTotalMeters = 0;
            distanceGoalFactor = 0;
            activeMinutesTotal = 0;
            activeMinutesGoalFactor = 0;
            lightSleepTotalMinutes = 0;
            deepSleepTotalMinutes = 0;
            remSleepTotalMinutes = 0;
            awakeSleepTotalMinutes = 0;
            sleepToday = 0;
            sleepYesterday = 0;
            sleepRest = 0;
            heartRate = 0;
            calories = 0;
            stress = 0;
            bloodOxygen = 0;
            bloodPressure = 0;
            generalizedActivities.clear();
            genericData.clear();
        }

        private boolean isEmpty() {
            return (stepsTotal == 0 &&
                    stepsGoalFactor == 0 &&
                    sleepTotalMinutes == 0 &&
                    sleepGoalFactor == 0 &&
                    distanceTotalMeters == 0 &&
                    distanceGoalFactor == 0 &&
                    activeMinutesTotal == 0 &&
                    activeMinutesGoalFactor == 0 &&
                    genericData.isEmpty() &&
                    generalizedActivities.isEmpty());
        }

        private DailyTotals getDailyTotals(GBDevice device, DBHandler db) {
            Calendar today = GregorianCalendar.getInstance();
            today.setTimeInMillis(timeTo * 1000L);
            return DailyTotals.getDailyTotalsForDevice(device, today, db);
        }

        private int getStepsTotal() {
            List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
            int totalSteps = 0;
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                int idx = 0;
                for (GBDevice dev : devices) {
                    if (idx > 0) continue;
                    idx++;
                    if ((showAllDevices || showDeviceList.contains(dev.getAddress())) && dev.getDeviceCoordinator().supportsActivityTracking()) {
                        totalSteps += (int) getDailyTotals(dev, dbHandler).getSteps();
                    }
                }
            } catch (Exception e) {
                LOG.warn("Could not calculate total amount of steps: ", e);
            }
            return totalSteps;
        }

        private float getStepsGoalFactor() {
            ActivityUser activityUser = new ActivityUser();
            float stepsGoal = activityUser.getStepsGoal();
            float goalFactor = getStepsTotal() / stepsGoal;
            if (goalFactor > 1) goalFactor = 1;

            return goalFactor;
        }

        private long[] getSleep(GBDevice device, DBHandler db) {
            SampleProvider<? extends ActivitySample> provider = getProvider(db, device);
            ActivityAnalysis analysis = new ActivityAnalysis();

            Calendar today = GregorianCalendar.getInstance();
            int hour = today.get(Calendar.HOUR_OF_DAY);
            today.setTimeInMillis(timeTo * 1000L);
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);

            long[] totalT = new long[]{0, 0, 0, 0};
            long[] totalY = new long[]{0, 0, 0, 0};
            long[] totalR = new long[]{0, 0, 0, 0};

            if (hour < 12) {
                today.add(Calendar.HOUR, -6);

                int sleep1 = (int) (today.getTimeInMillis() / 1000);
                int sleep2 = sleep1 + (6 * 3600);
                int sleep3 = sleep1 + (12 * 3600);
                int sleep4 = sleep1 - (12 * 3600);

                ActivityAmounts amountToday = analysis.calculateActivityAmounts(provider.getAllActivitySamples(sleep2, sleep3));
                totalT = getTotalsSleepForActivityAmounts(amountToday);
                ActivityAmounts amountYesterday = analysis.calculateActivityAmounts(provider.getAllActivitySamples(sleep1, sleep2));
                totalY = getTotalsSleepForActivityAmounts(amountYesterday);
                List<? extends ActivitySample> restSamples = provider.getAllActivitySamples(sleep4, sleep1);
                totalR = resolveRestSleepTotals(analysis, restSamples);

                sleepType = "night";
            } else {
                today.add(Calendar.HOUR, 6);

                int sleep1 = (int) (today.getTimeInMillis() / 1000);
                int sleep4 = sleep1 - (12 * 3600);

                List<? extends ActivitySample> restSamples = provider.getAllActivitySamples(sleep4, sleep1);
                totalR = resolveRestSleepTotals(analysis, restSamples);

                sleepType = "day";
            }

            sleepFrom = (int) (today.getTimeInMillis() / 1000);
            sleepTo = sleepFrom + (12 * 3600);

            ActivityAmounts amountSleep = analysis.calculateActivityAmounts(provider.getAllActivitySamples(sleepFrom, sleepTo));
            long[] totalS = getTotalsSleepForActivityAmounts(amountSleep);

            if (Objects.equals(sleepType, "day")) {
                totalT[0] = totalS[0];
                totalT[1] = totalS[1];
                totalT[2] = totalS[2];
            }

            return new long[]{totalS[0], totalS[1], totalS[2], totalS[3], (totalT[0] + totalT[1] + totalT[2]), (totalY[0] + totalY[1] + totalY[2]), (totalR[0] + totalR[1] + totalR[2])};
        }

        private long[] resolveRestSleepTotals(ActivityAnalysis analysis, List<? extends ActivitySample> restSamples) {
            if (restSamples == null || restSamples.isEmpty()) {
                return new long[]{0, 0, 0, 0};
            }

            ActivityAmounts amountRest = analysis.calculateActivityAmounts(restSamples);
            long[] totalR = getTotalsSleepForActivityAmounts(amountRest);
            long restSleepMinutes = totalR[0] + totalR[1] + totalR[2];

            // Rule: no sleep data => don't count, < 1 hour => keep exact wearable value, >= 1 hour => cap to 1 hour.
            if (restSleepMinutes <= 0) {
                totalR[0] = 0;
                totalR[1] = 0;
                totalR[2] = 0;
                return totalR;
            }

            if (restSleepMinutes < 60) {
                return totalR;
            }

            totalR[0] = 60;
            totalR[1] = 0;
            totalR[2] = 0;
            return totalR;
        }

        private long getSleepMinutesTotal() {
            List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
            sleepTotalMinutes = 0;
            lightSleepTotalMinutes = 0;
            deepSleepTotalMinutes = 0;
            remSleepTotalMinutes = 0;
            awakeSleepTotalMinutes = 0;
            sleepToday = 0;
            sleepYesterday = 0;
            sleepRest = 0;
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                for (GBDevice dev : devices) {
                    if ((showAllDevices || showDeviceList.contains(dev.getAddress())) && dev.getDeviceCoordinator().supportsActivityTracking()) {
                        long[] sleep = getSleep(dev, dbHandler);
                        sleepTotalMinutes += (sleep[0] + sleep[1] + sleep[2] + sleep[6]);
                        lightSleepTotalMinutes += sleep[0];
                        deepSleepTotalMinutes += sleep[1];
                        remSleepTotalMinutes += sleep[2];
                        awakeSleepTotalMinutes += sleep[3];
                        sleepToday += sleep[4];
                        sleepYesterday += sleep[5];
                        sleepRest += sleep[6];
                    }
                }
            } catch (Exception e) {
                LOG.warn("Could not calculate total amount of sleep: ", e);
            }
            return sleepTotalMinutes;
        }

        private float getSleepMinutesGoalFactor() {
            ActivityUser activityUser = new ActivityUser();
            int sleepMinutesGoal = activityUser.getSleepDurationGoal() * 60;
            float goalFactor = (float) getSleepMinutesTotal() / sleepMinutesGoal;
            if (goalFactor > 1) goalFactor = 1;

            return goalFactor;
        }

        private float getDistanceTotal() {
            ActivityUser activityUser = new ActivityUser();
            int stepLength = activityUser.getStepLengthCm();

            List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
            long totalDistanceCm = 0;
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                for (GBDevice dev : devices) {
                    if ((showAllDevices || showDeviceList.contains(dev.getAddress())) && dev.getDeviceCoordinator().supportsActivityTracking()) {
                        final DailyTotals dailyTotals = getDailyTotals(dev, dbHandler);
                        if (dailyTotals.getSteps() > 0 && dailyTotals.getDistance() > 0) {
                            totalDistanceCm += dailyTotals.getDistance();
                        } else {
                            totalDistanceCm += dailyTotals.getSteps() * stepLength;
                        }
                    }
                }
            } catch (Exception e) {
                LOG.warn("Could not calculate total distance: ", e);
            }
            return totalDistanceCm * 0.01f;
        }

        private float getDistanceGoalFactor() {
            ActivityUser activityUser = new ActivityUser();
            int distanceGoal = activityUser.getDistanceGoalMeters();
            float goalFactor = getDistanceTotal() / distanceGoal;
            if (goalFactor > 1) goalFactor = 1;

            return goalFactor;
        }

        private long getActiveMinutesTotal() {
            List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
            long totalActiveMinutes = 0;
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                for (GBDevice dev : devices) {
                    if ((showAllDevices || showDeviceList.contains(dev.getAddress())) && dev.getDeviceCoordinator().supportsActivityTracking()) {
                        totalActiveMinutes += getActiveMinutes(dev, dbHandler);
                    }
                }
            } catch (Exception e) {
                LOG.warn("Could not calculate total amount of activity: ", e);
            }
            return totalActiveMinutes;
        }

        private float getActiveMinutesGoalFactor() {
            ActivityUser activityUser = new ActivityUser();
            int activeTimeGoal = activityUser.getActiveTimeGoalMinutes();
            float goalFactor = (float) getActiveMinutesTotal() / activeTimeGoal;
            if (goalFactor > 1) goalFactor = 1;

            return goalFactor;
        }

        private long getActiveMinutes(GBDevice gbDevice, DBHandler db) {
            ActivitySession stepSessionsSummary = new ActivitySession();
            List<ActivitySession> stepSessions;
            List<? extends ActivitySample> activitySamples = getAllSamples(db, gbDevice);
            StepAnalysis stepAnalysis = new StepAnalysis();

            boolean isEmptySummary = false;
            if (activitySamples != null) {
                stepSessions = stepAnalysis.calculateStepSessions(activitySamples);
                if (stepSessions.toArray().length == 0) {
                    isEmptySummary = true;
                }
                stepSessionsSummary = stepAnalysis.calculateSummary(stepSessions, isEmptySummary);
            }
            long duration = stepSessionsSummary.getEndTime().getTime() - stepSessionsSummary.getStartTime().getTime();
            return duration / 1000 / 60;
        }

        private int getHeartRate() {
            List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
            heartRate = 0;
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                for (GBDevice dev : devices) {
                    if ((showAllDevices || showDeviceList.contains(dev.getAddress())) && dev.getDeviceCoordinator().supportsActivityTracking()) {
                        List<? extends ActivitySample> activitySamples = getAllSamples(dbHandler, dev);
                        int hr = 0;
                        for (ActivitySample row : activitySamples) {
                            if (row.getHeartRate() > 0 && row.getHeartRate() < 255)
                                hr = row.getHeartRate();
                        }
                        heartRate += hr;
                    }
                }
            } catch (Exception e) {
                LOG.warn("Could not calculate total heart rate: ", e);
            }
            return heartRate;
        }

        private int getCalories() {

            return calories;
        }

        private String getStress() {
            List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
            stress = 0;
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                for (GBDevice dev : devices) {
                    if ((showAllDevices || showDeviceList.contains(dev.getAddress())) && dev.getDeviceCoordinator().supportsActivityTracking()) {
                        DeviceCoordinator coordinator = dev.getDeviceCoordinator();
                        if (coordinator.supportsStressMeasurement()) {
                            TimeSampleProvider<? extends StressSample> stressProvider = coordinator.getStressSampleProvider(dev, dbHandler.getDaoSession());
                            List<? extends StressSample> stressSample = stressProvider.getAllSamples(timeFrom * 1000L, timeTo * 1000L);
                            int strs = 0;
                            for (StressSample row : stressSample) {
                                if (row.getStress() > 0) strs = row.getStress();
                            }
                            stress += strs;
                        }
                    }
                }
            } catch (Exception e) {
                LOG.warn("Could not calculate total stress: ", e);
            }
            if (stress == 0) return "-";
            return stress + " " + getStressType(stress, new int[]{1, 30, 60, 80});
        }

        private String getStressType(final int stress, final int[] stressRanges) {
            if (stress < stressRanges[0]) {
                return "";
            } else if (stress < stressRanges[1]) {
                return "(relaxed)";
            } else if (stress < stressRanges[2]) {
                return "(normal)";
            } else if (stress < stressRanges[3]) {
                return "(medium)";
            } else {
                return "(high)";
            }
        }

        private int getBloodOxygen() {
            List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
            bloodOxygen = 0;
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                for (GBDevice dev : devices) {
                    if ((showAllDevices || showDeviceList.contains(dev.getAddress())) && dev.getDeviceCoordinator().supportsActivityTracking()) {
                        DeviceCoordinator coordinator = dev.getDeviceCoordinator();
                        if (coordinator.supportsSpo2(dev)) {
                            TimeSampleProvider<? extends Spo2Sample> spo2Provider = coordinator.getSpo2SampleProvider(dev, dbHandler.getDaoSession());
                            List<? extends Spo2Sample> spo2Sample = spo2Provider.getAllSamples(timeFrom * 1000L, timeTo * 1000L);
                            int spo2 = 0;
                            for (Spo2Sample row : spo2Sample) {
                                if (row.getSpo2() > 0) spo2 = row.getSpo2();
                            }
                            bloodOxygen += spo2;
                        }
                    }
                }
            } catch (Exception e) {
                LOG.warn("Could not calculate total blood oxygen: ", e);
            }
            return bloodOxygen;
        }

        private int getBloodPressure() {

            return bloodPressure;
        }

        private List<? extends ActivitySample> getAllSamples(DBHandler db, GBDevice device) {
            SampleProvider<? extends ActivitySample> provider = getProvider(db, device);
            return provider.getAllActivitySamples(timeFrom, timeTo);
        }

        private List<? extends ActivitySample> getSleepChartSamples(DBHandler db, GBDevice device) {
            SampleProvider<? extends ActivitySample> provider = getProvider(db, device);
            int[] range = getSleepChartRange();
            return provider.getAllActivitySamples(range[0], range[1] + 3600);
        }

        private int[] getSleepChartRange() {
            Calendar today = GregorianCalendar.getInstance();
            int hour = today.get(Calendar.HOUR_OF_DAY);
            today.setTimeInMillis(timeTo * 1000L);
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            if (hour < 12) {
                today.add(Calendar.HOUR, -6);
            } else {
                today.add(Calendar.HOUR, 6);
            }

            int from = (int) (today.getTimeInMillis() / 1000);
            return new int[]{from, from + (12 * 3600)};
        }

        private int resolveSleepChartEnd(List<? extends ActivitySample> samples, int baseEnd) {
            int latestSample = 0;
            for (ActivitySample sample : samples) {
                latestSample = Math.max(latestSample, sample.getTimestamp());
            }
            if (latestSample <= 0) {
                return baseEnd;
            }
            return Math.min(baseEnd + 3600, Math.max(baseEnd, latestSample + 3600));
        }

        protected SampleProvider<? extends AbstractActivitySample> getProvider(DBHandler db, GBDevice device) {
            DeviceCoordinator coordinator = device.getDeviceCoordinator();
            return coordinator.getSampleProvider(device, db.getDaoSession());
        }

        private List<BaseActivitySummary> getWorkoutSamples(DBHandler db) {
            return db.getDaoSession().getBaseActivitySummaryDao().queryBuilder().where(
                    BaseActivitySummaryDao.Properties.StartTime.gt(new Date(timeFrom * 1000L)),
                    BaseActivitySummaryDao.Properties.EndTime.lt(new Date(timeTo * 1000L))
            ).build().list();
        }

        private void put(final String key, final Serializable value) {
            genericData.put(key, value);
        }

        private Serializable get(final String key) {
            return genericData.get(key);
        }

        /**
         * @noinspection UnusedReturnValue
         */
        private Serializable computeIfAbsent(final String key, final Supplier<Serializable> supplier) {
            return genericData.computeIfAbsent(key, absent -> supplier.get());
        }

        private static class GeneralizedActivity implements Serializable {
            public ActivityKind activityKind;
            public long timeFrom;
            public long timeTo;

            public GeneralizedActivity(ActivityKind activityKind, long timeFrom, long timeTo) {
                this.activityKind = activityKind;
                this.timeFrom = timeFrom;
                this.timeTo = timeTo;
            }

            @NonNull
            @Override
            public String toString() {
                return "Generalized activity: timeFrom=" + timeFrom + ", timeTo=" + timeTo + ", activityKind=" + activityKind + ", calculated duration: " + (timeTo - timeFrom) + " seconds";
            }
        }
    }

    private class ClockChart extends AsyncTask<Void, Void, Void> {
        private ImageView view;
        private MyData data;

        private ClockChart(ImageView view, MyData data) {
            this.view = view;
            this.data = data;
        }

        private final TreeMap<Long, ActivityKind> activityTimestamps = new TreeMap<>();

        private void addActivity(long timeFrom, long timeTo, ActivityKind activityKind) {
            for (long i = timeFrom; i <= timeTo; i++) {
                // If the current timestamp isn't saved yet, do so immediately
                if (activityTimestamps.get(i) == null) {
                    activityTimestamps.put(i, activityKind);
                    continue;
                }
                // If the current timestamp is already saved, compare the activity kinds and
                // keep the most 'important' one
                switch (activityTimestamps.get(i)) {
                    case EXERCISE:
                        break;
                    case ACTIVITY:
                        if (activityKind == ActivityKind.EXERCISE)
                            activityTimestamps.put(i, activityKind);
                        break;
                    case DEEP_SLEEP:
                        if (activityKind == ActivityKind.EXERCISE ||
                                activityKind == ActivityKind.ACTIVITY)
                            activityTimestamps.put(i, activityKind);
                        break;
                    case LIGHT_SLEEP:
                        if (activityKind == ActivityKind.EXERCISE ||
                                activityKind == ActivityKind.ACTIVITY ||
                                activityKind == ActivityKind.DEEP_SLEEP)
                            activityTimestamps.put(i, activityKind);
                        break;
                    case REM_SLEEP:
                        if (activityKind == ActivityKind.EXERCISE ||
                                activityKind == ActivityKind.ACTIVITY ||
                                activityKind == ActivityKind.DEEP_SLEEP ||
                                activityKind == ActivityKind.LIGHT_SLEEP)
                            activityTimestamps.put(i, activityKind);
                        break;
                    case AWAKE_SLEEP:
                        if (activityKind == ActivityKind.EXERCISE ||
                                activityKind == ActivityKind.ACTIVITY ||
                                activityKind == ActivityKind.DEEP_SLEEP ||
                                activityKind == ActivityKind.LIGHT_SLEEP ||
                                activityKind == ActivityKind.REM_SLEEP)
                            activityTimestamps.put(i, activityKind);
                        break;
                    case SLEEP_ANY:
                    case NOT_MEASURED:
                        if (activityKind == ActivityKind.EXERCISE ||
                                activityKind == ActivityKind.ACTIVITY ||
                                activityKind == ActivityKind.DEEP_SLEEP ||
                                activityKind == ActivityKind.LIGHT_SLEEP ||
                                activityKind == ActivityKind.REM_SLEEP ||
                                activityKind == ActivityKind.AWAKE_SLEEP)
                            activityTimestamps.put(i, activityKind);
                        break;
                    default:
                        activityTimestamps.put(i, activityKind);
                        break;
                }
            }
        }

        private void calculateWornSessions(List<ActivitySample> samples) {
            int firstTimestamp = 0;
            int lastTimestamp = 0;

            for (ActivitySample sample : samples) {
                // Treat as worn if: valid HR recorded OR device is recording sleep stages
                final boolean hasValidHR = HeartRateUtils.getInstance().isValidHeartRateValue(sample.getHeartRate());
                final boolean isSleepSample = ActivityKind.isSleep(sample.getKind());
                final boolean isWorn = hasValidHR || isSleepSample;

                if (!isWorn && firstTimestamp == 0) continue;
                if (firstTimestamp == 0) firstTimestamp = sample.getTimestamp();
                if (lastTimestamp == 0) lastTimestamp = sample.getTimestamp();
                if (isWorn
                        && sample.getTimestamp() > lastTimestamp + data.hrIntervalSecs
                        && firstTimestamp != lastTimestamp) {
                    LOG.debug("Registered worn session from {} to {}", firstTimestamp, lastTimestamp);
                    addActivity(firstTimestamp, lastTimestamp, ActivityKind.NOT_MEASURED);
                    firstTimestamp = sample.getTimestamp();
                    lastTimestamp = sample.getTimestamp();
                    continue;
                }
                if (isWorn) {
                    lastTimestamp = sample.getTimestamp();
                }
            }
            if (firstTimestamp != lastTimestamp) {
                LOG.debug("Registered worn session from {} to {}", firstTimestamp, lastTimestamp);
                addActivity(firstTimestamp, lastTimestamp, ActivityKind.NOT_MEASURED);
            }
        }

        private void createGeneralizedActivities() {
            long currentTime = Calendar.getInstance().getTimeInMillis() / 1000;
            long midDaySecond = data.timeTo - (12 * 60 * 60);
            MyData.GeneralizedActivity previous = null;
            for (Map.Entry<Long, ActivityKind> activity : activityTimestamps.entrySet()) {
                long timestamp = activity.getKey();
                ActivityKind activityKind = activity.getValue();
                // Start a new merged activity on certain conditions
                if (previous == null || previous.activityKind != activityKind || (!mode_24h && timestamp == midDaySecond) || (!mode_24h && timestamp == midDaySecond - 86400) || timestamp == data.timeTo - 86400 || timestamp == currentTime - 86400 || previous.timeTo < timestamp - 60) {
                    previous = new MyData.GeneralizedActivity(activityKind, timestamp, timestamp);
                    data.generalizedActivities.add(previous);
                } else {
                    previous.timeTo = timestamp;
                }
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            final long nanoStart = System.nanoTime();

            // Retrieve activity data
            data.generalizedActivities.clear();
            List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
            List<ActivitySample> allActivitySamples = new ArrayList<>();
            List<ActivitySession> stepSessions = new ArrayList<>();
            List<BaseActivitySummary> activitySummaries = null;
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                for (GBDevice dev : devices) {
                    if ((data.showAllDevices || data.showDeviceList.contains(dev.getAddress())) && dev.getDeviceCoordinator().supportsActivityTracking()) {
                        List<? extends ActivitySample> activitySamples = data.getAllSamples(dbHandler, dev);
                        allActivitySamples.addAll(activitySamples);
                        StepAnalysis stepAnalysis = new StepAnalysis();
                        stepSessions.addAll(stepAnalysis.calculateStepSessions(activitySamples));
                    }
                }
                activitySummaries = data.getWorkoutSamples(dbHandler);
            } catch (Exception e) {
                LOG.warn("Could not retrieve activity amounts: ", e);
            }
            Collections.sort(allActivitySamples, (lhs, rhs) -> Integer.valueOf(lhs.getTimestamp()).compareTo(rhs.getTimestamp()));

            // Determine worn sessions from heart rate samples
            calculateWornSessions(allActivitySamples);

            // Integrate various data from multiple devices
            for (ActivitySample sample : allActivitySamples) {
                // Handle only TYPE_NOT_WORN and TYPE_SLEEP (including variants) here
                if (sample.getKind() != ActivityKind.NOT_WORN && (sample.getKind() == ActivityKind.NOT_MEASURED || !ActivityKind.isSleep(sample.getKind())))
                    continue;
                // Add to day results
                addActivity(sample.getTimestamp(), sample.getTimestamp() + 60, sample.getKind());
            }
            if (activitySummaries != null) {
                for (BaseActivitySummary baseActivitySummary : activitySummaries) {
                    addActivity(baseActivitySummary.getStartTime().getTime() / 1000, baseActivitySummary.getEndTime().getTime() / 1000, ActivityKind.EXERCISE);
                }
            }
            for (ActivitySession session : stepSessions) {
                addActivity(session.getStartTime().getTime() / 1000, session.getEndTime().getTime() / 1000, ActivityKind.ACTIVITY);
            }

            // Merge per-second activities
            createGeneralizedActivities();

            final long nanoEnd = System.nanoTime();
            final long executionTime = (nanoEnd - nanoStart) / 1000000;
            LOG.debug("fillData for {} took {}ms", ClockChart.this.getClass().getSimpleName(), executionTime);

            return null;
        }

        @Override
        protected void onPostExecute(final Void unused) {
            super.onPostExecute(unused);
            try {
                view.setImageBitmap(drawClock(data));
            } catch (final Exception e) {
                LOG.error("calling drawClock() failed", e);
            }
        }
    }

    private Bitmap drawClock(MyData data) {
        Prefs prefs = GBApplication.getPrefs();
        boolean upsideDown24h = prefs.getBoolean("dashboard_widget_today_24h_upside_down", false);
        boolean showYesterday = prefs.getBoolean("dashboard_widget_today_show_yesterday", false);

        // Prepare circular chart
        long currentDayStart = data.timeTo - 86400;
        long midDaySecond = currentDayStart + (12 * 60 * 60);
        int width = Resources.getSystem().getDisplayMetrics().widthPixels;
        int height = width;
        int barWidth = Math.round(width * 0.08f);
        int hourTextSp = Math.round(width * 0.024f);
        float hourTextPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                hourTextSp,
                GBApplication.getContext().getResources().getDisplayMetrics()
        );
        float outerCircleMargin = mode_24h ? barWidth / 2f : barWidth / 2f + hourTextPixels * 1.3f;
        float innerCircleMargin = outerCircleMargin + barWidth * 1.3f;
        float degreeFactor = mode_24h ? 240 : 120;
        Bitmap bitmapChart = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapChart);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);

        // Draw clock stripes
        float clockMargin = outerCircleMargin + (mode_24h ? barWidth : barWidth * 2.3f);
        int clockStripesInterval = mode_24h ? 15 : 30;
        float clockStripesWidth = barWidth / 3f;
        paint.setStrokeWidth(clockStripesWidth);
        paint.setColor(color_stripe);
        for (int i = 0; i < 360; i += clockStripesInterval) {
            canvas.drawArc(clockMargin, clockMargin, width - clockMargin, height - clockMargin, i, 1, false, paint);
        }

        // Draw hours
        boolean normalClock = DateFormat.is24HourFormat(GBApplication.getContext());
        Map<Integer, String> hours = new HashMap<Integer, String>() {
            {
                put(0, normalClock ? (mode_24h ? "0" : "12") : "12pm");
                put(3, "3");
                put(6, normalClock ? "6" : "6am");
                put(9, "9");
                put(12, normalClock ? (mode_24h ? "12" : "0") : "12am");
                put(15, normalClock ? "15" : "3");
                put(18, normalClock ? "18" : "6pm");
                put(21, normalClock ? "21" : "9");
            }
        };
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(color_text);
        textPaint.setTextSize(hourTextPixels);
        textPaint.setTextAlign(Paint.Align.CENTER);
        Rect textBounds = new Rect();
        if (mode_24h && upsideDown24h) {
            textPaint.getTextBounds(hours.get(0), 0, hours.get(0).length(), textBounds);
            canvas.drawText(hours.get(0), width / 2f, height - (clockMargin + clockStripesWidth), textPaint);
            textPaint.getTextBounds(hours.get(6), 0, hours.get(6).length(), textBounds);
            canvas.drawText(hours.get(6), clockMargin + clockStripesWidth + textBounds.width() / 2f, height / 2f + textBounds.height() / 2f, textPaint);
            textPaint.getTextBounds(hours.get(12), 0, hours.get(12).length(), textBounds);
            canvas.drawText(hours.get(12), width / 2f, clockMargin + clockStripesWidth + textBounds.height(), textPaint);
            textPaint.getTextBounds(hours.get(18), 0, hours.get(18).length(), textBounds);
            canvas.drawText(hours.get(18), width - (clockMargin + clockStripesWidth + textBounds.width()), height / 2f + textBounds.height() / 2f, textPaint);
        } else if (mode_24h) {
            textPaint.getTextBounds(hours.get(0), 0, hours.get(0).length(), textBounds);
            canvas.drawText(hours.get(0), width / 2f, clockMargin + clockStripesWidth + textBounds.height(), textPaint);
            textPaint.getTextBounds(hours.get(6), 0, hours.get(6).length(), textBounds);
            canvas.drawText(hours.get(6), width - (clockMargin + clockStripesWidth + textBounds.width()), height / 2f + textBounds.height() / 2f, textPaint);
            textPaint.getTextBounds(hours.get(12), 0, hours.get(12).length(), textBounds);
            canvas.drawText(hours.get(12), width / 2f, height - (clockMargin + clockStripesWidth), textPaint);
            textPaint.getTextBounds(hours.get(18), 0, hours.get(18).length(), textBounds);
            canvas.drawText(hours.get(18), clockMargin + clockStripesWidth + textBounds.width() / 2f, height / 2f + textBounds.height() / 2f, textPaint);
        } else {
            textPaint.getTextBounds(hours.get(0), 0, hours.get(0).length(), textBounds);
            canvas.drawText(hours.get(0), width / 2f, textBounds.height(), textPaint);
            textPaint.getTextBounds(hours.get(3), 0, hours.get(3).length(), textBounds);
            canvas.drawText(hours.get(3), width - (clockMargin + clockStripesWidth + textBounds.width()), height / 2f + textBounds.height() / 2f, textPaint);
            textPaint.getTextBounds(hours.get(6), 0, hours.get(6).length(), textBounds);
            canvas.drawText(hours.get(6), width / 2f, height - (clockMargin + clockStripesWidth), textPaint);
            textPaint.getTextBounds(hours.get(9), 0, hours.get(9).length(), textBounds);
            canvas.drawText(hours.get(9), clockMargin + clockStripesWidth + textBounds.width() / 2f, height / 2f + textBounds.height() / 2f, textPaint);
            textPaint.getTextBounds(hours.get(12), 0, hours.get(12).length(), textBounds);
            canvas.drawText(hours.get(12), width / 2f, clockMargin + clockStripesWidth + textBounds.height(), textPaint);
            textPaint.getTextBounds(hours.get(15), 0, hours.get(15).length(), textBounds);
            canvas.drawText(hours.get(15), (float) (width - Math.ceil(textBounds.width() / 2f)), height / 2f + textBounds.height() / 2f, textPaint);
            textPaint.getTextBounds(hours.get(18), 0, hours.get(18).length(), textBounds);
            canvas.drawText(hours.get(18), width / 2f, height - textBounds.height() / 2f, textPaint);
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.getTextBounds(hours.get(21), 0, hours.get(21).length(), textBounds);
            canvas.drawText(hours.get(21), 1, height / 2f + textBounds.height() / 2f, textPaint);
        }

        // Draw generalized activities on circular chart
        long secondIndex = data.timeFrom;
        long currentTime = Calendar.getInstance().getTimeInMillis() / 1000;
        boolean dayIsToday = !(data.timeTo < currentTime);
        int startAngle = mode_24h && upsideDown24h ? 90 : 270;
        synchronized (data.generalizedActivities) {
            for (MyData.GeneralizedActivity activity : data.generalizedActivities) {
                // Determine margin
                float margin = innerCircleMargin;
                if (mode_24h || activity.timeFrom >= midDaySecond) {
                    margin = outerCircleMargin;
                }
                if (!mode_24h && showYesterday && dayIsToday) {
                    if (activity.timeFrom < currentDayStart && activity.timeFrom > midDaySecond - 86400) {
                        margin = outerCircleMargin;
                    }
                }
                // Skip activities from before 24h ago (to prevent double drawing the same position)
                if (showYesterday && dayIsToday && (activity.timeTo < currentTime - 86400)) {
                    continue;
                }
                // Draw inactive slices
                if (!mode_24h && secondIndex < midDaySecond && activity.timeFrom >= midDaySecond) {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_unknown);
                    canvas.drawArc(innerCircleMargin, innerCircleMargin, width - innerCircleMargin, height - innerCircleMargin, startAngle + (secondIndex - data.timeFrom) / degreeFactor, (midDaySecond - secondIndex) / degreeFactor, false, paint);
                    secondIndex = midDaySecond;
                }
                if (activity.timeFrom > secondIndex) {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_unknown);
                    canvas.drawArc(margin, margin, width - margin, height - margin, startAngle + (secondIndex - data.timeFrom) / degreeFactor, (activity.timeFrom - secondIndex) / degreeFactor, false, paint);
                }
                float start_angle = startAngle + (activity.timeFrom - data.timeFrom) / degreeFactor;
                float sweep_angle = (activity.timeTo - activity.timeFrom) / degreeFactor;
                if (activity.activityKind == ActivityKind.NOT_MEASURED) {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_worn);
                    if (showYesterday && dayIsToday && activity.timeFrom < currentDayStart) {
                        paint.setAlpha(64);
                    }
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                } else if (activity.activityKind == ActivityKind.NOT_WORN) {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_not_worn);
                    if (showYesterday && dayIsToday && activity.timeFrom < currentDayStart) {
                        paint.setAlpha(64);
                    }
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                } else if (activity.activityKind == ActivityKind.LIGHT_SLEEP || activity.activityKind == ActivityKind.SLEEP_ANY) {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_light_sleep);
                    if (showYesterday && dayIsToday && activity.timeFrom < currentDayStart) {
                        paint.setAlpha(64);
                    }
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                } else if (activity.activityKind == ActivityKind.REM_SLEEP) {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_rem_sleep);
                    if (showYesterday && dayIsToday && activity.timeFrom < currentDayStart) {
                        paint.setAlpha(64);
                    }
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                } else if (activity.activityKind == ActivityKind.DEEP_SLEEP) {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_deep_sleep);
                    if (showYesterday && dayIsToday && activity.timeFrom < currentDayStart) {
                        paint.setAlpha(64);
                    }
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                } else if (activity.activityKind == ActivityKind.AWAKE_SLEEP) {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_awake_sleep);
                    if (showYesterday && dayIsToday && activity.timeFrom < currentDayStart) {
                        paint.setAlpha(64);
                    }
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                } else if (activity.activityKind == ActivityKind.EXERCISE) {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_exercise);
                    if (showYesterday && dayIsToday && activity.timeFrom < currentDayStart) {
                        paint.setAlpha(64);
                    }
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                } else {
                    paint.setStrokeWidth(barWidth);
                    paint.setColor(color_activity);
                    if (showYesterday && dayIsToday && activity.timeFrom < currentDayStart) {
                        paint.setAlpha(64);
                    }
                    canvas.drawArc(margin, margin, width - margin, height - margin, start_angle, sweep_angle, false, paint);
                }
                secondIndex = activity.timeTo;
            }
        }
        // Draw indicator for current time
        if (prefs.getBoolean("dashboard_widget_today_time_indicator", false) && currentTime < data.timeTo) {
            float margin = (mode_24h || currentTime >= midDaySecond) ? outerCircleMargin : innerCircleMargin;
            paint.setStrokeWidth(barWidth);
            paint.setColor(GBApplication.getTextColor(requireContext()));
            canvas.drawArc(margin, margin, width - margin, height - margin, startAngle + (currentTime - data.timeFrom) / degreeFactor, 300 / degreeFactor, false, paint);
        }
        // Fill remaining time until current time in 12h mode before midday
        if (!mode_24h && currentTime < midDaySecond) {
            // Fill inner bar up until current time
            paint.setStrokeWidth(barWidth);
            paint.setColor(color_unknown);
            canvas.drawArc(innerCircleMargin, innerCircleMargin, width - innerCircleMargin, height - innerCircleMargin, startAngle + (secondIndex - data.timeFrom) / degreeFactor, (currentTime - secondIndex) / degreeFactor, false, paint);
            // Fill inner bar up until midday
            paint.setStrokeWidth(barWidth);
            paint.setColor(color_unknown);
            canvas.drawArc(innerCircleMargin, innerCircleMargin, width - innerCircleMargin, height - innerCircleMargin, startAngle + (currentTime - data.timeFrom) / degreeFactor, (midDaySecond - currentTime) / degreeFactor, false, paint);
            // Fill outer bar up until midnight
            paint.setStrokeWidth(barWidth);
            paint.setColor(color_unknown);
            canvas.drawArc(outerCircleMargin, outerCircleMargin, width - outerCircleMargin, height - outerCircleMargin, 0, 360, false, paint);
        }
        // Fill remaining time until current time in 24h mode or in 12h mode after midday
        if ((mode_24h || currentTime >= midDaySecond) && currentTime < data.timeTo) {
            // Fill inner bar up until midday
            if (!mode_24h && secondIndex < midDaySecond) {
                paint.setStrokeWidth(barWidth);
                paint.setColor(color_unknown);
                canvas.drawArc(innerCircleMargin, innerCircleMargin, width - innerCircleMargin, height - innerCircleMargin, startAngle + (secondIndex - data.timeFrom) / degreeFactor, (midDaySecond - secondIndex) / degreeFactor, false, paint);
                secondIndex = midDaySecond;
            }
            // Fill outer bar up until current time
            paint.setStrokeWidth(barWidth);
            paint.setColor(color_unknown);
            canvas.drawArc(outerCircleMargin, outerCircleMargin, width - outerCircleMargin, height - outerCircleMargin, startAngle + (secondIndex - data.timeFrom) / degreeFactor, (currentTime - secondIndex) / degreeFactor, false, paint);
            // Fill outer bar up until midnight
            paint.setStrokeWidth(barWidth);
            paint.setColor(color_unknown);
            canvas.drawArc(outerCircleMargin, outerCircleMargin, width - outerCircleMargin, height - outerCircleMargin, startAngle + (currentTime - data.timeFrom) / degreeFactor, (data.timeTo - currentTime) / degreeFactor, false, paint);
        }
        // Only when displaying a past day
        if (secondIndex < data.timeTo && currentTime > data.timeTo) {
            // Fill outer bar up until midnight
            paint.setStrokeWidth(barWidth);
            paint.setColor(color_unknown);
            canvas.drawArc(outerCircleMargin, outerCircleMargin, width - outerCircleMargin, height - outerCircleMargin, startAngle + (secondIndex - data.timeFrom) / degreeFactor, (data.timeTo - secondIndex) / degreeFactor, false, paint);
        }

        return bitmapChart;
    }

    private void setupActivityChart() {
        activityChart.setBackgroundColor(BACKGROUND_COLOR);
        activityChart.getDescription().setTextColor(DESCRIPTION_COLOR);
        configureBarLineChartDefaults(activityChart);

        XAxis x = activityChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
        x.setEnabled(true);
        x.setTextColor(CHART_TEXT_COLOR);
        x.setDrawLimitLinesBehindData(true);

        YAxis y = activityChart.getAxisLeft();
        y.setDrawGridLines(false);
        y.setAxisMaximum(1f);
        y.setAxisMinimum(0);
        y.setDrawTopYLabelEntry(false);
        y.setTextColor(CHART_TEXT_COLOR);
        y.setEnabled(true);

        YAxis yAxisRight = activityChart.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(true);
        yAxisRight.setDrawTopYLabelEntry(true);
        yAxisRight.setTextColor(CHART_TEXT_COLOR);
        // Accommodate HR, SpO2, and stress values on the same axis.
        yAxisRight.setAxisMaximum(105f);
        yAxisRight.setAxisMinimum(0f);

        activityChart.getLegend().setWordWrapEnabled(true);
        activityChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        activityChart.getLegend().setTextColor(LEGEND_TEXT_COLOR);
    }

    private void applySleepLegend() {
        List<LegendEntry> legendEntries = new ArrayList<>(6);

        LegendEntry deepSleepEntry = new LegendEntry();
        deepSleepEntry.label = akDeepSleep.label;
        deepSleepEntry.formColor = akDeepSleep.color;
        legendEntries.add(deepSleepEntry);

        LegendEntry lightSleepEntry = new LegendEntry();
        lightSleepEntry.label = akLightSleep.label;
        lightSleepEntry.formColor = akLightSleep.color;
        legendEntries.add(lightSleepEntry);

        LegendEntry remSleepEntry = new LegendEntry();
        remSleepEntry.label = akRemSleep.label;
        remSleepEntry.formColor = akRemSleep.color;
        legendEntries.add(remSleepEntry);

        LegendEntry awakeSleepEntry = new LegendEntry();
        awakeSleepEntry.label = akAwakeSleep.label;
        awakeSleepEntry.formColor = akAwakeSleep.color;
        legendEntries.add(awakeSleepEntry);

        LegendEntry hrEntry = new LegendEntry();
        hrEntry.label = HEARTRATE_LABEL;
        hrEntry.formColor = HEARTRATE_COLOR;
        legendEntries.add(hrEntry);

        LegendEntry spo2Entry = new LegendEntry();
        spo2Entry.label = "SpO2";
        spo2Entry.formColor = Color.GREEN;
        legendEntries.add(spo2Entry);

        LegendEntry stressEntry = new LegendEntry();
        stressEntry.label = "Stress";
        stressEntry.formColor = Color.MAGENTA;
        legendEntries.add(stressEntry);

        activityChart.getLegend().setCustom(legendEntries);
    }

    private boolean supportsHeartrate(GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        return coordinator.supportsHeartRateMeasurement(device);
    }

    private int getIndexOfActivity(ActivityKind kind) {
        switch (kind) {
            case DEEP_SLEEP:
                return 0;
            case LIGHT_SLEEP:
                return 1;
            case REM_SLEEP:
                return 2;
            case AWAKE_SLEEP:
                return 3;
            case NOT_WORN:
                return 4;
            default:
                return 5; // treated as ActivityKind.ACTIVITY
        }
    }

    protected Entry createLineEntry(float value, int xValue) {
        return new Entry(xValue, value);
    }

    private boolean supportsRemSleep(GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        return coordinator.supportsRemSleep();
    }

    private boolean supportsAwakeSleep(GBDevice device) {
        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        return coordinator.supportsAwakeSleep();
    }

    protected void configureChartDefaults(Chart<?> chart) {
        chart.getXAxis().setValueFormatter(new TimestampValueFormatter());
        chart.getDescription().setText("");
        // if enabled, the chart will always start at zero on the y-axis
        chart.setNoDataText(getString(R.string.chart_no_data_synchronize));
        // disable value highlighting
        chart.setHighlightPerTapEnabled(false);
        // enable touch gestures
        chart.setTouchEnabled(true);
    }

    protected void configureBarLineChartDefaults(BarLineChartBase<?> chart) {
        configureChartDefaults(chart);
        if (chart instanceof BarChart) {
            ((BarChart) chart).setFitBars(true);
        }
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
    }

    protected LineDataSet createDataSet(List<Entry> values, Integer color, String label) {
        LineDataSet set1 = new LineDataSet(values, label);
        set1.setColor(color);
        set1.setDrawFilled(true);
        set1.setDrawCircles(false);
        set1.setFillColor(color);
        set1.setFillAlpha(255);
        set1.setDrawValues(false);
        set1.setValueTextColor(CHART_TEXT_COLOR);
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        return set1;
    }

    protected LineDataSet createHeartrateSet(List<Entry> values, String label) {
        LineDataSet set1 = new LineDataSet(values, label);
        set1.setLineWidth(2.2f);
        set1.setColor(HEARTRATE_COLOR);
        set1.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        set1.setCubicIntensity(0.1f);
        set1.setDrawCircles(false);
        set1.setDrawValues(true);
        set1.setValueTextColor(CHART_TEXT_COLOR);
        set1.setAxisDependency(YAxis.AxisDependency.RIGHT);
        return set1;
    }

    private float calculateIntensitySum(List<Float> samples) {
        float result = 0;
        for (Float sample : samples) {
            result += sample;
        }
        return result;
    }

    private float calculateSumOfInts(List<Integer> samples) {
        float result = 0;
        for (Integer sample : samples) {
            result += sample;
        }
        return result;
    }

    private Triple<Float, Integer, Integer> calculateHrData(List<? extends ActivitySample> samples) {
        if (samples.toArray().length < 1) {
            return Triple.of(0f, 0, 0);
        }

        List<Integer> heartRateValues = new ArrayList<>();
        HeartRateUtils heartRateUtilsInstance = HeartRateUtils.getInstance();
        for (ActivitySample sample : samples) {
            if (sample.getKind() == ActivityKind.LIGHT_SLEEP || sample.getKind() == ActivityKind.DEEP_SLEEP) {
                int heartRate = sample.getHeartRate();
                if (heartRateUtilsInstance.isValidHeartRateValue(heartRate)) {
                    heartRateValues.add(heartRate);
                }
            }
        }
        if (heartRateValues.toArray().length < 1) {
            return Triple.of(0f, 0, 0);
        }

        int min = Collections.min(heartRateValues);
        int max = Collections.max(heartRateValues);
        int count = heartRateValues.toArray().length;
        float sum = calculateSumOfInts(heartRateValues);
        float average = sum / count;
        return Triple.of(average, min, max);
    }

    private Triple<Float, Float, Float> calculateIntensityData(List<? extends ActivitySample> samples) {
        if (samples.toArray().length < 1) {
            return Triple.of(0f, 0f, 0f);
        }

        List<Float> allIntensities = new ArrayList<>();

        for (ActivitySample s : samples) {
            if (s.getKind() == ActivityKind.LIGHT_SLEEP || s.getKind() == ActivityKind.DEEP_SLEEP) {
                float intensity = s.getIntensity();
                allIntensities.add(intensity);
            }
        }
        if (allIntensities.toArray().length < 1) {
            return Triple.of(0f, 0f, 0f);
        }

        Float min = Collections.min(allIntensities);
        Float max = Collections.max(allIntensities);
        Float sum = calculateIntensitySum(allIntensities);

        return Triple.of(sum, min, max);
    }

    private void refreshChart() {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        if (devices.toArray().length > 0) {
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                // Merge samples from all selected devices
                List<ActivitySample> allSamples = new ArrayList<>();
                GBDevice primaryDevice = null;
                int[] chartRange = myData1.getSleepChartRange();
                for (GBDevice dev : devices) {
                    if (myData1.showAllDevices || myData1.showDeviceList.contains(dev.getAddress())) {
                        if (primaryDevice == null) primaryDevice = dev;
                        allSamples.addAll(myData1.getSleepChartSamples(dbHandler, dev));
                    }
                }
                if (primaryDevice == null) return;
                allSamples.sort((a, b) -> Integer.compare(a.getTimestamp(), b.getTimestamp()));
                List<? extends ActivitySample> samples = allSamples;
                int chartTo = myData1.resolveSleepChartEnd(samples, chartRange[1]);
                DefaultChartsData<LineData> chartsData = defChartsData(primaryDevice, samples, chartRange[0], chartTo);
                Triple<Float, Integer, Integer> hrData = calculateHrData(samples);
                Triple<Float, Float, Float> intensityData = calculateIntensityData(samples);
                MyChartsData mcd = new MyChartsData(chartsData, hrData.getLeft(), hrData.getMiddle(), hrData.getRight(), intensityData.getLeft(), intensityData.getMiddle(), intensityData.getRight());

                activityChart.setData(null);
                activityChart.getXAxis().setValueFormatter(mcd.getChartsData().getXValueFormatter());
                activityChart.getXAxis().setAxisMinimum(0f);
                activityChart.getXAxis().setAxisMaximum(Math.max(1, chartTo - chartRange[0]));
                activityChart.getAxisLeft().setDrawLabels(false);
                activityChart.setData(mcd.getChartsData().getData());
                applySleepLegend();
            } catch (Exception e) {
                LOG.warn("Could not get charts data: ", e);
            }
        }
    }

    private DefaultChartsData<LineData> defChartsData(GBDevice gbDevice, List<? extends ActivitySample> samples, int chartFrom, int chartTo) {
        TimestampTranslation tsTranslation = new TimestampTranslation();
        tsTranslation.shorten(chartFrom);
        LineData lineData;

        if (samples.isEmpty()) {
            lineData = new LineData();
            ValueFormatter xValueFormatter = new SampleXLabelFormatter(tsTranslation, "HH:mm");
            return new DefaultChartsData<>(lineData, xValueFormatter);
        }

        ActivityKind last_type = ActivityKind.UNKNOWN;
        float last_value = 0;

        int numEntries = samples.size();
        List<List<Entry>> entries = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            entries.add(new ArrayList<>());
        }

        boolean hr = supportsHeartrate(gbDevice);
        List<Entry> heartrateEntries = hr ? new ArrayList<Entry>(numEntries) : null;

        int lastHrSampleIndex = -1;
        HeartRateUtils heartRateUtilsInstance = HeartRateUtils.getInstance();

        for (int i = 0; i < numEntries; i++) {
            ActivitySample sample = samples.get(i);
            ActivityKind type = sample.getKind();
            int ts = tsTranslation.shorten(sample.getTimestamp());
            final float value;
            if (type != ActivityKind.NOT_WORN) {
                if (ActivityKind.isSleep(type) && sample.getIntensity() < 0) {
                    switch (type) {
                        case SLEEP_ANY:
                        case AWAKE_SLEEP:
                            value = 0.25f;
                            break;
                        case DEEP_SLEEP:
                            value = 0.10f;
                            break;
                        case LIGHT_SLEEP:
                            value = 0.15f;
                            break;
                        case REM_SLEEP:
                            value = 0.20f;
                            break;
                        default:
                            value = Y_VALUE_DEEP_SLEEP;
                            break;
                    }
                } else {
                    value = sample.getIntensity();
                }
            } else {
                value = Y_VALUE_DEEP_SLEEP;
            }

            // do not interpolate NOT_WORN on any side
            boolean interpolate = !(last_type == ActivityKind.NOT_WORN || type == ActivityKind.NOT_WORN);
            float interpolation_value = interpolate ? value : last_value;

            // filled charts
            int index = getIndexOfActivity(type);
            int last_index = getIndexOfActivity(last_type);
            if (last_type != type) {
                entries.get(index).add(createLineEntry(0, ts));
                entries.get(last_index).add(createLineEntry(interpolation_value, ts));
                entries.get(last_index).add(createLineEntry(0, ts));
            }
            entries.get(index).add(createLineEntry(value, ts));

            // heart rate line graph
            if (hr && type != ActivityKind.NOT_WORN && heartRateUtilsInstance.isValidHeartRateValue(sample.getHeartRate())) {
                if (lastHrSampleIndex > -1 && ts - lastHrSampleIndex > 1800 * HeartRateUtils.MAX_HR_MEASUREMENTS_GAP_MINUTES) {
                    heartrateEntries.add(createLineEntry(0, lastHrSampleIndex + 1));
                    heartrateEntries.add(createLineEntry(0, ts - 1));
                }
                heartrateEntries.add(createLineEntry(sample.getHeartRate(), ts));
                lastHrSampleIndex = ts;
            }
            last_type = type;
            last_value = value;
        }

        // convert Entry Lists to Datasets
        List<ILineDataSet> lineDataSets = new ArrayList<>();

        lineDataSets.add(createDataSet(
                entries.get(getIndexOfActivity(ActivityKind.DEEP_SLEEP)), akDeepSleep.color, "Deep Sleep"
        ));
        lineDataSets.add(createDataSet(
                entries.get(getIndexOfActivity(ActivityKind.LIGHT_SLEEP)), akLightSleep.color, "Light Sleep"
        ));
        if (!entries.get(getIndexOfActivity(ActivityKind.REM_SLEEP)).isEmpty()) {
            lineDataSets.add(createDataSet(
                    entries.get(getIndexOfActivity(ActivityKind.REM_SLEEP)), akRemSleep.color, "REM Sleep"
            ));
        }
        if (!entries.get(getIndexOfActivity(ActivityKind.AWAKE_SLEEP)).isEmpty()) {
            lineDataSets.add(createDataSet(
                    entries.get(getIndexOfActivity(ActivityKind.AWAKE_SLEEP)), akAwakeSleep.color, "Awake Sleep"
            ));
        }
        if (hr && !heartrateEntries.isEmpty()) {
            LineDataSet heartrateSet = createHeartrateSet(heartrateEntries, "Heart Rate");
            lineDataSets.add(heartrateSet);
        }

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DeviceCoordinator coordinator = gbDevice.getDeviceCoordinator();
            int tsFrom = samples.get(0).getTimestamp();
            int tsTo = samples.get(samples.size() - 1).getTimestamp();

            if (coordinator.supportsSpo2(gbDevice)) {
                TimeSampleProvider<? extends Spo2Sample> spo2Provider = coordinator.getSpo2SampleProvider(gbDevice, dbHandler.getDaoSession());
                List<? extends Spo2Sample> spo2Samples = spo2Provider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);
                List<Entry> spo2Entries = new ArrayList<>();
                for (Spo2Sample spo2Sample : spo2Samples) {
                    int ts = tsTranslation.shorten((int) (spo2Sample.getTimestamp() / 1000));
                    int spo2 = spo2Sample.getSpo2();
                    if (spo2 > 0) {
                        spo2Entries.add(createLineEntry(spo2, ts));
                    }
                }
                if (spo2Entries.isEmpty()) {
                    Integer spo2 = getDailySummaryValue(dbHandler, gbDevice, tsFrom, tsTo, true);
                    if (spo2 != null && spo2 > 0) {
                        spo2Entries.add(createLineEntry(spo2, tsTranslation.shorten(tsFrom)));
                        spo2Entries.add(createLineEntry(spo2, tsTranslation.shorten(tsTo)));
                    }
                }

                if (!spo2Entries.isEmpty()) {
                    LineDataSet spo2Set = createHeartrateSet(spo2Entries, "SpO2");
                    spo2Set.setColor(Color.GREEN);
                    spo2Set.setLabel("SpO2");
                    lineDataSets.add(spo2Set);
                }
            }

            if (coordinator.supportsStressMeasurement()) {
                TimeSampleProvider<? extends StressSample> stressProvider = coordinator.getStressSampleProvider(gbDevice, dbHandler.getDaoSession());
                List<? extends StressSample> stressSamples = stressProvider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);
                List<Entry> stressEntries = new ArrayList<>();
                for (StressSample stressSample : stressSamples) {
                    int ts = tsTranslation.shorten((int) (stressSample.getTimestamp() / 1000));
                    int stress = stressSample.getStress();
                    if (stress > 0) {
                        stressEntries.add(createLineEntry(stress, ts));
                    }
                }
                if (stressEntries.isEmpty()) {
                    Integer stress = getDailySummaryValue(dbHandler, gbDevice, tsFrom, tsTo, false);
                    if (stress != null && stress > 0) {
                        stressEntries.add(createLineEntry(stress, tsTranslation.shorten(tsFrom)));
                        stressEntries.add(createLineEntry(stress, tsTranslation.shorten(tsTo)));
                    }
                }

                if (!stressEntries.isEmpty()) {
                    LineDataSet stressSet = createHeartrateSet(stressEntries, "Stress");
                    stressSet.setColor(Color.MAGENTA);
                    stressSet.setLabel("Stress");
                    lineDataSets.add(stressSet);
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not add SpO2/stress data to sleep chart", e);
        }

        lineData = new LineData(lineDataSets);

        ValueFormatter xValueFormatter = new SampleXLabelFormatter(tsTranslation, "HH:mm");
        return new DefaultChartsData<>(lineData, xValueFormatter);
    }

    private Integer getDailySummaryValue(DBHandler dbHandler, GBDevice device, int tsFrom, int tsTo, boolean spo2) {
        try {
            XiaomiDailySummarySampleProvider provider = new XiaomiDailySummarySampleProvider(device, dbHandler.getDaoSession());
            long dayStart = getDayStartMillis(tsTo);
            long dayEnd = dayStart + TimeUnit.DAYS.toMillis(1) - 1;
            List<XiaomiDailySummarySample> summaries = provider.getAllSamples(dayStart, dayEnd);
            if (summaries.isEmpty()) {
                dayStart = getDayStartMillis(tsFrom);
                dayEnd = dayStart + TimeUnit.DAYS.toMillis(1) - 1;
                summaries = provider.getAllSamples(dayStart, dayEnd);
            }
            for (XiaomiDailySummarySample summary : summaries) {
                Integer value = spo2 ? summary.getSpo2Avg() : summary.getStressAvg();
                if (value != null && value > 0) {
                    return value;
                }
            }
        } catch (Exception ignored) {
            // Daily summary is only available for some Xiaomi devices.
        }
        return null;
    }

    private long getDayStartMillis(int timestampSeconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestampSeconds * 1000L);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static class MyChartsData extends ChartsData {
        private final DefaultChartsData<LineData> chartsData;
        private final float heartRateAverage;
        private int heartRateAxisMax;
        private int heartRateAxisMin;
        private float intensityAxisMax;
        private float intensityAxisMin;
        private float intensityTotal;

        public MyChartsData(DefaultChartsData<LineData> chartsData, float heartRateAverage, int heartRateAxisMin, int heartRateAxisMax, float intensityTotal, float intensityAxisMin, float intensityAxisMax) {
            this.chartsData = chartsData;
            this.heartRateAverage = heartRateAverage;
            this.heartRateAxisMax = heartRateAxisMax;
            this.heartRateAxisMin = heartRateAxisMin;
            this.intensityTotal = intensityTotal;
            this.intensityAxisMin = intensityAxisMin;
            this.intensityAxisMax = intensityAxisMax;
        }

        public DefaultChartsData<LineData> getChartsData() {
            return chartsData;
        }

        public float getHeartRateAverage() {
            return heartRateAverage;
        }

        public int getHeartRateAxisMax() {
            return heartRateAxisMax;
        }

        public int getHeartRateAxisMin() {
            return heartRateAxisMin;
        }

        public float getIntensityAxisMax() {
            return intensityAxisMax;
        }

        public float getIntensityAxisMin() {
            return intensityAxisMin;
        }

        public float getIntensityTotal() {
            return intensityTotal;
        }
    }
}

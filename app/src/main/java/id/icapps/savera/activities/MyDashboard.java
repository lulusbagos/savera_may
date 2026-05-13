package id.icapps.savera.activities;

import static id.icapps.savera.model.DailyTotals.getTotalsSleepForActivityAmounts;
import static id.icapps.savera.util.GB.toast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.util.Collections;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.UUID;
import java.util.regex.Pattern;

import id.icapps.savera.GBApplication;
import id.icapps.savera.BuildConfig;
import id.icapps.savera.Http;
import id.icapps.savera.LocalStorage;
import id.icapps.savera.R;
import id.icapps.savera.util.ApiUrl;
import id.icapps.savera.activities.charts.ActivityAnalysis;
import id.icapps.savera.activities.charts.SleepAnalysis;
import id.icapps.savera.activities.charts.StepAnalysis;
import id.icapps.savera.activities.dashboard.AbstractDashboardWidget;
import id.icapps.savera.database.DBHandler;
import id.icapps.savera.devices.DeviceCoordinator;
import id.icapps.savera.devices.SampleProvider;
import id.icapps.savera.devices.TimeSampleProvider;
import id.icapps.savera.entities.AbstractActivitySample;
import id.icapps.savera.entities.BaseActivitySummary;
import id.icapps.savera.entities.BaseActivitySummaryDao;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.impl.GBDeviceService;
import id.icapps.savera.model.ActivityAmounts;
import id.icapps.savera.model.ActivityKind;
import id.icapps.savera.model.ActivitySample;
import id.icapps.savera.model.ActivitySession;
import id.icapps.savera.model.ActivityUser;
import id.icapps.savera.model.DeviceType;
import id.icapps.savera.model.DailyTotals;
import id.icapps.savera.model.HeartRateSample;
import id.icapps.savera.model.RecordedDataTypes;
import id.icapps.savera.model.Spo2Sample;
import id.icapps.savera.model.StressSample;
import id.icapps.savera.service.devices.xiaomi.activity.XiaomiActivityFileId;
import id.icapps.savera.util.DateTimeUtils;
import id.icapps.savera.util.FileUtils;
import id.icapps.savera.util.ImageUtils;
import id.icapps.savera.util.PendingUploadRetryManager;
import id.icapps.savera.util.PendingUploadQueue;
import id.icapps.savera.util.FormatUtils;
import id.icapps.savera.util.GB;
import id.icapps.savera.util.Prefs;

public class MyDashboard extends Fragment {
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int MAX_NETWORK_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_BASE_MS = 750L;
    private static final int INGEST_RAW_CHUNK_TARGET_BYTES = 700 * 1024;
    private static final int MAX_PENDING_UPLOADS_PER_FLUSH_LOCAL = 4;
    private static final int MAX_PENDING_UPLOADS_PER_FLUSH_PUBLIC = 2;
    private static final long PENDING_UPLOAD_FLUSH_JITTER_MS = 300L;
    private static final long PENDING_UPLOAD_GAP_MS_LOCAL = 350L;
    private static final long PENDING_UPLOAD_GAP_MS_PUBLIC = 1200L;
    private static final Logger LOG = LoggerFactory.getLogger(MyDashboard.class);
    private static Calendar now = GregorianCalendar.getInstance();
    private TextView textNama, textNik, textDepartemen, textMess, textInfo, textInfoSubtitle, textSteps, textDistance, textActive, textSleep, textSleepType, textHeartRate, textCalories, textMoving, textStanding, textBloodOxygen, textBloodPressure, textStress, textPAI, textWeight, textVOMax;
    private TextView textDate, textOperator, textDevice, textTime, textToday, textYesterday, textRest, arrowLeft, arrowRight;
    private TextView textNotificationBadge;
    private Button btnFit1Ya, btnFit2Ya, btnFit3Ya,
                     btnFit1Tdk, btnFit2Tdk, btnFit3Tdk;
    private ImageView imageProfile, imageProfile_;
    private View serverStatusIndicator;
    private LinearLayout sleepStatusCard;
    private ImageButton btnSync, btnReload, btnP5m, btnZona, btnNotification;
    private String employeePhoto;
    private int employeeId, userId, companyId, departmentId, shiftId, deviceId;
    private int isFit1, isFit2, isFit3;
    private int lastSummaryId = 0;
    private final Map<String, AbstractDashboardWidget> widgetMap = new HashMap<>();
    private MyData myData1 = new MyData();
    private MyData myData2 = new MyData();
    private boolean isConfigChanged = false;
    private boolean mode_24h;
    private NestedScrollView scrollView;
    private ProgressBar loading;
    private CircularProgressIndicator loadingSleep;
    private final Object metricJsonLock = new Object();
    private final AtomicBoolean pendingUploadFlushRunning = new AtomicBoolean(false);
    private final AtomicBoolean sleepSnapshotRunning = new AtomicBoolean(false);
    private final AtomicBoolean p5mUploadCheckRunning = new AtomicBoolean(false);
    private boolean p5mUploadGatePassed = false;

    // Queue-wait polling state — keeps overlay open until queued item is sent
    private static final int QUEUE_WAIT_MAX_POLLS = 40;   // 40 × 3s ≈ 2 menit
    private static final int QUEUE_WAIT_POLL_MS   = 3_000;
    private final Handler queueWaitHandler = new Handler(android.os.Looper.getMainLooper());
    private int      queueWaitPollCount    = 0;
    private String   queueWaitPhase        = null;  // "summary" | "detail" | null
    private GBDevice queueWaitDevice       = null;
    private String   queueWaitSummaryFingerprint = null;
    private String   queueWaitDetailFingerprint  = null;
    private String   queueWaitDetailData         = null;

    private RelativeLayout uploadProgressOverlay;
    private TextView uploadProgressTitle;
    private TextView uploadProgressText;
    private TextView uploadQueueStatus;
    private LocalStorage localStorage;

    private JSONArray jsonActivity = new JSONArray();
    private JSONArray jsonSleep = new JSONArray();
    private JSONArray jsonStress = new JSONArray();
    private JSONArray jsonSpo2 = new JSONArray();
    private JSONArray jsonHeartRateMax = new JSONArray();
    private JSONArray jsonHeartRateResting = new JSONArray();
    private JSONArray jsonHeartRateManual = new JSONArray();

    private static final class IngestChunk {
        private final String uploadId;
        private final int chunkIndex;
        private final int chunkCount;
        private final String payload;
        private final String queueKey;

        private IngestChunk(String uploadId, int chunkIndex, int chunkCount, String payload, String queueKey) {
            this.uploadId = uploadId;
            this.chunkIndex = chunkIndex;
            this.chunkCount = chunkCount;
            this.payload = payload;
            this.queueKey = queueKey;
        }
    }

    public static final String ACTION_CONFIG_CHANGE = "id.icapps.savera.activities.dashboardfragment.action.config_change";
    public static final String ACTION_PROFILE_CONTEXT_UPDATED = "id.icapps.savera.activities.dashboardfragment.action.profile_context_updated";
    private static final int SERVER_STATUS_LOCAL = 1;
    private static final int SERVER_STATUS_PUBLIC = 2;
    private static final int SERVER_STATUS_DISCONNECTED = 3;
    private static final int HEART_RATE_VALID_MIN = 1;
    private static final int HEART_RATE_VALID_MAX = 240;
    private static final long RECORDED_DATA_FETCH_COOLDOWN_MS = 60 * 1000L;
    private static final String APP_VERSION_STAMP = "Savera 9";
    private static boolean queueRestoreOnLaunch = false;
    private long lastRecordedDataFetchMs = 0L;
    private boolean recordedDataFetchPending = false;

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
                case ACTION_PROFILE_CONTEXT_UPDATED:
                    syncEmployeeContextFromLocalStorage();
                    showImage();
                    break;
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.my_dashboard, container, false);

        Prefs prefs = GBApplication.getPrefs();
        mode_24h = prefs.getBoolean("dashboard_widget_today_24h", true);

        textNama = view.findViewById(R.id.textNama);
        textNik = view.findViewById(R.id.textNik);
        textDepartemen = view.findViewById(R.id.textDepartemen);
        textMess = view.findViewById(R.id.textMess);
        textInfo = view.findViewById(R.id.textInfo);
        textInfoSubtitle = view.findViewById(R.id.textInfoSubtitle);
        sleepStatusCard = view.findViewById(R.id.sleepStatusCard);
        textSteps = view.findViewById(R.id.textSteps);
        textDistance = view.findViewById(R.id.textDistance);
        textActive = view.findViewById(R.id.textActive);
        textSleep = view.findViewById(R.id.textSleep);
        textSleepType = view.findViewById(R.id.textSleepType);
        textHeartRate = view.findViewById(R.id.textHeartRate);
        textCalories = view.findViewById(R.id.textCalories);
        textMoving = view.findViewById(R.id.textMoving);
        textStanding = view.findViewById(R.id.textStanding);
        textBloodOxygen = view.findViewById(R.id.textBloodOxygen);
        textBloodPressure = view.findViewById(R.id.textBloodPressure);
        textStress = view.findViewById(R.id.textStress);
        textPAI = view.findViewById(R.id.textPAI);
        textWeight = view.findViewById(R.id.textWeight);
        textVOMax = view.findViewById(R.id.textVOMax);

        textDate = view.findViewById(R.id.dashboard_date);
        textOperator = view.findViewById(R.id.textOperator);
        textDevice = view.findViewById(R.id.textDevice);
        textTime = view.findViewById(R.id.textTime);
        textToday = view.findViewById(R.id.textToday);
        textYesterday = view.findViewById(R.id.textYesterday);
        textRest = view.findViewById(R.id.textRest);
        textNotificationBadge = view.findViewById(R.id.textNotificationBadge);
        serverStatusIndicator = view.findViewById(R.id.viewServerStatusIndicator);

        scrollView = view.findViewById(R.id.my_scroll);

        localStorage = new LocalStorage(view.getContext());
        syncEmployeeContextFromLocalStorage();

        if (!localStorage.getShift().isEmpty() && !localStorage.getShift().isBlank()) {
            try {
                JSONObject jsonShift = new JSONObject(localStorage.getShift());
                if (jsonShift.has("id") && !jsonShift.getString("id").equals("null")) {
                    shiftId = jsonShift.getInt("id");
                }
            } catch (JSONException e) {
                LOG.warn("Could not restore shift from local storage", e);
            }
        }

        imageProfile_ = view.findViewById(R.id.imageProfile_);
        imageProfile = view.findViewById(R.id.imageProfile);
        imageProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                assert getActivity() != null;
                ((HomeActivity) getActivity()).changeViewPagerPostition(4);
            }
        });

        showImage();

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

        loading = view.findViewById(R.id.progressbar);
        loading.setProgress(0);
        loading.setIndeterminate(true);
        loading.setVisibility(View.GONE);

        loadingSleep = view.findViewById(R.id.loadingSleep);
        loadingSleep.setProgress(0);

        uploadProgressOverlay = view.findViewById(R.id.uploadProgressOverlay);
        uploadProgressTitle = view.findViewById(R.id.uploadProgressTitle);
        uploadProgressText = view.findViewById(R.id.uploadProgressText);
        uploadQueueStatus = view.findViewById(R.id.uploadQueueStatus);

        btnFit1Ya = view.findViewById(R.id.btnFit1Ya);
        btnFit2Ya = view.findViewById(R.id.btnFit2Ya);
        btnFit3Ya = view.findViewById(R.id.btnFit3Ya);
        btnFit1Tdk = view.findViewById(R.id.btnFit1Tdk);
        btnFit2Tdk = view.findViewById(R.id.btnFit2Tdk);
        btnFit3Tdk = view.findViewById(R.id.btnFit3Tdk);

        restoreFitToWorkStateForToday();
        applyFitToWorkButtonState();

        btnFit1Ya.setOnClickListener(v -> {
            isFit1 = 1; localStorage.setFit1(1);
            persistFitToWorkStateIfComplete();
            btnFit1Ya.setBackgroundColor(getResources().getColor(R.color.chart_deep_sleep_light));
            btnFit1Tdk.setBackgroundColor(getResources().getColor(R.color.chart_stress_relaxed));
        });
        btnFit2Ya.setOnClickListener(v -> {
            isFit2 = 1; localStorage.setFit2(1);
            persistFitToWorkStateIfComplete();
            btnFit2Ya.setBackgroundColor(getResources().getColor(R.color.chart_deep_sleep_light));
            btnFit2Tdk.setBackgroundColor(getResources().getColor(R.color.chart_stress_relaxed));
        });
        btnFit3Ya.setOnClickListener(v -> {
            isFit3 = 1; localStorage.setFit3(1);
            persistFitToWorkStateIfComplete();
            btnFit3Ya.setBackgroundColor(getResources().getColor(R.color.chart_deep_sleep_light));
            btnFit3Tdk.setBackgroundColor(getResources().getColor(R.color.chart_stress_relaxed));
        });
        btnFit1Tdk.setOnClickListener(v -> {
            isFit1 = 0; localStorage.setFit1(0);
            persistFitToWorkStateIfComplete();
            btnFit1Ya.setBackgroundColor(getResources().getColor(R.color.chart_stress_relaxed));
            btnFit1Tdk.setBackgroundColor(getResources().getColor(R.color.chart_deep_sleep_light));
        });
        btnFit2Tdk.setOnClickListener(v -> {
            isFit2 = 0; localStorage.setFit2(0);
            persistFitToWorkStateIfComplete();
            btnFit2Ya.setBackgroundColor(getResources().getColor(R.color.chart_stress_relaxed));
            btnFit2Tdk.setBackgroundColor(getResources().getColor(R.color.chart_deep_sleep_light));
        });
        btnFit3Tdk.setOnClickListener(v -> {
            isFit3 = 0; localStorage.setFit3(0);
            persistFitToWorkStateIfComplete();
            btnFit3Ya.setBackgroundColor(getResources().getColor(R.color.chart_stress_relaxed));
            btnFit3Tdk.setBackgroundColor(getResources().getColor(R.color.chart_deep_sleep_light));
        });

        btnNotification = view.findViewById(R.id.btnNotification);
        btnNotification.setOnClickListener(v -> startActivity(new Intent(getActivity(), NotificationActivity.class)));

        btnP5m = view.findViewById(R.id.btnP5m);
        btnP5m.setOnClickListener(v -> {
            assert getActivity() != null;
            ((HomeActivity) getActivity()).changeViewPagerPostition(8);
        });

        btnZona = view.findViewById(R.id.btnZona);
        btnZona.setOnClickListener(v -> {
            assert getActivity() != null;
            ((HomeActivity) getActivity()).changeViewPagerPostition(9);
        });

        btnSync = view.findViewById(R.id.btnSync);
        btnSync.setOnClickListener(this::sendSummary);

        btnReload = view.findViewById(R.id.btnReload);
        btnReload.setOnClickListener(v -> take_share_screenshot(requireActivity(), true));

        if (savedInstanceState != null && savedInstanceState.containsKey("dashboard_data") && myData1.isEmpty()) {
            myData1 = (MyData) savedInstanceState.getSerializable("dashboard_data");
        } else if (myData1.isEmpty()) {
            reloadPreferences();
        }

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        filterLocal.addAction(GBApplication.ACTION_NEW_DATA);
        filterLocal.addAction(ACTION_CONFIG_CHANGE);
        filterLocal.addAction(ACTION_PROFILE_CONTEXT_UPDATED);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mReceiver, filterLocal);

        getBanner(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        syncEmployeeContextFromLocalStorage();
        showImage();
        updateServerStatusIndicatorFromActiveRoute();
        restorePendingQueueOnColdStart();
        flushPendingUploads(false);
        refreshNotificationBadge();
        
        // Auto-detect noon reset time (12:00 PM) and trigger cache clear
        Calendar now_check = GregorianCalendar.getInstance();
        int currentHour = now_check.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now_check.get(Calendar.MINUTE);
        if (currentHour == 12 && currentMinute < 5) {
            // Around noon, force full refresh to clear cache
            fullRefresh();
        } else if (isConfigChanged) {
            isConfigChanged = false;
            fullRefresh();
        } else {
            // Always refresh data and redraw UI when resuming
            // This ensures cached 48-minute values don't persist
            refresh();
        }
    }

    private void refreshNotificationBadge() {
        if (!isAdded()) {
            return;
        }

        String url = getString(R.string.base_url) + "/notifications";
        Context context = getContext();
        if (context == null) {
            return;
        }
        new Thread(() -> {
            Http http = new Http(context, url);
            http.setMethod("get");
            http.setToken(true);
            http.setBypassCache(true);
            http.send();

            Activity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.runOnUiThread(() -> {
                int code = http.getStatusCode() == null ? 0 : http.getStatusCode();
                if (code != 200) {
                    showNotificationBadge(0);
                    updateServerStatusIndicator(SERVER_STATUS_DISCONNECTED);
                    return;
                }

                try {
                    JSONObject response = new JSONObject(http.getResponse());
                    JSONObject meta = response.optJSONObject("meta");
                    int unreadCount = meta != null ? meta.optInt("unread_count", 0) : 0;
                    showNotificationBadge(unreadCount);
                    updateServerStatusIndicatorFromActiveRoute();
                } catch (JSONException e) {
                    showNotificationBadge(0);
                    updateServerStatusIndicatorFromActiveRoute();
                }
            });
        }).start();
    }

    private void updateServerStatusIndicatorFromActiveRoute() {
        if (localStorage == null) {
            updateServerStatusIndicator(SERVER_STATUS_DISCONNECTED);
            return;
        }

        // Check actual network connectivity first
        Context context = getContext();
        if (context != null) {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                android.net.NetworkInfo netInfo = cm.getActiveNetworkInfo();
                if (netInfo == null || !netInfo.isConnected()) {
                    updateServerStatusIndicator(SERVER_STATUS_DISCONNECTED);
                    return;
                }
            }
        }

        String localBaseUrl = localStorage.getApiLocalUrl();
        String publicBaseUrl = localStorage.getApiPublicUrl();
        String activeBaseUrl = localStorage.getApiActiveBaseUrl();

        if (activeBaseUrl == null || activeBaseUrl.isEmpty()) {
            updateServerStatusIndicator(SERVER_STATUS_DISCONNECTED);
            return;
        }

        if (!localBaseUrl.isEmpty() && localBaseUrl.equalsIgnoreCase(activeBaseUrl)) {
            updateServerStatusIndicator(SERVER_STATUS_LOCAL);
            return;
        }

        if (!publicBaseUrl.isEmpty() && publicBaseUrl.equalsIgnoreCase(activeBaseUrl)) {
            updateServerStatusIndicator(SERVER_STATUS_PUBLIC);
            return;
        }

        updateServerStatusIndicator(SERVER_STATUS_PUBLIC);
    }

    private void updateServerStatusIndicator(int status) {
        if (serverStatusIndicator == null) {
            return;
        }

        int drawableRes;
        if (status == SERVER_STATUS_LOCAL) {
            drawableRes = R.drawable.bg_server_status_green;
        } else if (status == SERVER_STATUS_PUBLIC) {
            drawableRes = R.drawable.bg_server_status_blue;
        } else {
            drawableRes = R.drawable.bg_server_status_red;
        }

        serverStatusIndicator.setBackgroundResource(drawableRes);
    }

    private void syncEmployeeContextFromLocalStorage() {
        textNama.setText("-");
        textNik.setText("-");
        textDepartemen.setText("-");
        textMess.setText("-");

        String employeeJson = localStorage.getEmployee();
        if (employeeJson.isEmpty() || employeeJson.isBlank()) {
            return;
        }

        try {
            JSONObject jsonEmployee = new JSONObject(employeeJson);

            textNama.setText(getEmployeeField(jsonEmployee, "fullname", "-"));
            textNik.setText(getEmployeeField(jsonEmployee, "code", "-"));
            textDepartemen.setText(getEmployeeField(jsonEmployee, "department_name", "-"));
            textMess.setText(getEmployeeField(jsonEmployee, "mess_name", "-"));

            employeeId = jsonEmployee.optInt("id", employeeId);
            userId = jsonEmployee.optInt("user_id", userId);
            companyId = jsonEmployee.optInt("company_id", companyId);
            departmentId = jsonEmployee.optInt("department_id", departmentId);
            deviceId = jsonEmployee.optInt("device_id", deviceId);

            String profilePhoto = getEmployeeField(jsonEmployee, "photo", "");
            if (!profilePhoto.isEmpty()) {
                employeePhoto = profilePhoto;
            }
        } catch (JSONException e) {
            LOG.warn("Could not parse employee context from local storage", e);
        }
    }

    private String getEmployeeField(JSONObject employee, String field, String fallback) {
        if (employee == null) {
            return fallback;
        }

        String value = employee.optString(field, fallback);
        if (value == null) {
            return fallback;
        }

        String normalized = value.trim();
        if (normalized.isEmpty() || "null".equalsIgnoreCase(normalized)) {
            return fallback;
        }

        return normalized;
    }

    private String extractApiMessage(String responseBody, String fallback) {
        if (responseBody == null) {
            return fallback;
        }

        String normalizedBody = responseBody.trim();
        if (normalizedBody.isEmpty()) {
            return fallback;
        }

        try {
            JSONObject response = new JSONObject(normalizedBody);
            String message = response.optString("message", "").trim();
            return message.isEmpty() ? fallback : message;
        } catch (JSONException ignored) {
            return fallback;
        }
    }

    private void showNotificationBadge(int unreadCount) {
        if (textNotificationBadge == null) {
            return;
        }

        if (unreadCount <= 0) {
            textNotificationBadge.setVisibility(View.GONE);
            return;
        }

        textNotificationBadge.setVisibility(View.VISIBLE);
        textNotificationBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mReceiver);
        queueWaitHandler.removeCallbacksAndMessages(null);
        queueWaitPhase = null;
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
        
        // Clear memory cache of sleep/activity data
        clearDataCache();
        
        reloadPreferences();
        getDevice();
        
        // Generate sample data for developer mode
        if (isDeveloperMode()) {
            populateDeveloperSampleData();
        }
        
        assert getActivity() != null;
        ((HomeActivity) getActivity()).setDateNow(now);

        if (((HomeActivity) getActivity()).getScrollDown()) {
            ((HomeActivity) getActivity()).setScrollDown(false);
            scrollView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
            }, 1000);
        }
        
        // Redraw UI with fresh data after cache clear
        draw();
    }

    private void clearDataCache() {
        // Clear JSON cache variables
        jsonActivity = new JSONArray();
        jsonSleep = new JSONArray();
        jsonStress = new JSONArray();
        jsonSpo2 = new JSONArray();
        jsonHeartRateMax = new JSONArray();
        jsonHeartRateResting = new JSONArray();
        jsonHeartRateManual = new JSONArray();
    }

    private void reloadPreferences() {
        Prefs prefs = GBApplication.getPrefs();

        myData1.showAllDevices = prefs.getBoolean("dashboard_devices_all", true);
        myData1.showDeviceList = prefs.getStringSet("dashboard_devices_multiselect", new HashSet<>());
        myData1.hrIntervalSecs = prefs.getInt("dashboard_widget_today_hr_interval", 1) * 60;
        myData1.timeTo = (int) (now.getTimeInMillis() / 1000);
        myData1.timeFrom = DateTimeUtils.shiftDays(myData1.timeTo, -1);

        myData2.showAllDevices = myData1.showAllDevices;
        myData2.timeTo = myData1.timeTo - (24 * 3600);
        myData2.timeFrom = myData1.timeFrom - (24 * 3600);
    }

    @SuppressLint("SetTextI18n")
    private void draw() {
        Calendar today = GregorianCalendar.getInstance();
        int hour = today.get(Calendar.HOUR_OF_DAY);

        textDate.setText(new SimpleDateFormat("E, dd MMM yyyy", Locale.getDefault()).format(now.getTime()));
        if (DateTimeUtils.isSameDay(today, now)) {
            arrowRight.setAlpha(0.5f);
            // btnSync.setEnabled(true);
        } else {
            arrowRight.setAlpha(1);
            // btnSync.setEnabled(false);
        }

        final long totalActiveMinutes = myData1.getActiveMinutesTotal();
        final String valueActive = String.format(
                Locale.ROOT,
                "%d:%02d",
                (int) Math.floor(totalActiveMinutes / 60f),
                (int) (totalActiveMinutes % 60f)
        );
        textActive.setText(valueActive);

        final long totalSleepMinutes = myData1.getSleepMinutesTotal();
        final long totalWearableSleepMinutes = getWearableSleepTotalMinutes(myData1);
        localStorage.setSleepMinutes(totalSleepMinutes);
        final String valueSleep = String.format(
                Locale.ROOT,
                "%d:%02d",
                (int) Math.floor(totalSleepMinutes / 60f),
                (int) (totalSleepMinutes % 60f)
        );
        textSleep.setText(valueSleep);
        textSleepType.setText("Total Tidur Efektif\n(" + myData1.sleepType + ")");
        loadingSleep.setProgress((int) Math.floor(totalSleepMinutes / 60f));

        textSteps.setText(String.valueOf(myData1.getStepsTotal()));
        textDistance.setText(FormatUtils.getFormattedDistanceLabel(myData1.getDistanceTotal()));

        textHeartRate.setText(String.format("%s bpm", myData1.getHeartRate()));
        textCalories.setText(String.format("%s kcal", myData1.getCalories()));
        textMoving.setText("-");
        textStanding.setText("-");
        textBloodOxygen.setText(String.format("%s%%", myData1.getBloodOxygen()));
        textBloodPressure.setText("-");
        textStress.setText(String.valueOf(myData1.getStress()));
        textPAI.setText("-");
        textWeight.setText("-");
        textVOMax.setText("-");

        int sleepStatusColor = getSleepStatusColor(totalSleepMinutes);
        textInfo.setText(getSleepStatusLabel(totalSleepMinutes));
        textInfo.setTextColor(sleepStatusColor);
        applySleepStatusCardStyle(totalSleepMinutes);
        loadingSleep.setIndicatorColor(sleepStatusColor);

        final String tsFrom = new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(new Date(myData1.sleepFrom * 1000L));
        final String tsTo = new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(new Date(myData1.sleepTo * 1000L));
        textTime.setText(tsFrom + " - " + tsTo);

        final String sleepToday = String.format(
                Locale.ROOT,
                "%d jam, %02d menit",
                (int) Math.floor(myData1.sleepToday / 60f),
                (int) (myData1.sleepToday % 60f)
        );
        textToday.setText(sleepToday);

        final String sleepYesterday = String.format(
                Locale.ROOT,
                "%d jam, %02d menit",
                (int) Math.floor(myData1.sleepYesterday / 60f),
                (int) (myData1.sleepYesterday % 60f)
        );
        textYesterday.setText(sleepYesterday);

        final String sleepWearable = String.format(
                Locale.ROOT,
                "%d jam, %02d menit",
                (int) Math.floor(totalWearableSleepMinutes / 60f),
                (int) (totalWearableSleepMinutes % 60f)
        );
        textRest.setText(sleepWearable);

        sendSleepSnapshotIfNeeded();
    }

    private long getWearableSleepTotalMinutes(MyData data) {
        if (data == null) {
            return 0;
        }
        return data.sleepWearableTotalMinutes > 0 ? data.sleepWearableTotalMinutes : data.sleepTotalMinutes;
    }

    private String getSleepStatusLabel(long effectiveSleepMinutes) {
        if (effectiveSleepMinutes < 270) {
            return "Langsung dipulangkan";
        }
        if (effectiveSleepMinutes < 300) {
            return "Istirahat minimal 2 jam (jam savera)";
        }
        if (effectiveSleepMinutes < 330) {
            return "Istirahat minimal 1 jam (jam savera)";
        }
        if (effectiveSleepMinutes < 360) {
            return "Dapat bekerja";
        }
        return "Langsung bekerja";
    }

    private int getSleepStatusColor(long effectiveSleepMinutes) {
        if (effectiveSleepMinutes < 270) {
            return getResources().getColor(R.color.hrv_status_char_line_color);
        }
        if (effectiveSleepMinutes < 330) {
            return getResources().getColor(R.color.hrv_status_unbalanced);
        }
        return getResources().getColor(R.color.hrv_status_balanced);
    }

    private void applySleepStatusCardStyle(long effectiveSleepMinutes) {
        if (sleepStatusCard == null) {
            return;
        }

        if (effectiveSleepMinutes < 270) {
            sleepStatusCard.setBackgroundResource(R.drawable.bg_sleep_status_red);
            if (textInfoSubtitle != null) {
                textInfoSubtitle.setTextColor(Color.parseColor("#7F1D1D"));
            }
            return;
        }

        if (effectiveSleepMinutes < 330) {
            sleepStatusCard.setBackgroundResource(R.drawable.bg_sleep_status_yellow);
            if (textInfoSubtitle != null) {
                textInfoSubtitle.setTextColor(Color.parseColor("#92400E"));
            }
            return;
        }

        sleepStatusCard.setBackgroundResource(R.drawable.bg_sleep_status_green);
        if (textInfoSubtitle != null) {
            textInfoSubtitle.setTextColor(Color.parseColor("#166534"));
        }
    }

    @SuppressLint("SetTextI18n")
    private void getDevice() {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        GBDevice activeDevice = resolvePrimaryWearableDevice();
        if (activeDevice != null) {
            triggerRecordedDataFetch(activeDevice, false);
            textDevice.setText(": " + activeDevice.getName());
            textOperator.setText(": -");

            employeeId = 0;
            userId = 0;
            companyId = 0;
            departmentId = 0;
            shiftId = 0;
            deviceId = 0;

                String macAddress = activeDevice.getAddress() == null
                    ? ""
                    : activeDevice.getAddress().trim().toUpperCase(Locale.ROOT);
                String url = getString(R.string.base_url) + "/device/" + macAddress;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Http http = new Http(requireActivity(), url);
                    http.setMethod("get");
                    http.setToken(true);
                    http.setCacheTtlSeconds(900);
                    http.send();

                    Activity activity = getActivity();
                    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                        activity.runOnUiThread(new Runnable() {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public void run() {
                                Integer code = http.getStatusCode();
                                if (code == 200) {
                                    try {
                                        JSONObject response = new JSONObject(http.getResponse());
                                        deviceId = response.optInt("id", 0);
                                        if (response.has("employee") && !response.get("employee").toString().equals("null")) {
                                            JSONObject jsonEmployee = new JSONObject(response.get("employee").toString());
                                            String fullName = getEmployeeField(jsonEmployee, "fullname", "-");
                                            String employeeCode = getEmployeeField(jsonEmployee, "code", "-");
                                            String departmentName = getEmployeeField(jsonEmployee, "department_name", "-");
                                            String messName = getEmployeeField(jsonEmployee, "mess_name", "-");
                                            String profilePhoto = getEmployeeField(jsonEmployee, "photo", "");

                                            textOperator.setText(": " + fullName);
                                            textNama.setText(fullName);
                                            textNik.setText(employeeCode);
                                            textDepartemen.setText(departmentName);
                                            textMess.setText(messName);
                                            employeeId = jsonEmployee.optInt("id", employeeId);
                                            userId = jsonEmployee.optInt("user_id", userId);
                                            companyId = jsonEmployee.optInt("company_id", companyId);
                                            departmentId = jsonEmployee.optInt("department_id", departmentId);
                                            if (!profilePhoto.isEmpty()) {
                                                employeePhoto = profilePhoto;
                                            }
                                            activeDevice.setAlias(fullName);
                                            localStorage.setEmployee(response.get("employee").toString());

                                            showImage();
                                        }
                                        if (response.has("shift") && !response.get("shift").toString().equals("null")) {
                                            JSONObject jsonShift = response.optJSONObject("shift");
                                            if (jsonShift == null) {
                                                jsonShift = new JSONObject(response.get("shift").toString());
                                            }
                                            shiftId = jsonShift.optInt("id", shiftId);
                                        }
                                        localStorage.markUserContextSynced();
                                        draw();
                                    } catch (JSONException e) {
                                        LOG.warn("Failed parsing device payload", e);
                                    }
                                } else if (code == 422 || code == 401 || code == 404) {
                                    String msg = extractApiMessage(http.getResponse(), "Gagal mengambil data device.");
                                    toast(requireActivity(), msg, Toast.LENGTH_LONG, GB.ERROR);
                                } else {
                                    toast(requireActivity(), http.getErrorMessage("Gagal mengambil data device registration auth key."), Toast.LENGTH_LONG, GB.ERROR);
                                    assert getActivity() != null;
                                    ((HomeActivity) getActivity()).changeViewPagerPostition(2);
                                }
                            }
                        });
                    }
                }
            }).start();
        } else {
            toast(requireActivity(), "Device not found", Toast.LENGTH_SHORT, GB.ERROR);
        }
    }

    private GBDevice resolvePrimaryWearableDevice() {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        GBDevice selectedInitialized = null;
        GBDevice anyInitialized = null;
        GBDevice selectedTracking = null;

        for (GBDevice device : devices) {
            if (device == null || device.getDeviceCoordinator() == null) {
                continue;
            }

            boolean selected = myData1.showAllDevices
                    || myData1.showDeviceList == null
                    || myData1.showDeviceList.contains(device.getAddress());
            boolean supportsActivity = device.getDeviceCoordinator().supportsActivityTracking();

            if (selected && supportsActivity && device.getState() == GBDevice.State.INITIALIZED) {
                selectedInitialized = device;
                break;
            }

            if (anyInitialized == null && device.getState() == GBDevice.State.INITIALIZED) {
                anyInitialized = device;
            }

            if (selectedTracking == null && selected && supportsActivity) {
                selectedTracking = device;
            }
        }

        if (selectedInitialized != null) {
            return selectedInitialized;
        }
        if (anyInitialized != null) {
            return anyInitialized;
        }
        return selectedTracking;
    }

    private void getBanner(View view) {
        final ArrayList<SlideModel> imageList = new ArrayList<>();
        final ImageSlider bannerSlider = view.findViewById(R.id.bannerSlider);
        final RelativeLayout bannerLayout = view.findViewById(R.id.bannerLayout);

        String url = getString(R.string.base_url) + "/banner";

        new Thread(new Runnable() {
            @Override
            public void run() {
                Http http = new Http(requireActivity(), url);
                http.setMethod("get");
                http.setToken(true);
                http.setCacheTtlSeconds(3600);
                http.send();

                Activity activity = getActivity();
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    activity.runOnUiThread(new Runnable() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            Integer code = http.getStatusCode();
                            if (code == 200) {
                                try {
                                    JSONArray response = new JSONArray(http.getResponse());
                                    if (response.length() > 0) {
                                        for (int i = 0; i < response.length(); i++) {
                                            imageList.add(new SlideModel(response.get(i).toString(), ScaleTypes.FIT));
                                        }
                                        bannerLayout.setVisibility(View.VISIBLE);
                                        bannerSlider.setImageList(imageList);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else if (code == 404) {
                                // Banner endpoint is optional on some deployments.
                                bannerLayout.setVisibility(View.GONE);
                                LOG.warn("Banner endpoint not available (404), hiding banner section");
                            } else if (code == 422 || code == 401) {
                                String msg = extractApiMessage(http.getResponse(), "Gagal mengambil banner.");
                                toast(requireActivity(), msg, Toast.LENGTH_LONG, GB.ERROR);
                            } else {
                                toast(requireActivity(), http.getErrorMessage("Gagal mengambil banner."), Toast.LENGTH_LONG, GB.ERROR);
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private void dataActivity(GBDevice device, int tsFrom, int tsTo) {
        jsonActivity = new JSONArray();
        jsonSleep = new JSONArray();
        jsonStress = new JSONArray();
        jsonSpo2 = new JSONArray();
        jsonHeartRateMax = new JSONArray();
        jsonHeartRateResting = new JSONArray();
        jsonHeartRateManual = new JSONArray();

        try (DBHandler db = GBApplication.acquireDB()) {
            final DeviceCoordinator coordinator = device.getDeviceCoordinator();

            // Returns the sample provider for the device being supported.
            final SampleProvider<? extends ActivitySample> getProvider = coordinator.getSampleProvider(device, db.getDaoSession());
            // final List<? extends ActivitySample> getActivity = getProvider.getActivitySamples(tsFrom, tsTo);
            final List<? extends ActivitySample> rawActivity = getProvider.getAllActivitySamples(tsFrom, tsTo);
            final List<ActivitySample> getActivity = new ArrayList<>(rawActivity);
            getActivity.sort(Comparator.comparingInt(ActivitySample::getTimestamp));
            final TreeMap<Long, Integer> stressByTs = new TreeMap<>();
            final TreeMap<Long, Integer> spo2ByTs = new TreeMap<>();

            for (ActivitySample row : getActivity) {
                ActivityKind kind = row.getProvider() != null ? row.getKind() : ActivityKind.NOT_MEASURED;
                int rawKind = row.getProvider() != null ? row.getRawKind() : ActivityKind.NOT_MEASURED.getCode();
                float intensity = row.getProvider() != null ? row.getIntensity() : ActivitySample.NOT_MEASURED;
                int rawIntensity = row.getProvider() != null ? row.getRawIntensity() : ActivitySample.NOT_MEASURED;

                String providerName = "unknown";
                Object provider = row.getProvider();
                if (provider != null) {
                    String[] provider1 = provider.toString().split("\\.");
                    String providerTail = provider1[provider1.length - 1];
                    String[] provider2 = providerTail.split("@");
                    providerName = provider2[0];
                }

                JSONObject obj = new JSONObject();
                obj.put("timestamp", row.getTimestamp());
                obj.put("kind", kind);
                obj.put("rawKind", rawKind);
                obj.put("intensity", intensity);
                obj.put("rawIntensity", rawIntensity);
                obj.put("steps", row.getSteps());
                obj.put("distanceCm", row.getDistanceCm());
                obj.put("activeCalories", row.getActiveCalories());
                int hr = row.getHeartRate();
                obj.put("heartRate", hr);
                obj.put("provider", providerName);
                jsonActivity.put(obj);

                // Extract stress and spo2 into separate arrays (not embedded in activity)
                int sampleStress = 0;
                int sampleSpo2 = 0;
                try {
                    java.lang.reflect.Method mStress = row.getClass().getMethod("getStress");
                    Object stressVal = mStress.invoke(row);
                    if (stressVal instanceof Number) sampleStress = ((Number) stressVal).intValue();
                } catch (Exception ignored) {}
                try {
                    java.lang.reflect.Method mSpo2 = row.getClass().getMethod("getSpo2");
                    Object spo2Val = mSpo2.invoke(row);
                    if (spo2Val instanceof Number) sampleSpo2 = ((Number) spo2Val).intValue();
                } catch (Exception ignored) {}

                // Keep raw per-minute series aligned with activity timeline, including zeros.
                long ts = row.getTimestamp();
                stressByTs.put(ts, sampleStress);
                spo2ByTs.put(ts, sampleSpo2);
            }

            if (coordinator.supportsSleepMeasurement()) {
                final SleepAnalysis sleepAnalysis = new SleepAnalysis();
                final List<SleepAnalysis.SleepSession> getSleepSession = sleepAnalysis.calculateSleepSessions(getActivity);

                for (SleepAnalysis.SleepSession row : getSleepSession) {
                    JSONObject obj = new JSONObject();
                    long sleepStart = row.getSleepStart().getTime() / 1000;
                    long sleepEnd = row.getSleepEnd().getTime() / 1000;
                    long interval = Math.max(0, sleepEnd - sleepStart);
                    long light = Math.max(0, row.getLightSleepDuration());
                    long deep = Math.max(0, row.getDeepSleepDuration());
                    long rem = Math.max(0, row.getRemSleepDuration());
                    long awake = Math.max(0, row.getAwakeSleepDuration());
                    long sleepStageTotal = light + deep + rem;
                    if (interval > 0 && sleepStageTotal > interval) {
                        double ratio = (double) interval / (double) sleepStageTotal;
                        light = Math.round(light * ratio);
                        deep = Math.round(deep * ratio);
                        rem = Math.round(rem * ratio);
                        long overflow = (light + deep + rem) - interval;
                        if (overflow > 0) {
                            light = Math.max(0, light - overflow);
                        }
                        awake = 0;
                    } else if (interval > 0 && sleepStageTotal + awake > interval) {
                        awake = Math.max(0, interval - sleepStageTotal);
                    }
                    obj.put("sleepStart", sleepStart);
                    obj.put("sleepEnd", sleepEnd);
                    obj.put("lightSleepDuration", light);
                    obj.put("deepSleepDuration", deep);
                    obj.put("remSleepDuration", rem);
                    obj.put("totalSleepDuration", (light + deep + rem + awake));
                    obj.put("awakeSleepDuration", awake);
                    jsonSleep.put(obj);
                }
            }
            if (coordinator.supportsStressMeasurement()) {
                // Returns the sample provider for stress data, for the device being supported.
                final TimeSampleProvider<? extends StressSample> getStressProvider = coordinator.getStressSampleProvider(device, db.getDaoSession());
                final List<? extends StressSample> getStress = getStressProvider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);
                LOG.warn("[SAVERA_DEBUG] stress samples count=" + getStress.size() + " tsFrom=" + tsFrom + " tsTo=" + tsTo);

                for (StressSample row : getStress) {
                    // TimeSampleProvider timestamps are in milliseconds; convert to seconds
                    long ts = row.getTimestamp() / 1000L;
                    int val = row.getStress();
                    Integer prev = stressByTs.get(ts);
                    if (prev == null || val > prev) {
                        stressByTs.put(ts, val);
                    }
                    // Build jsonStress directly from dedicated provider (like Saverax)
                    JSONObject stressObj = new JSONObject();
                    stressObj.put("timestamp", ts);
                    stressObj.put("type", 0);
                    stressObj.put("stress", val);
                    jsonStress.put(stressObj);
                }
            }
            if (coordinator.supportsSpo2(device)) {
                // Returns the sample provider for SpO2 data, for the device being supported.
                final TimeSampleProvider<? extends Spo2Sample> getSpo2Provider = coordinator.getSpo2SampleProvider(device, db.getDaoSession());
                final List<? extends Spo2Sample> getSpo2 = getSpo2Provider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);
                LOG.warn("[SAVERA_DEBUG] spo2 samples count=" + getSpo2.size() + " tsFrom=" + tsFrom + " tsTo=" + tsTo);

                for (Spo2Sample row : getSpo2) {
                    // TimeSampleProvider timestamps are in milliseconds; convert to seconds
                    long ts = row.getTimestamp() / 1000L;
                    int val = row.getSpo2();
                    Integer prev = spo2ByTs.get(ts);
                    if (prev == null || val > prev) {
                        spo2ByTs.put(ts, val);
                    }
                    // Build jsonSpo2 directly from dedicated provider (like Saverax)
                    JSONObject spo2Obj = new JSONObject();
                    spo2Obj.put("timestamp", ts);
                    spo2Obj.put("type", 0);
                    spo2Obj.put("spo2", val);
                    jsonSpo2.put(spo2Obj);
                }
            }
            if (coordinator.supportsStepCounter()) {
                //
            }
            if (coordinator.supportsSpeedzones()) {
                //
            }
            if (coordinator.supportsRealtimeData()) {
                //
            }
            if (coordinator.supportsHeartRateStats()) {
                // Returns the sample provider for max HR data, for the device being supported.
                final TimeSampleProvider<? extends HeartRateSample> getHeartRateMaxProvider = coordinator.getHeartRateMaxSampleProvider(device, db.getDaoSession());
                final List<? extends HeartRateSample> getHeartRateMax = getHeartRateMaxProvider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);

                for (HeartRateSample row : getHeartRateMax) {
                    int hr = row.getHeartRate();
                    if (!isValidHeartRateForUpload(hr)) {
                        continue;
                    }
                    JSONObject obj = new JSONObject();
                    obj.put("timestamp", row.getTimestamp() / 1000L);
                    obj.put("hr", hr);
                    obj.put("hr_valid", true);
                    jsonHeartRateMax.put(obj);
                }

                // Returns the sample provider for resting HR data, for the device being supported.
                final TimeSampleProvider<? extends HeartRateSample> getHeartRateRestingProvider = coordinator.getHeartRateRestingSampleProvider(device, db.getDaoSession());
                final List<? extends HeartRateSample> getHeartRateResting = getHeartRateRestingProvider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);

                for (HeartRateSample row : getHeartRateResting) {
                    int hr = row.getHeartRate();
                    if (!isValidHeartRateForUpload(hr)) {
                        continue;
                    }
                    JSONObject obj = new JSONObject();
                    obj.put("timestamp", row.getTimestamp() / 1000L);
                    obj.put("hr", hr);
                    obj.put("hr_valid", true);
                    jsonHeartRateResting.put(obj);
                }

                // Returns the sample provider for manual HR data, for the device being supported.
                final TimeSampleProvider<? extends HeartRateSample> getHeartRateManualProvider = coordinator.getHeartRateManualSampleProvider(device, db.getDaoSession());
                final List<? extends HeartRateSample> getHeartRateManual = getHeartRateManualProvider.getAllSamples(tsFrom * 1000L, tsTo * 1000L);

                for (HeartRateSample row : getHeartRateManual) {
                    int hr = row.getHeartRate();
                    if (!isValidHeartRateForUpload(hr)) {
                        continue;
                    }
                    JSONObject obj = new JSONObject();
                    obj.put("timestamp", row.getTimestamp() / 1000L);
                    obj.put("hr", hr);
                    obj.put("hr_valid", true);
                    jsonHeartRateManual.put(obj);
                }
            }

            // Build aligned per-minute stress/spo2 series using activity timestamps.
            JSONArray alignedStress = new JSONArray();
            JSONArray alignedSpo2 = new JSONArray();

            if (!getActivity.isEmpty()) {
                for (ActivitySample row : getActivity) {
                    long ts = row.getTimestamp();

                    JSONObject stressObj = new JSONObject();
                    stressObj.put("timestamp", ts);
                    stressObj.put("type", 0);
                    stressObj.put("stress", stressByTs.getOrDefault(ts, 0));
                    alignedStress.put(stressObj);

                    JSONObject spo2Obj = new JSONObject();
                    spo2Obj.put("timestamp", ts);
                    spo2Obj.put("type", 0);
                    spo2Obj.put("spo2", spo2ByTs.getOrDefault(ts, 0));
                    alignedSpo2.put(spo2Obj);
                }
            } else {
                for (Map.Entry<Long, Integer> entry : stressByTs.entrySet()) {
                    JSONObject stressObj = new JSONObject();
                    stressObj.put("timestamp", entry.getKey());
                    stressObj.put("type", 0);
                    stressObj.put("stress", entry.getValue());
                    alignedStress.put(stressObj);
                }
                for (Map.Entry<Long, Integer> entry : spo2ByTs.entrySet()) {
                    JSONObject spo2Obj = new JSONObject();
                    spo2Obj.put("timestamp", entry.getKey());
                    spo2Obj.put("type", 0);
                    spo2Obj.put("spo2", entry.getValue());
                    alignedSpo2.put(spo2Obj);
                }
            }

            // Final normalization guard: ascending order, no duplicate timestamps.
            // Use dedicated provider data directly (like Saverax); fall back to aligned if empty.
            LOG.warn("[SAVERA_DEBUG] jsonStress.length=" + jsonStress.length() + " jsonSpo2.length=" + jsonSpo2.length() + " alignedStress.length=" + alignedStress.length());
            if (jsonStress.length() == 0) {
                jsonStress = normalizeSeriesByTimestamp(alignedStress, "stress");
            } else {
                jsonStress = normalizeSeriesByTimestamp(jsonStress, "stress");
            }
            if (jsonSpo2.length() == 0) {
                jsonSpo2 = normalizeSeriesByTimestamp(alignedSpo2, "spo2");
            } else {
                jsonSpo2 = normalizeSeriesByTimestamp(jsonSpo2, "spo2");
            }
        } catch (Exception e) {
            LOG.warn("Could not calculate total distance: ", e);
        }
    }

    private JSONArray normalizeSeriesByTimestamp(JSONArray source, String valueKey) {
        TreeMap<Long, JSONObject> byTimestamp = new TreeMap<>();

        for (int i = 0; i < source.length(); i++) {
            JSONObject item = source.optJSONObject(i);
            if (item == null) {
                continue;
            }

            long ts = item.optLong("timestamp", Long.MIN_VALUE);
            if (ts == Long.MIN_VALUE) {
                continue;
            }

            if (("stress".equals(valueKey) || "spo2".equals(valueKey)) && item.optInt(valueKey, 0) <= 0) {
                continue;
            }

            JSONObject existing = byTimestamp.get(ts);
            if (existing == null) {
                byTimestamp.put(ts, item);
                continue;
            }

            int existingValue = existing.optInt(valueKey, 0);
            int currentValue = item.optInt(valueKey, 0);

            // Prefer non-zero readings and keep the larger value when duplicates occur.
            if (currentValue > existingValue) {
                byTimestamp.put(ts, item);
            }
        }

        JSONArray normalized = new JSONArray();
        for (JSONObject item : byTimestamp.values()) {
            normalized.put(item);
        }

        return normalized;
    }

    private boolean isValidHeartRateForUpload(int hr) {
        return hr >= HEART_RATE_VALID_MIN && hr <= HEART_RATE_VALID_MAX;
    }

    private String getAppVersionStamp() {
        return APP_VERSION_STAMP;
    }

    private void sendDetail(GBDevice device, String data, boolean fromQueue, boolean openTicketOnSuccess) {
        btnSync.setEnabled(false);
        loading.setVisibility(View.VISIBLE);

        String url = getString(R.string.base_url) + "/detail";
        Context context = getContext();
        if (context == null) {
            btnSync.setEnabled(true);
            loading.setVisibility(View.GONE);
            return;
        }
        
        // Developer mode: simulate successful upload without hitting server
        if (isDeveloperMode()) {
            Activity activity = getActivity();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                activity.runOnUiThread(() -> {
                    // Simulate delay for realistic feel
                    new Handler().postDelayed(() -> {
                        updateUploadProgress("Upload selesai!", 2, 2);
                        toast(requireActivity(), "✅ [DEV] Success send activity details", Toast.LENGTH_SHORT, GB.INFO);
                        
                        // Small delay before opening ticket
                        new Handler().postDelayed(() -> {
                            showUploadProgressOverlay(false);
                            btnSync.setEnabled(true);
                            loading.setVisibility(View.GONE);
                            
                            if (openTicketOnSuccess) {
                                startActivity(new Intent(getActivity(), TicketActivity.class));
                            }
                        }, 800);
                    }, 500);
                });
            }
            return;
        }
        
        new Thread(() -> {
            Http http = new Http(context, url);
            http.setMethod("post");
            http.setToken(true);
            http.setHeader("X-App-Version", getAppVersionStamp());
            http.setData(data);
            http.send();

            Activity activity = getActivity();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                activity.runOnUiThread(() -> {
                    Integer code = http.getStatusCode();
                    if (code == 200) {
                        try {
                            new JSONObject(http.getResponse());
                            updateUploadProgress("Upload selesai!", 2, 2);
                            toast(requireActivity(), "Success send activity details", Toast.LENGTH_SHORT, GB.INFO);
                            refreshUploadNotificationState();
                            sendFitToWork();
                            showUploadProgressOverlay(false);
                            btnSync.setEnabled(true);
                            loading.setVisibility(View.GONE);
                            if (openTicketOnSuccess) {
                                startActivity(new Intent(getActivity(), TicketActivity.class));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else if (code == 422 || code == 401 || code == 404) {
                        String msg = extractApiMessage(http.getResponse(), "Gagal mengirim detail aktivitas.");
                        toast(requireActivity(), msg, Toast.LENGTH_LONG, GB.ERROR);
                        showUploadProgressOverlay(false);
                        btnSync.setEnabled(true);
                        loading.setVisibility(View.GONE);
                    } else {
                        if (!fromQueue && shouldQueueUpload(code)) {
                            String detailKey = detailQueueKey(device);
                            String detailFp  = PendingUploadQueue.fingerprint("detail", detailKey);
                            queuePendingUpload("detail", url, data, detailKey);
                            showUploadPendingNotification();
                            // Keep overlay open, poll until detail leaves queue
                            startQueueWait("detail", device, null, detailFp, data);
                        } else {
                            showUploadPendingNotification();
                            toast(requireActivity(), http.getErrorMessage("Gagal mengirim detail aktivitas. Data disimpan di antrian upload."), Toast.LENGTH_LONG, GB.ERROR);
                            showUploadProgressOverlay(false);
                            btnSync.setEnabled(true);
                            loading.setVisibility(View.GONE);
                        }
                    }
                });
            }
        }).start();
    }

    private void sendFitToWork() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        if (hasFitToWorkBeenSentToday()) {
            LOG.info("Fit-to-work already sent today, skipping duplicate POST");
            return;
        }

        String url = getString(R.string.base_url) + "/mobile/fit-to-work";
        JSONObject params = new JSONObject();
        try {
            if (lastSummaryId > 0) {
                params.put("summary_id", lastSummaryId);
            } else {
                if (userId > 0) {
                    params.put("user_id", userId);
                }
                if (employeeId > 0) {
                    params.put("employee_id", employeeId);
                }
                if (companyId > 0) {
                    params.put("company_id", companyId);
                }
                params.put("send_date", new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date()));
            }
            params.put("fit_to_work_q1", isFit1);
            params.put("fit_to_work_q2", isFit2);
            params.put("fit_to_work_q3", isFit3);
            params.put("fit_to_work_submitted_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date()));
        } catch (JSONException e) {
            LOG.warn("Failed building fit-to-work payload", e);
            return;
        }

        String payload = params.toString();
        new Thread(() -> {
            try {
                Http http = new Http(context, url);
                http.setMethod("post");
                http.setToken(true);
                http.setHeader("X-App-Version", getAppVersionStamp());
                http.setData(payload);
                http.send();

                Integer code = http.getStatusCode();
                if (code == null || (code != 200 && code != 201)) {
                    LOG.warn("Fit-to-work POST failed with code: {}", code);
                } else if (localStorage != null) {
                    localStorage.setFitToWorkSentDate(todayStorageKey());
                }
            } catch (Exception e) {
                LOG.warn("Fit-to-work POST error", e);
            }
        }).start();
    }

    private void restoreFitToWorkStateForToday() {
        isFit1 = -1;
        isFit2 = -1;
        isFit3 = -1;
        if (localStorage == null) {
            return;
        }

        int storedFit1 = localStorage.getFit1();
        int storedFit2 = localStorage.getFit2();
        int storedFit3 = localStorage.getFit3();
        if (!isValidFitAnswer(storedFit1) || !isValidFitAnswer(storedFit2) || !isValidFitAnswer(storedFit3)) {
            return;
        }

        String today = todayStorageKey();
        String storedDate = localStorage.getFitToWorkDate();
        if (today.equals(storedDate)) {
            isFit1 = storedFit1;
            isFit2 = storedFit2;
            isFit3 = storedFit3;
            return;
        }

        // Do not reuse answers without today's date. Operators must answer all
        // Fit To Work questions once per day before upload.
    }

    private void applyFitToWorkButtonState() {
        int colorDefault = getResources().getColor(R.color.chart_stress_relaxed);
        int colorSelected = getResources().getColor(R.color.chart_deep_sleep_light);

        btnFit1Ya.setBackgroundColor(isFit1 == 1 ? colorSelected : colorDefault);
        btnFit1Tdk.setBackgroundColor(isFit1 == 0 ? colorSelected : colorDefault);
        btnFit2Ya.setBackgroundColor(isFit2 == 1 ? colorSelected : colorDefault);
        btnFit2Tdk.setBackgroundColor(isFit2 == 0 ? colorSelected : colorDefault);
        btnFit3Ya.setBackgroundColor(isFit3 == 1 ? colorSelected : colorDefault);
        btnFit3Tdk.setBackgroundColor(isFit3 == 0 ? colorSelected : colorDefault);
    }

    private void persistFitToWorkStateIfComplete() {
        if (localStorage == null || !isFitToWorkComplete()) {
            return;
        }
        localStorage.setFitToWorkDate(todayStorageKey());
    }

    private boolean restoreAndCheckFitToWorkForToday() {
        if (isFitToWorkComplete() && todayStorageKey().equals(localStorage.getFitToWorkDate())) {
            return true;
        }
        restoreFitToWorkStateForToday();
        applyFitToWorkButtonState();
        return isFitToWorkComplete() && todayStorageKey().equals(localStorage.getFitToWorkDate());
    }

    private boolean isFitToWorkComplete() {
        return isValidFitAnswer(isFit1) && isValidFitAnswer(isFit2) && isValidFitAnswer(isFit3);
    }

    private boolean isValidFitAnswer(int value) {
        return value == 0 || value == 1;
    }

    private boolean hasFitToWorkBeenSentToday() {
        return localStorage != null && todayStorageKey().equals(localStorage.getFitToWorkSentDate());
    }

    private String todayStorageKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
    }

    private void sendSummary(View view) {
        // Prevent spam click
        if (btnSync.isEnabled() == false || uploadProgressOverlay.getVisibility() == View.VISIBLE) {
            return;
        }

        Context pendingContext = getContext();
        if (pendingContext != null && !PendingUploadQueue.isEmpty(pendingContext)) {
            toast(requireActivity(), "Ada upload tertunda. Dicoba kirim ulang sekarang; kalau belum berhasil akan tetap otomatis.", Toast.LENGTH_LONG, GB.WARN);
            flushPendingUploads(true);
            return;
        }
        
        // Force populate sample data for developer mode before validation
        if (isDeveloperMode()) {
            // Ensure data is set synchronously before validation
            myData1.stepsTotal = 8547;
            myData1.activeMinutesTotal = 124;
            myData1.heartRate = 72;
            myData1.sleepToday = 420;
            myData1.sleepYesterday = 60;
            myData1.sleepRest = 30;
            myData1.sleepFrom = (int) (System.currentTimeMillis() / 1000) - (8 * 3600);
            myData1.sleepTo = (int) (System.currentTimeMillis() / 1000) - (1 * 3600);
            myData1.sleepTotalMinutes = 480;
            myData1.lightSleepTotalMinutes = 240;
            myData1.deepSleepTotalMinutes = 180;
            myData1.remSleepTotalMinutes = 0;
            myData1.awakeSleepTotalMinutes = 0;
            myData1.sleepWearableTotalMinutes = myData1.sleepTotalMinutes + myData1.awakeSleepTotalMinutes;
            
            myData2.stepsTotal = 7823;
            myData2.activeMinutesTotal = 98;
            myData2.heartRate = 70;
            
            isFit1 = 1;
            isFit2 = 1;
            isFit3 = 1;
            localStorage.setFit1(1);
            localStorage.setFit2(1);
            localStorage.setFit3(1);
            
            String p5mKey = textNik.getText() + "_" + textDate.getText();
            localStorage.setP5M(p5mKey);
        }
        
        Animation anim = new AlphaAnimation(1, 0.5f);
        anim.setDuration(50);
        anim.setRepeatMode(Animation.REVERSE);
        btnSync.startAnimation(anim);
        
        showUploadProgressOverlay(true);
        updateUploadProgress("Memulai upload...", 0, 2);

        long currentTime = Calendar.getInstance().getTimeInMillis() / 1000;
        boolean dayIsToday = !(myData1.timeTo < currentTime);

        GBDevice deviceToUse = resolvePrimaryWearableDevice();

        if (deviceToUse == null && isDeveloperMode()) {
            // Developer mode: create dummy device for testing
            deviceToUse = new GBDevice("00:11:22:33:44:55", "Developer Mi Band", "Mi Band Dev", null, DeviceType.MIBAND7);
            deviceToUse.setState(GBDevice.State.INITIALIZED);
        }

        if (deviceToUse == null) {
            showUploadProgressOverlay(false);
            toast(requireActivity(), "Device not connected!", Toast.LENGTH_SHORT, GB.ERROR);
            return;
        }
        
        // Validate date (must be today)
        if (!dayIsToday) {
            showUploadProgressOverlay(false);
            toast(requireActivity(), "Tanggal kirim harus hari ini!", Toast.LENGTH_SHORT, GB.ERROR);
            return;
        }
        
        // Validate sleep data (skip for developer mode as dummy data is auto-generated)
        if (!isDeveloperMode() && myData1.sleepTotalMinutes == 0) {
            showUploadProgressOverlay(false);
            toast(requireActivity(), "Jam tidur belum tersedia!", Toast.LENGTH_SHORT, GB.ERROR);
            return;
        }
        
        // Validate fit to work (skip only for developer mode as it's auto-set)
        if (!isDeveloperMode() && !restoreAndCheckFitToWorkForToday()) {
            showUploadProgressOverlay(false);
            toast(requireActivity(), "Fit to Work hari ini belum lengkap. Isi sekali saja sebelum upload.", Toast.LENGTH_SHORT, GB.ERROR);
            return;
        }
        
        // Validate P5M. If there is an active/fallback P5M, it must be submitted before upload.
        // If backend has no active P5M configured, upload can continue.
        if (!isDeveloperMode() && !p5mUploadGatePassed && !hasP5MSubmissionForToday()) {
            checkP5MRequirementBeforeUpload(view);
            return;
        }
        p5mUploadGatePassed = false;

        // All validations passed, proceed with upload
        adaptUploadRouteForCurrentNetwork();
        sendNetworkReport(deviceToUse);

        btnSync.setEnabled(false);
        loading.setVisibility(View.VISIBLE);

        // Generate sync batch identifiers for tracing
        String syncId = UUID.randomUUID().toString();
        long rangeStartTs = myData1.timeFrom;
        long rangeEndTs = myData1.timeTo;

        String summaryData = buildSummaryPayload(deviceToUse, syncId, rangeStartTs, rangeEndTs);
        String detailData = buildDetailPayload(deviceToUse, syncId, rangeStartTs, rangeEndTs);
        sendLegacySummary(deviceToUse, summaryData, detailData, false);
    }

    private void sendLegacySummary(GBDevice device, String summaryData, String detailData, boolean fromQueue) {
        btnSync.setEnabled(false);
        loading.setVisibility(View.VISIBLE);
        updateUploadProgress("Mengirim summary...", 0, 2);

        String url = getString(R.string.base_url) + "/summary";
        Context context = getContext();
        if (context == null) {
            btnSync.setEnabled(true);
            loading.setVisibility(View.GONE);
            showUploadProgressOverlay(false);
            return;
        }

        if (summaryData == null || summaryData.isEmpty()) {
            btnSync.setEnabled(true);
            loading.setVisibility(View.GONE);
            showUploadProgressOverlay(false);
            toast(requireActivity(), "Summary data empty, check wearable data", Toast.LENGTH_SHORT, GB.ERROR);
            return;
        }

        if (isDeveloperMode()) {
            Activity activity = getActivity();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                activity.runOnUiThread(() -> new Handler().postDelayed(() -> {
                    toast(requireActivity(), "✅ [DEV] Success send activity summary", Toast.LENGTH_SHORT, GB.INFO);
                    updateUploadProgress("Summary terkirim, mengirim detail...", 1, 2);
                    sendDetail(device, detailData, false, true);
                }, 500));
            }
            return;
        }

        new Thread(() -> {
            Http http = new Http(context, url);
            http.setMethod("post");
            http.setToken(true);
            http.setHeader("X-App-Version", getAppVersionStamp());
            http.setData(summaryData);
            http.send();

            Activity activity = getActivity();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                activity.runOnUiThread(() -> {
                    Integer code = http.getStatusCode();
                    if (code == 200) {
                        try {
                               JSONObject summaryResp = new JSONObject(http.getResponse());
                               lastSummaryId = summaryResp.optInt("id", summaryResp.optInt("summary_id", 0));
                               if (lastSummaryId <= 0) {
                                   JSONObject summaryPayload = summaryResp.optJSONObject("data");
                                   if (summaryPayload != null) {
                                       lastSummaryId = summaryPayload.optInt("summary_id", summaryPayload.optInt("id", 0));
                                   }
                               }
                            if (localStorage != null) {
                                localStorage.clearSummaryWeekCache();
                            }
                            toast(requireActivity(), "Success send activity summary", Toast.LENGTH_SHORT, GB.INFO);
                            updateUploadProgress("Summary terkirim, mengirim detail...", 1, 2);
                            sendDetail(device, detailData, false, true);
                        } catch (JSONException e) {
                            LOG.warn("Invalid summary response", e);
                            showUploadProgressOverlay(false);
                            btnSync.setEnabled(true);
                            loading.setVisibility(View.GONE);
                        }
                    } else if (code == 422 || code == 401 || code == 404) {
                        String msg = extractApiMessage(http.getResponse(), "Gagal mengirim ringkasan aktivitas.");
                        toast(requireActivity(), msg, Toast.LENGTH_LONG, GB.ERROR);
                        showUploadProgressOverlay(false);
                        btnSync.setEnabled(true);
                        loading.setVisibility(View.GONE);
                    } else {
                        if (!fromQueue && shouldQueueUpload(code)) {
                            String summaryKey = summaryQueueKey(device);
                            String summaryFp = PendingUploadQueue.fingerprint("summary", summaryKey);
                            String detailUrl = getString(R.string.base_url) + "/detail";
                            String detailKey = detailQueueKey(device);
                            String detailFp = PendingUploadQueue.fingerprint("detail", detailKey);
                            queuePendingUpload("summary", url, summaryData, summaryKey);
                            queuePendingUpload("detail", detailUrl, detailData, detailKey);
                            showUploadPendingNotification();
                            startQueueWait("summary", device, summaryFp, detailFp, detailData);
                        } else {
                            showUploadPendingNotification();
                            toast(requireActivity(), http.getErrorMessage("Gagal mengirim ringkasan aktivitas. Data disimpan di antrian upload."), Toast.LENGTH_LONG, GB.ERROR);
                            showUploadProgressOverlay(false);
                            btnSync.setEnabled(true);
                            loading.setVisibility(View.GONE);
                        }
                    }
                });
            }
        }).start();
    }

    private void sendSummary(GBDevice device, List<IngestChunk> chunks, int chunkPosition) {
        // Legacy compatibility guard: keep upload schema on /summary + /detail.
        long rangeStartTs = myData1.timeFrom;
        long rangeEndTs = myData1.timeTo;
        String syncId = UUID.randomUUID().toString();
        String summaryData = buildSummaryPayload(device, syncId, rangeStartTs, rangeEndTs);
        String detailData = buildDetailPayload(device, syncId, rangeStartTs, rangeEndTs);
        sendLegacySummary(device, summaryData, detailData, false);
    }

    private void queueRemainingIngestChunks(String url, List<IngestChunk> chunks, int startIndex) {
        for (int i = startIndex; i < chunks.size(); i++) {
            IngestChunk chunk = chunks.get(i);
            queuePendingUpload("ingest", url, chunk.payload, chunk.queueKey);
        }
    }

    private List<IngestChunk> buildIngestChunks(GBDevice device, String uploadId, long rangeStartTs, long rangeEndTs) {
        List<IngestChunk> chunks = new ArrayList<>();

        try {
            JSONArray rawFiles = loadRawFetchFileEntries(device, rangeStartTs, rangeEndTs);
            List<JSONArray> rawFileChunks = chunkRawFetchFiles(rawFiles);
            int chunkCount = rawFileChunks.size();

            for (int i = 0; i < rawFileChunks.size(); i++) {
                int chunkIndex = i + 1;
                boolean includeMetricSeries = chunkIndex == 1;
                String payload = buildIngestPayload(device, uploadId, rangeStartTs, rangeEndTs, chunkIndex, chunkCount, rawFileChunks.get(i), includeMetricSeries);
                chunks.add(new IngestChunk(uploadId, chunkIndex, chunkCount, payload, ingestQueueKey(device, uploadId, chunkIndex)));
            }
        } catch (Exception e) {
            LOG.warn("Failed to build ingest chunks", e);
        }

        return chunks;
    }

    private List<JSONArray> chunkRawFetchFiles(JSONArray rawFiles) {
        List<JSONArray> chunks = new ArrayList<>();
        JSONArray currentChunk = new JSONArray();
        int currentBytes = 0;

        for (int i = 0; i < rawFiles.length(); i++) {
            JSONObject file = rawFiles.optJSONObject(i);
            if (file == null) {
                continue;
            }

            int estimatedBytes = file.optString("payload_base64", "").length() + 2048;
            if (currentChunk.length() > 0 && currentBytes + estimatedBytes > INGEST_RAW_CHUNK_TARGET_BYTES) {
                chunks.add(currentChunk);
                currentChunk = new JSONArray();
                currentBytes = 0;
            }

            currentChunk.put(file);
            currentBytes += estimatedBytes;
        }

        if (currentChunk.length() > 0 || chunks.isEmpty()) {
            chunks.add(currentChunk);
        }

        return chunks;
    }

    private String buildIngestPayload(GBDevice device, String uploadId, long rangeStartTs, long rangeEndTs,
                                      int chunkIndex, int chunkCount, JSONArray rawFetchFiles,
                                      boolean includeMetricSeries) {
        JSONObject payload = new JSONObject();
        try {
            JSONArray activity = includeMetricSeries && jsonActivity != null ? jsonActivity : new JSONArray();
            JSONArray sleepSessions = includeMetricSeries && jsonSleep != null ? jsonSleep : new JSONArray();
            JSONArray spo2 = includeMetricSeries && jsonSpo2 != null ? jsonSpo2 : new JSONArray();
            JSONArray stress = includeMetricSeries && jsonStress != null ? jsonStress : new JSONArray();
            JSONObject rawFetch = buildRawFetchPayload(rawFetchFiles, rangeStartTs, rangeEndTs);

            long minTs = Long.MAX_VALUE;
            long maxTs = Long.MIN_VALUE;

            long[] activityRange = computeRangeFromSeries(activity, "timestamp");
            minTs = Math.min(minTs, activityRange[0]);
            maxTs = Math.max(maxTs, activityRange[1]);

            long[] sleepRange = computeSleepRange(sleepSessions);
            minTs = Math.min(minTs, sleepRange[0]);
            maxTs = Math.max(maxTs, sleepRange[1]);

            long[] spo2Range = computeRangeFromSeries(spo2, "timestamp");
            minTs = Math.min(minTs, spo2Range[0]);
            maxTs = Math.max(maxTs, spo2Range[1]);

            long[] stressRange = computeRangeFromSeries(stress, "timestamp");
            minTs = Math.min(minTs, stressRange[0]);
            maxTs = Math.max(maxTs, stressRange[1]);

            if (minTs == Long.MAX_VALUE || maxTs == Long.MIN_VALUE || minTs <= 0 || maxTs <= 0) {
                minTs = rangeStartTs;
                maxTs = rangeEndTs;
            }

            JSONObject counts = new JSONObject();
            counts.put("activity", activity.length());
            counts.put("sleep", sleepSessions.length());
            counts.put("spo2", spo2.length());
            counts.put("stress", stress.length());
            counts.put("raw_fetch_files", rawFetch.optInt("file_count", 0));

            JSONObject range = new JSONObject();
            range.put("min_ts", minTs);
            range.put("max_ts", maxTs);

            JSONObject data = new JSONObject();
            data.put("activity", activity);
            data.put("sleep_sessions", sleepSessions);
            data.put("spo2", spo2);
            data.put("stress", stress);
            data.put("raw_fetch", rawFetch);

            payload.put("upload_id", uploadId);
            payload.put("chunk_index", chunkIndex);
            payload.put("chunk_count", chunkCount);
            payload.put("idempotency_key", uploadId + "-c" + chunkIndex);
            payload.put("sent_at", System.currentTimeMillis() / 1000L);
            payload.put("device_id", deviceId);
            payload.put("user_id", resolveUserIdForIngest());
            payload.put("date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(rangeEndTs * 1000L)));
            payload.put("counts", counts);
            payload.put("range", range);
            payload.put("payload_size_bytes", 0);
            payload.put("payload_hash_sha256", "");
            payload.put("data", data);
            finalizeIngestPayload(payload);
        } catch (JSONException e) {
            LOG.error("buildIngestPayload error", e);
        }
        return payload.toString();
    }

    private JSONObject buildRawFetchPayload(JSONArray filesJson, long rangeStartTs, long rangeEndTs) throws JSONException {
        JSONObject rawFetch = new JSONObject();
        long totalBytes = 0L;
        long minFileTs = Long.MAX_VALUE;
        long maxFileTs = Long.MIN_VALUE;

        rawFetch.put("source", "xiaomi_raw_fetch_operations");
        rawFetch.put("requested_min_ts", rangeStartTs);
        rawFetch.put("requested_max_ts", rangeEndTs);

        for (int i = 0; i < filesJson.length(); i++) {
            JSONObject fileJson = filesJson.optJSONObject(i);
            if (fileJson == null) {
                continue;
            }

            JSONObject headerJson = fileJson.optJSONObject("header");
            long fileTs = headerJson != null ? headerJson.optLong("timestamp", 0L) : 0L;
            totalBytes += fileJson.optLong("size_bytes", 0L);
            if (fileTs > 0) {
                minFileTs = Math.min(minFileTs, fileTs);
                maxFileTs = Math.max(maxFileTs, fileTs);
            }
        }

        rawFetch.put("available", filesJson.length() > 0);
        rawFetch.put("file_count", filesJson.length());
        rawFetch.put("total_bytes", totalBytes);
        rawFetch.put("files", filesJson);
        rawFetch.put("file_min_ts", minFileTs == Long.MAX_VALUE ? JSONObject.NULL : minFileTs);
        rawFetch.put("file_max_ts", maxFileTs == Long.MIN_VALUE ? JSONObject.NULL : maxFileTs);

        return rawFetch;
    }

    private JSONArray loadRawFetchFileEntries(GBDevice device, long rangeStartTs, long rangeEndTs) throws JSONException {
        JSONArray filesJson = new JSONArray();

        try {
            File exportDirectory = device.getDeviceCoordinator().getWritableExportDirectory(device);
            File rawFetchDir = new File(exportDirectory, "rawFetchOperations");
            File[] rawFiles = rawFetchDir.listFiles((dir, name) -> name.startsWith("xiaomi_") && name.endsWith(".bin"));
            if (rawFiles == null || rawFiles.length == 0) {
                return filesJson;
            }

            List<File> sortedFiles = new ArrayList<>();
            Collections.addAll(sortedFiles, rawFiles);
            sortedFiles.sort(Comparator.comparing(File::getName));

            for (File rawFile : sortedFiles) {
                byte[] bytes = readFileBytes(rawFile);
                if (bytes.length < 7) {
                    continue;
                }

                XiaomiActivityFileId fileId = XiaomiActivityFileId.from(new byte[]{
                        bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6]
                });
                long fileTs = fileId.getTimestamp().getTime() / 1000L;
                if (fileTs < rangeStartTs || fileTs > rangeEndTs) {
                    continue;
                }

                JSONObject fileJson = new JSONObject();
                JSONObject headerJson = new JSONObject();
                headerJson.put("timestamp", fileTs);
                headerJson.put("timezone_quarter_hours", fileId.getTimezone());
                headerJson.put("timezone_offset_minutes", fileId.getTimezone() * 15);
                headerJson.put("type", fileId.getType().name());
                headerJson.put("type_code", fileId.getTypeCode());
                headerJson.put("subtype", fileId.getSubtype().name());
                headerJson.put("subtype_code", fileId.getSubtypeCode());
                headerJson.put("detail_type", fileId.getDetailType().name());
                headerJson.put("detail_type_code", fileId.getDetailTypeCode());
                headerJson.put("version", fileId.getVersion());

                fileJson.put("filename", rawFile.getName());
                fileJson.put("relative_path", "rawFetchOperations/" + rawFile.getName());
                fileJson.put("size_bytes", bytes.length);
                fileJson.put("modified_at", rawFile.lastModified() / 1000L);
                fileJson.put("sha256", sha256Bytes(bytes));
                fileJson.put("header", headerJson);
                fileJson.put("payload_base64", Base64.encodeToString(bytes, Base64.NO_WRAP));
                filesJson.put(fileJson);
            }
        } catch (Exception e) {
            LOG.warn("Failed to load raw fetch files", e);
        }

        return filesJson;
    }

    private byte[] readFileBytes(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }

    private void finalizeIngestPayload(JSONObject payload) throws JSONException {
        JSONObject canonicalPayload = new JSONObject(payload.toString());
        canonicalPayload.remove("payload_hash_sha256");
        canonicalPayload.remove("payload_size_bytes");

        String canonicalJson = canonicalJson(canonicalPayload);
        byte[] canonicalBytes = canonicalJson.getBytes(StandardCharsets.UTF_8);

        payload.put("payload_size_bytes", canonicalBytes.length);
        payload.put("payload_hash_sha256", sha256Bytes(canonicalBytes));
    }

    private String canonicalJson(Object value) throws JSONException {
        if (value == null || value == JSONObject.NULL) {
            return "null";
        }

        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            List<String> keys = new ArrayList<>();
            Iterator<String> iterator = object.keys();
            while (iterator.hasNext()) {
                keys.add(iterator.next());
            }
            Collections.sort(keys);

            StringBuilder out = new StringBuilder();
            out.append('{');
            for (int index = 0; index < keys.size(); index++) {
                String key = keys.get(index);
                if (index > 0) {
                    out.append(',');
                }
                out.append(quoteCanonicalString(key));
                out.append(':');
                out.append(canonicalJson(object.opt(key)));
            }
            out.append('}');
            return out.toString();
        }

        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            StringBuilder out = new StringBuilder();
            out.append('[');
            for (int index = 0; index < array.length(); index++) {
                if (index > 0) {
                    out.append(',');
                }
                out.append(canonicalJson(array.opt(index)));
            }
            out.append(']');
            return out.toString();
        }

        if (value instanceof String || value instanceof Character) {
            return quoteCanonicalString(String.valueOf(value));
        }

        if (value instanceof Boolean) {
            return String.valueOf(value);
        }

        if (value instanceof Number) {
            return String.valueOf(value);
        }

        return quoteCanonicalString(String.valueOf(value));
    }

    private String quoteCanonicalString(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            switch (ch) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        out.append(String.format(Locale.US, "\\u%04x", (int) ch));
                    } else {
                        out.append(ch);
                    }
                    break;
            }
        }
        out.append('"');
        return out.toString();
    }

    private String sha256Bytes(byte[] rawBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(rawBytes);
            StringBuilder out = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                out.append(String.format(Locale.US, "%02x", b));
            }
            return out.toString();
        } catch (Exception e) {
            LOG.warn("sha256 failed", e);
            return "";
        }
    }

    private String buildIngestErrorMessage(Integer code, String rawResponse) {
        JSONObject response = parseJsonSafe(rawResponse);
        String message = response.optString("message", "").trim();
        String clientHash = response.optString("client_hash_sha256", "");
        String rawHash = response.optString("server_hash_raw_sha256", "");
        String canonicalHash = response.optString("server_hash_canonical_sha256", "");
        int rawSize = response.optInt("server_raw_size_bytes", -1);
        int canonicalSize = response.optInt("server_canonical_size_bytes", -1);
        String mode = response.optString("mode", response.optString("hash_mode", ""));

        if (!clientHash.isEmpty() || !rawHash.isEmpty() || !canonicalHash.isEmpty() || rawSize >= 0 || canonicalSize >= 0 || !mode.isEmpty()) {
            LOG.warn("Ingest debug clientHash={}, serverRawHash={}, serverCanonicalHash={}, serverRawSize={}, serverCanonicalSize={}, mode={}",
                    clientHash, rawHash, canonicalHash, rawSize, canonicalSize, mode);
        }

        if (!message.isEmpty()) {
            return message;
        }

        if (code == null) return "Upload ingest gagal";
        if (code == 409) return "Upload ingest gagal: idempotency conflict";
        if (code == 413) return "Upload ingest gagal: payload terlalu besar";
        if (code == 422) return "Upload ingest gagal: validasi, hash, atau size mismatch";
        if (code == 500) return "Upload ingest gagal: server error";
        return "Upload ingest gagal";
    }

    private long[] computeRangeFromSeries(JSONArray arr, String tsKey) {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        if (arr == null) return new long[]{min, max};
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.optJSONObject(i);
            if (obj == null) continue;
            long ts = obj.optLong(tsKey, 0L);
            if (ts <= 0) continue;
            if (ts < min) min = ts;
            if (ts > max) max = ts;
        }
        return new long[]{min, max};
    }

    private long[] computeSleepRange(JSONArray sleepArr) {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        if (sleepArr == null) return new long[]{min, max};
        for (int i = 0; i < sleepArr.length(); i++) {
            JSONObject s = sleepArr.optJSONObject(i);
            if (s == null) continue;
            long start = s.optLong("sleepStart", s.optLong("sleep_start", 0L));
            long end = s.optLong("sleepEnd", s.optLong("sleep_end", 0L));
            if (start > 0 && start < min) min = start;
            if (start > 0 && start > max) max = start;
            if (end > 0 && end > max) max = end;
            if (end > 0 && end < min) min = end;
        }
        return new long[]{min, max};
    }

    private String sha256(String raw) {
        return sha256Bytes(raw.getBytes(StandardCharsets.UTF_8));
    }

    private JSONObject parseJsonSafe(String raw) {
        if (raw == null || raw.isEmpty()) return new JSONObject();
        try {
            return new JSONObject(raw);
        } catch (JSONException ignored) {
            return new JSONObject();
        }
    }

    private int resolveUserIdForIngest() {
        if (userId > 0) {
            return userId;
        }

        String employeeJson = localStorage != null ? localStorage.getEmployee() : "";
        if (employeeJson != null && !employeeJson.isBlank()) {
            try {
                JSONObject employee = new JSONObject(employeeJson);
                int parsedUserId = employee.optInt("user_id", 0);
                if (parsedUserId > 0) {
                    userId = parsedUserId;
                    return userId;
                }
            } catch (JSONException ignored) {
                // Keep fallback below.
            }
        }

        // Last fallback to preserve previous behavior when user_id is unavailable.
        return employeeId;
    }

    private void logIngestAuditMeta(String phase, String ingestData, Integer statusCode) {
        JSONObject ingestPayload = parseJsonSafe(ingestData);
        String uploadId = ingestPayload.optString("upload_id", "");
        long sentAt = ingestPayload.optLong("sent_at", 0L);
        int payloadUserId = ingestPayload.optInt("user_id", 0);
        String token = localStorage != null ? localStorage.getToken() : "";
        String tokenId = "";
        if (token != null && !token.isEmpty()) {
            int separator = token.indexOf('|');
            tokenId = separator > 0 ? token.substring(0, separator) : token;
        }

        LOG.info("Ingest audit phase={}, upload_id={}, sent_at={}, user_id={}, employee_id={}, bearer_token_id={}, status={}",
                phase,
                uploadId,
                sentAt,
                payloadUserId,
                employeeId,
                tokenId,
                statusCode == null ? "-" : statusCode.toString());
    }

    private boolean isIngestResponseAcceptable(JSONObject response, String expectedUploadId) {
        if (response == null) return false;
        if (!response.optBoolean("ok", true)) {
            return false;
        }

        JSONObject accepted = response.optJSONObject("accepted_counts");
        JSONObject parsed = response.optJSONObject("parsed_counts");
        if (accepted == null || parsed == null) {
            JSONObject data = response.optJSONObject("data");
            String responseUploadId = data != null ? data.optString("upload_id", "") : response.optString("upload_id", "");
            if (!expectedUploadId.isEmpty() && expectedUploadId.equals(responseUploadId)) {
                return true;
            }

            String message = response.optString("message", "").toLowerCase(Locale.ROOT);
            return message.contains("success");
        }

        int acceptedActivity = accepted.optInt("activity", -1);
        int acceptedSleep = accepted.optInt("sleep", -1);
        int acceptedSpo2 = accepted.optInt("spo2", -1);
        int acceptedStress = accepted.optInt("stress", -1);
        int parsedActivity = parsed.optInt("activity", -1);
        int parsedSleep = parsed.optInt("sleep", -1);
        int parsedSpo2 = parsed.optInt("spo2", -1);
        int parsedStress = parsed.optInt("stress", -1);

        if (acceptedActivity < 0 || acceptedSleep < 0 || acceptedSpo2 < 0 || acceptedStress < 0) {
            return false;
        }

        boolean parsedOk = parsedActivity >= acceptedActivity
                && parsedSleep >= acceptedSleep
                && parsedSpo2 >= acceptedSpo2
                && parsedStress >= acceptedStress;

        if (!parsedOk) {
            LOG.warn("Ingest mismatch parsed<accepted, accepted={}, parsed={}", accepted, parsed);
            return false;
        }

        JSONObject stored = response.optJSONObject("stored_counts");
        if (stored != null) {
            LOG.info("Ingest stored_counts: {}", stored);
        }

        String serverHash = response.optString("server_hash_sha256", "");
        if (!serverHash.isEmpty()) {
            LOG.info("Ingest server_hash_sha256: {}", serverHash);
        }

        JSONArray warnings = response.optJSONArray("warnings");
        if (warnings != null && warnings.length() > 0) {
            LOG.warn("Ingest warnings: {}", warnings);
        }

        return true;
    }

    private String extractUploadId(String ingestPayload) {
        JSONObject payload = parseJsonSafe(ingestPayload);
        return payload.optString("upload_id", "");
    }

    private String buildSummaryPayload(GBDevice device, String syncId, long rangeStartTs, long rangeEndTs) {
        synchronized (metricJsonLock) {
            dataActivity(device, myData1.timeFrom, myData1.timeTo);

            JSONObject params = new JSONObject();
            try {
                params.put("sync_id", syncId);
                params.put("upload_id", syncId);
                params.put("range_start_ts", rangeStartTs);
                params.put("range_end_ts", rangeEndTs);
                params.put("active", myData1.getActiveMinutesTotal());
                params.put("steps", myData1.getStepsTotal());
                int summaryHr = myData1.getHeartRate();
                params.put("heart_rate", summaryHr);
                params.put("distance", myData1.getDistanceTotal());
                params.put("calories", myData1.getCalories());
                params.put("spo2", myData1.bloodOxygen);
                params.put("stress", myData1.stress);
                params.put("sleep", myData1.sleepTotalMinutes);
                params.put("sleep_effective", myData1.sleepTotalMinutes);
                params.put("sleep_effective_minutes", myData1.sleepTotalMinutes);
                params.put("sleep_wearable", getWearableSleepTotalMinutes(myData1));
                params.put("sleep_wearable_minutes", getWearableSleepTotalMinutes(myData1));
                params.put("sleep_decision", getSleepStatusLabel(myData1.sleepTotalMinutes));
                params.put("sleep_start", myData1.sleepFrom);
                params.put("sleep_end", myData1.sleepTo);
                params.put("sleep_type", myData1.sleepType);
                params.put("light_sleep", myData1.lightSleepTotalMinutes);
                params.put("deep_sleep", myData1.deepSleepTotalMinutes);
                params.put("rem_sleep", myData1.remSleepTotalMinutes);
                params.put("awake", myData1.awakeSleepTotalMinutes);
                params.put("wakeup", "0");
                params.put("status", "0");
                params.put("device_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                params.put("mac_address", device.getAddress());
                params.put("app_version", getAppVersionStamp());
                params.put("user_activity", jsonActivity.toString());
                params.put("user_sleep", jsonSleep.toString());
                params.put("user_stress", jsonStress.toString());
                params.put("user_spo2", jsonSpo2.toString());
                params.put("user_heart_rate_max", jsonHeartRateMax.toString());
                params.put("user_heart_rate_resting", jsonHeartRateResting.toString());
                params.put("user_heart_rate_manual", jsonHeartRateManual.toString());
                params.put("device_id", deviceId);
                params.put("employee_id", employeeId);
                params.put("company_id", companyId);
                params.put("department_id", departmentId);
                params.put("shift_id", shiftId);
                params.put("fit_to_work_q1", isFit1);
                params.put("fit_to_work_q2", isFit2);
                params.put("fit_to_work_q3", isFit3);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return params.toString();
        }
    }

    private boolean isSleepUploaderAccount() {
        if (localStorage == null) {
            return false;
        }

        if (localStorage.getSleepUploader()) {
            return true;
        }

        String employeeJson = localStorage.getEmployee();
        if (employeeJson == null || employeeJson.isBlank()) {
            return false;
        }

        try {
            JSONObject employee = new JSONObject(employeeJson);
            return readBooleanFlag(employee, "is_sleep_uploader");
        } catch (JSONException ignored) {
            return false;
        }
    }

    private boolean readBooleanFlag(JSONObject object, String key) {
        if (object == null || !object.has(key)) {
            return false;
        }

        Object value = object.opt(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }

        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return "1".equals(text) || "true".equals(text) || "yes".equals(text) || "ya".equals(text);
    }

    private boolean hasP5MSubmissionForToday() {
        if (isTodayP5MMarker(localStorage.getP5M())) {
            return true;
        }

        String cachedAnswers = localStorage.getP5MAnswers();
        if (cachedAnswers == null || cachedAnswers.isBlank()) {
            return false;
        }

        try {
            JSONObject payload = new JSONObject(cachedAnswers);
            String submittedAt = payload.optString("submitted_at", "");
            JSONArray answers = payload.optJSONArray("answers");
            if (answers == null || answers.length() == 0 || submittedAt.isEmpty()) {
                return false;
            }

            String submittedDate = submittedAt.length() >= 10 ? submittedAt.substring(0, 10) : submittedAt;
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(System.currentTimeMillis());
            return today.equals(submittedDate);
        } catch (JSONException e) {
            LOG.warn("Failed parsing cached P5M answers", e);
            return false;
        }
    }

    private void checkP5MRequirementBeforeUpload(View retryView) {
        Context context = getContext();
        if (context == null) {
            showUploadProgressOverlay(false);
            return;
        }

        if (!p5mUploadCheckRunning.compareAndSet(false, true)) {
            return;
        }

        updateUploadProgress("Memeriksa status P5M...", 0, 2);
        String url = getString(R.string.base_url) + "/p5m";

        new Thread(() -> {
            Http http = new Http(context, url);
            http.setMethod("get");
            http.setToken(true);
            http.setBypassCache(true);
            http.setConnectTimeoutMs(8000);
            http.setReadTimeoutMs(12000);
            http.setMaxAttempts(1);
            http.send();

            Activity activity = getActivity();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                p5mUploadCheckRunning.set(false);
                return;
            }

            activity.runOnUiThread(() -> {
                p5mUploadCheckRunning.set(false);
                handleP5MUploadCheckResponse(http, retryView);
            });
        }).start();
    }

    private void handleP5MUploadCheckResponse(Http http, View retryView) {
        int statusCode = http.getStatusCode() == null ? 0 : http.getStatusCode();
        if (statusCode != 200) {
            blockUploadAfterP5MWarning(http.getErrorMessage(getString(R.string.p5m_check_failed_before_upload)));
            return;
        }

        try {
            JSONObject response = new JSONObject(Objects.requireNonNullElse(http.getResponse(), "{}"));
            JSONObject data = response.optJSONObject("data");
            if (data == null) {
                blockUploadAfterP5MWarning(getString(R.string.p5m_error_invalid_response));
                return;
            }

            boolean submitted = data.optBoolean("already_submitted", false) || data.optJSONObject("today_score") != null;
            if (submitted) {
                markTodayP5MSubmitted();
                p5mUploadGatePassed = true;
                showUploadProgressOverlay(false);
                sendSummary(retryView);
                return;
            }

            JSONObject quiz = data.optJSONObject("quiz");
            JSONArray items = data.optJSONArray("items");
            boolean hasActiveQuiz = quiz != null && items != null && items.length() > 0;
            if (!hasActiveQuiz) {
                p5mUploadGatePassed = true;
                showUploadProgressOverlay(false);
                sendSummary(retryView);
                return;
            }

            showUploadProgressOverlay(false);
            toast(requireActivity(), getString(R.string.p5m_required_before_upload), Toast.LENGTH_LONG, GB.ERROR);
            navigateToP5M();
        } catch (JSONException e) {
            LOG.warn("Failed parsing P5M upload check response", e);
            blockUploadAfterP5MWarning(getString(R.string.p5m_error_invalid_response));
        }
    }

    private void blockUploadAfterP5MWarning() {
        blockUploadAfterP5MWarning(getString(R.string.p5m_check_failed_before_upload));
    }

    private void blockUploadAfterP5MWarning(String message) {
        toast(requireActivity(), message, Toast.LENGTH_LONG, GB.WARN);
        p5mUploadGatePassed = false;
        showUploadProgressOverlay(false);
    }

    private void markTodayP5MSubmitted() {
        String nik = String.valueOf(textNik.getText()).trim();
        if (nik.isBlank()) {
            return;
        }

        String today = new SimpleDateFormat("E, dd MMM yyyy", Locale.getDefault()).format(System.currentTimeMillis());
        localStorage.setP5M(nik + "_" + today);
    }

    private void navigateToP5M() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Activity activity = getActivity();
                if (activity instanceof HomeActivity) {
                    ((HomeActivity) activity).changeViewPagerPostition(8);
                }
            }
        }, 700);
    }

    private boolean isTodayP5MMarker(String marker) {
        String nik = String.valueOf(textNik.getText()).trim();
        if (marker == null || marker.isBlank() || nik.isBlank()) {
            return false;
        }

        String prefix = nik + "_";
        if (!marker.startsWith(prefix)) {
            return false;
        }

        String markerDate = marker.substring(prefix.length()).trim();
        String screenDate = String.valueOf(textDate.getText()).trim();
        if (Objects.equals(markerDate, screenDate)) {
            return true;
        }

        long now = System.currentTimeMillis();
        String[] acceptedTodayFormats = new String[] {
                new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(now),
                new SimpleDateFormat("E, dd MMM yyyy", Locale.getDefault()).format(now),
                new SimpleDateFormat("EEE, dd MMM yyyy", Locale.ENGLISH).format(now)
        };

        for (String today : acceptedTodayFormats) {
            if (Objects.equals(markerDate, today)) {
                return true;
            }
        }

        return false;
    }

    private void adaptUploadRouteForCurrentNetwork() {
        Context context = getContext();
        if (context == null || localStorage == null) {
            return;
        }

        String publicBaseUrl = localStorage.getApiPublicUrl();
        if (publicBaseUrl.isEmpty()) {
            return;
        }

        String networkType = getNetworkType(context);
        boolean metered = isActiveNetworkMetered(context);
        if ("mobile".equalsIgnoreCase(networkType) || metered) {
            localStorage.setApiPreferredRoute("public");
            localStorage.setApiActiveBaseUrl(publicBaseUrl);
        }
    }

    private void sendNetworkReport(GBDevice device) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        String url = getString(R.string.base_url) + "/network-report";
        String payload = buildNetworkReportPayload(device);

        new Thread(() -> {
            try {
                Http http = new Http(context, url);
                http.setMethod("post");
                http.setToken(true);
                http.setData(payload);
                http.setConnectTimeoutMs(5000);
                http.setReadTimeoutMs(7000);
                http.setMaxAttempts(1);
                http.send();

                Integer code = http.getStatusCode();
                if (code == null || (code != 200 && code != 201)) {
                    return;
                }
            } catch (Exception e) {
                // Network report is best-effort, do not crash if it fails
                LOG.warn("Network report failed: ", e);
            }
        }).start();
    }

    private void sendSleepSnapshotIfNeeded() {
        if (localStorage == null || isDeveloperMode()) {
            return;
        }

        Context context = getContext();
        if (context == null || employeeId <= 0 || myData1.sleepTotalMinutes <= 0 || myData1.sleepFrom <= 0 || myData1.sleepTo <= 0) {
            return;
        }

        GBDevice device = firstSnapshotDevice();
        if (device == null) {
            return;
        }

        final int snapshotSleepFrom = myData1.sleepFrom;
        final int snapshotSleepTo = myData1.sleepTo;
        final int snapshotTimeFrom = snapshotSleepFrom;
        final int snapshotTimeTo = snapshotSleepTo + 3600;
        final String snapshotSleepType = myData1.sleepType == null ? "night" : myData1.sleepType;
        final long snapshotSleepTotal = myData1.sleepTotalMinutes;
        final long snapshotSleepWearableTotal = getWearableSleepTotalMinutes(myData1);
        final long snapshotLightSleep = myData1.lightSleepTotalMinutes;
        final long snapshotDeepSleep = myData1.deepSleepTotalMinutes;
        final long snapshotRemSleep = myData1.remSleepTotalMinutes;
        final long snapshotAwakeSleep = myData1.awakeSleepTotalMinutes;
        final int snapshotHeartRate = myData1.heartRate;
        final int snapshotSpo2 = myData1.bloodOxygen;
        final int snapshotStress = myData1.stress;
        final String snapshotKey = sleepSnapshotQueueKey(device, snapshotSleepType, snapshotSleepFrom, snapshotSleepTo, snapshotSleepTotal);

        if (snapshotKey.equals(localStorage.getLastSleepSnapshotKey())) {
            return;
        }

        if (!sleepSnapshotRunning.compareAndSet(false, true)) {
            return;
        }

        final Context appContext = context.getApplicationContext();
        final String url = getString(R.string.base_url) + "/mobile/sleep-snapshot";

        new Thread(() -> {
            try {
                String payload;
                synchronized (metricJsonLock) {
                    dataActivity(device, snapshotTimeFrom, snapshotTimeTo);
                    if (jsonSleep.length() == 0 && jsonActivity.length() == 0) {
                        return;
                    }
                    payload = buildSleepSnapshotPayload(
                            device,
                            snapshotTimeTo,
                            snapshotSleepType,
                            snapshotSleepFrom,
                            snapshotSleepTo,
                            snapshotSleepTotal,
                            snapshotSleepWearableTotal,
                            snapshotLightSleep,
                            snapshotDeepSleep,
                            snapshotRemSleep,
                            snapshotAwakeSleep,
                            snapshotHeartRate,
                            snapshotSpo2,
                            snapshotStress
                    );
                }

                Http http = new Http(appContext, url);
                http.setMethod("post");
                http.setToken(true);
                http.setHeader("X-App-Version", getAppVersionStamp());
                http.setData(payload);
                http.setConnectTimeoutMs(5000);
                http.setReadTimeoutMs(8000);
                http.setMaxAttempts(1);
                http.send();

                Integer code = http.getStatusCode();
                if (code != null && (code == 200 || code == 201)) {
                    localStorage.setLastSleepSnapshotKey(snapshotKey);
                    return;
                }

                if (shouldQueueUpload(code)) {
                    PendingUploadQueue.enqueue(
                            appContext,
                            PendingUploadQueue.createItem("sleep-snapshot", url, payload, snapshotKey)
                    );
                    localStorage.setLastSleepSnapshotKey(snapshotKey);
                    showUploadPendingNotification();
                }
            } catch (Exception e) {
                LOG.warn("Sleep snapshot sync failed", e);
            } finally {
                sleepSnapshotRunning.set(false);
            }
        }).start();
    }

    private GBDevice firstSnapshotDevice() {
        return resolvePrimaryWearableDevice();
    }

    private String buildSleepSnapshotPayload(
            GBDevice device,
            int snapshotTimeTo,
            String sleepType,
            int sleepFrom,
            int sleepTo,
            long sleepTotal,
            long sleepWearableTotal,
            long lightSleep,
            long deepSleep,
            long remSleep,
            long awakeSleep,
            int heartRate,
            int spo2,
            int stress
    ) throws JSONException {
        JSONObject params = new JSONObject();
        params.put("device_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(snapshotTimeTo * 1000L)));
        params.put("mac_address", device.getAddress());
        params.put("app_version", getAppVersionStamp());
        params.put("employee_id", employeeId);
        params.put("device_id", deviceId);
        params.put("company_id", companyId);
        params.put("department_id", departmentId);
        params.put("shift_id", shiftId);
        params.put("sleep", sleepTotal);
        params.put("sleep_effective", sleepTotal);
        params.put("sleep_effective_minutes", sleepTotal);
        params.put("sleep_wearable", sleepWearableTotal);
        params.put("sleep_wearable_minutes", sleepWearableTotal);
        params.put("sleep_decision", getSleepStatusLabel(sleepTotal));
        params.put("sleep_start", sleepFrom);
        params.put("sleep_end", sleepTo);
        params.put("sleep_type", sleepType);
        params.put("light_sleep", lightSleep);
        params.put("deep_sleep", deepSleep);
        params.put("rem_sleep", remSleep);
        params.put("awake", awakeSleep);
        params.put("wakeup", 0);
        params.put("heart_rate", heartRate);
        params.put("spo2", spo2);
        params.put("stress", stress);
        params.put("user_activity", jsonActivity.toString());
        params.put("user_sleep", jsonSleep.toString());
        params.put("user_stress", jsonStress.toString());
        params.put("user_spo2", jsonSpo2.toString());
        params.put("user_heart_rate_max", jsonHeartRateMax.toString());
        params.put("user_heart_rate_resting", jsonHeartRateResting.toString());
        params.put("user_heart_rate_manual", jsonHeartRateManual.toString());
        return params.toString();
    }

    private String sleepSnapshotQueueKey(GBDevice device, String sleepType, int sleepFrom, int sleepTo, long sleepTotal) {
        return "sleep-snapshot|" + employeeId + "|" + device.getAddress() + "|" + sleepType + "|" + sleepFrom + "|" + sleepTo + "|" + sleepTotal;
    }

    private String buildNetworkReportPayload(GBDevice device) {
        Context context = getContext();
        JSONObject params = new JSONObject();
        try {
            if (device != null) {
                params.put("mac_address", device.getAddress());
            }
            params.put("device_id", deviceId);
            params.put("employee_id", employeeId);
            params.put("company_id", companyId);
            params.put("department_id", departmentId);
            params.put("shift_id", shiftId);
            params.put("app_version", getAppVersionStamp());
            params.put("device_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            params.put("network_type", getNetworkType(context));
            params.put("is_metered", isActiveNetworkMetered(context));
            params.put("downlink_mbps", getNetworkBandwidthMbps(context, true));
            params.put("uplink_mbps", getNetworkBandwidthMbps(context, false));
            params.put("rtt_ms", 0);
            params.put("device_signal_level", getNetworkSignalLevel(context));
        } catch (JSONException e) {
            LOG.warn("Error building network report: ", e);
        } catch (Exception e) {
            LOG.warn("Unexpected error in network report payload: ", e);
        }

        return params.toString();
    }

    private String getNetworkType(Context context) {
        if (context == null) {
            return "unknown";
        }

        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return "unknown";
        }

        android.net.NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo == null || !netInfo.isConnected()) {
            return "none";
        }

        int type = netInfo.getType();
        if (type == android.net.ConnectivityManager.TYPE_WIFI) {
            return "wifi";
        }
        if (type == android.net.ConnectivityManager.TYPE_MOBILE) {
            return "mobile";
        }
        return "other";
    }

    private boolean isActiveNetworkMetered(Context context) {
        if (context == null) {
            return false;
        }

        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.isActiveNetworkMetered();
    }

    private int getNetworkBandwidthMbps(Context context, boolean downstream) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return 0;
        }

        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return 0;
        }

        android.net.Network network = cm.getActiveNetwork();
        if (network == null) {
            return 0;
        }

        android.net.NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        if (capabilities == null) {
            return 0;
        }

        int kbps = downstream ? capabilities.getLinkDownstreamBandwidthKbps() : capabilities.getLinkUpstreamBandwidthKbps();
        if (kbps <= 0) {
            return 0;
        }

        return Math.max(kbps / 1024, 0);
    }

    private int getNetworkSignalLevel(Context context) {
        if (context == null) {
            return 0;
        }

        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return 0;
        }

        android.net.NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo == null || !netInfo.isConnected()) {
            return 0;
        }

        if (netInfo.getType() == android.net.ConnectivityManager.TYPE_WIFI) {
            android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                return 0;
            }
            android.net.wifi.WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            return android.net.wifi.WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5);
        }

        return 0;
    }

    private String buildDetailPayload(GBDevice device, String syncId, long rangeStartTs, long rangeEndTs) {
        synchronized (metricJsonLock) {
            dataActivity(device, myData2.timeFrom, myData2.timeTo);

            JSONObject params = new JSONObject();
            try {
                params.put("sync_id", syncId);
                params.put("upload_id", syncId);
                params.put("range_start_ts", rangeStartTs);
                params.put("range_end_ts", rangeEndTs);
                params.put("device_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(myData2.timeTo * 1000L)));
                params.put("mac_address", device.getAddress());
                params.put("app_version", getAppVersionStamp());
                params.put("user_activity", jsonActivity.toString());
                params.put("user_sleep", jsonSleep.toString());
                params.put("user_stress", jsonStress.toString());
                params.put("user_spo2", jsonSpo2.toString());
                params.put("user_heart_rate_max", jsonHeartRateMax.toString());
                params.put("user_heart_rate_resting", jsonHeartRateResting.toString());
                params.put("user_heart_rate_manual", jsonHeartRateManual.toString());
                params.put("employee_id", employeeId);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return params.toString();
        }
    }

    // -----------------------------------------------------------------------
    // Queue-wait polling: keeps overlay visible while queued item is pending
    // -----------------------------------------------------------------------

    private void startQueueWait(String phase, GBDevice device,
                                String summaryFingerprint, String detailFingerprint,
                                String detailData) {
        queueWaitHandler.removeCallbacksAndMessages(null);
        queueWaitPhase              = phase;
        queueWaitDevice             = device;
        queueWaitSummaryFingerprint = summaryFingerprint;
        queueWaitDetailFingerprint  = detailFingerprint;
        queueWaitDetailData         = detailData;
        queueWaitPollCount          = 0;

        showUploadProgressOverlay(true);
        updateUploadProgress("Data masuk antrian. Akan retry otomatis; tekan Upload lagi untuk coba sekarang.", 0, 1);
        queueWaitHandler.postDelayed(this::pollQueueWait, QUEUE_WAIT_POLL_MS);
    }

    private void pollQueueWait() {
        Context ctx = getContext();
        if (ctx == null || queueWaitPhase == null) return;

        queueWaitPollCount++;
        int elapsed = queueWaitPollCount * QUEUE_WAIT_POLL_MS / 1000;
        updateUploadProgress("Menunggu server... " + elapsed + "s"
                + " (percobaan " + queueWaitPollCount + "/" + QUEUE_WAIT_MAX_POLLS + ")", 0, 1);

        if (queueWaitPollCount > QUEUE_WAIT_MAX_POLLS) {
            abortQueueWait("Server sibuk. Data tersimpan aman dan akan dikirim otomatis. Tekan Upload lagi untuk coba sekarang.");
            return;
        }

        final String phase              = queueWaitPhase;
        final String summaryFingerprint = queueWaitSummaryFingerprint;
        final String detailFingerprint  = queueWaitDetailFingerprint;
        final GBDevice device           = queueWaitDevice;
        final String detailData         = queueWaitDetailData;
        Activity activity = getActivity();

        new Thread(() -> {
            // Try to flush queued items
            boolean wasInQueue;
            boolean stillInQueue;
            if ("summary".equals(phase)) {
                wasInQueue   = containsFingerprint(ctx, summaryFingerprint);
                sendQueuedUpload(ctx, summaryFingerprint);
                stillInQueue = containsFingerprint(ctx, summaryFingerprint);
            } else {
                wasInQueue   = containsFingerprint(ctx, detailFingerprint);
                sendQueuedUpload(ctx, detailFingerprint);
                stillInQueue = containsFingerprint(ctx, detailFingerprint);
            }

            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                activity.runOnUiThread(() -> {
                    if (queueWaitPhase == null) return;
                    if (wasInQueue && !stillInQueue) {
                        // Item berhasil terkirim
                        onQueueWaitSuccess(phase, device, detailData);
                    } else {
                        // Masih ada, poll lagi
                        queueWaitHandler.postDelayed(this::pollQueueWait, QUEUE_WAIT_POLL_MS);
                    }
                });
            }
        }).start();
    }

    private void onQueueWaitSuccess(String phase, GBDevice device, String detailData) {
        String queuedDetailFingerprint = queueWaitDetailFingerprint;
        queueWaitPhase = null;
        queueWaitHandler.removeCallbacksAndMessages(null);
        refreshUploadNotificationState();

        if ("summary".equals(phase)) {
            toast(requireActivity(), "Summary terkirim! Mengirim detail...", Toast.LENGTH_SHORT, GB.INFO);
            updateUploadProgress("Mengirim detail...", 1, 2);
            Context ctx = getContext();
            if (ctx != null && queuedDetailFingerprint != null && !queuedDetailFingerprint.isEmpty()
                    && containsFingerprint(ctx, queuedDetailFingerprint)) {
                startQueueWait("detail", device, null, queuedDetailFingerprint, null);
            } else if (device != null && detailData != null) {
                sendDetail(device, detailData, false, true);
            } else {
                showUploadProgressOverlay(false);
                startActivity(new Intent(getActivity(), TicketActivity.class));
            }
        } else {
            updateUploadProgress("Upload selesai!", 2, 2);
            toast(requireActivity(), "Data berhasil dikirim!", Toast.LENGTH_SHORT, GB.INFO);
            showUploadProgressOverlay(false);
            startActivity(new Intent(getActivity(), TicketActivity.class));
        }
    }

    private void abortQueueWait(String message) {
        queueWaitHandler.removeCallbacksAndMessages(null);
        queueWaitPhase = null;
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            activity.runOnUiThread(() -> {
                showUploadProgressOverlay(false);
                if (message != null && !message.isEmpty()) {
                    toast(requireActivity(), message, Toast.LENGTH_LONG, GB.WARN);
                }
            });
        }
    }

    private boolean containsFingerprint(Context ctx, String fingerprint) {
        if (fingerprint == null || fingerprint.isEmpty()) return false;
        JSONArray queue = PendingUploadQueue.snapshot(ctx);
        for (int i = 0; i < queue.length(); i++) {
            JSONObject item = queue.optJSONObject(i);
            if (item != null && fingerprint.equals(item.optString("fingerprint", ""))) return true;
        }
        return false;
    }

    private void sendQueuedUpload(Context ctx, String fingerprint) {
        if (fingerprint == null || fingerprint.isEmpty()) return;
        JSONArray queue = PendingUploadQueue.snapshot(ctx);
        for (int i = 0; i < queue.length(); i++) {
            JSONObject item = queue.optJSONObject(i);
            if (item != null && fingerprint.equals(item.optString("fingerprint", ""))) {
                sendQueuedUpload(item);
                return;
            }
        }
    }

    // -----------------------------------------------------------------------

    private void flushPendingUploads() {
        flushPendingUploads(false);
    }

    private void flushPendingUploads(boolean showOverlayUi) {
        Context context = getContext();
        if (context != null && !PendingUploadQueue.isEmpty(context) && pendingUploadFlushRunning.compareAndSet(false, true)) {
            if (showOverlayUi) {
                showUploadProgressOverlay(true);
            } else {
                resetUploadProgressStatus();
            }
            new Thread(() -> {
                try {
                    sleepQuietly(PENDING_UPLOAD_FLUSH_JITTER_MS + (long) (Math.random() * PENDING_UPLOAD_FLUSH_JITTER_MS));
                    JSONArray queue = PendingUploadQueue.snapshot(context);
                    int totalItems = queue.length();
                    int flushLimit = maxPendingUploadsPerFlush();
                    long gapMs = pendingUploadGapMs();
                    int processed = 0;
                    
                    if (showOverlayUi) {
                        updateUploadProgress("Memproses antrian upload...", processed, totalItems);
                    }
                    
                    for (int i = 0; i < queue.length(); i++) {
                        JSONObject item = queue.optJSONObject(i);
                        if (item == null) {
                            continue;
                        }
                        
                        if (showOverlayUi) {
                            updateUploadProgress("Mengirim data ke server...", processed + 1, totalItems);
                        }
                        
                        if (!sendQueuedUpload(item)) {
                            break;
                        }
                        processed++;
                        
                        if (showOverlayUi) {
                            updateUploadProgress("Data terkirim", processed, totalItems);
                        }
                        
                        if (processed >= flushLimit) {
                            break;
                        }
                        sleepQuietly(gapMs);
                    }
                    if (PendingUploadQueue.isEmpty(context)) {
                        GB.removeUploadFailedNotification(context);
                    } else {
                        showUploadPendingNotification();
                    }
                } finally {
                    pendingUploadFlushRunning.set(false);
                    if (showOverlayUi) {
                        showUploadProgressOverlay(false);
                    } else {
                        resetUploadProgressStatus();
                    }
                }
            }).start();
        }
    }

    private int maxPendingUploadsPerFlush() {
        return isLocalRouteActive() ? MAX_PENDING_UPLOADS_PER_FLUSH_LOCAL : MAX_PENDING_UPLOADS_PER_FLUSH_PUBLIC;
    }

    private long pendingUploadGapMs() {
        return isLocalRouteActive() ? PENDING_UPLOAD_GAP_MS_LOCAL : PENDING_UPLOAD_GAP_MS_PUBLIC;
    }

    private boolean isLocalRouteActive() {
        if (localStorage == null) {
            return false;
        }

        String localBaseUrl = localStorage.getApiLocalUrl();
        String activeBaseUrl = localStorage.getApiActiveBaseUrl();

        return !localBaseUrl.isEmpty() && localBaseUrl.equalsIgnoreCase(activeBaseUrl);
    }

    private void sleepQuietly(long delayMs) {
        try {
            Thread.sleep(Math.max(delayMs, 0L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean sendQueuedUpload(JSONObject item) {
        Context context = getContext();
        if (context == null) {
            return false;
        }

        String kind = item.optString("kind", "");
        String url = item.optString("url", "");
        String payload = item.optString("payload", "");
        String fingerprint = item.optString("fingerprint", "");
        if (url.isEmpty() || payload.isEmpty()) {
            return true;
        }

        if ("ingest".equals(kind)) {
            // Keep queue compatible with legacy schema: remove old ingest entries.
            PendingUploadQueue.removeByFingerprint(context, fingerprint);
            refreshUploadNotificationState();
            return true;
        }

        PendingUploadQueue.markSending(context, fingerprint);
        Http http = new Http(context, url);
        http.setMethod("post");
        http.setToken(true);
        http.setHeader("X-App-Version", getAppVersionStamp());
        http.setData(payload);
        http.send();

        Integer code = http.getStatusCode();
        if (code != null && (code == 200 || code == 201)) {
            if ("ingest".equals(kind)) {
                JSONObject response = parseJsonSafe(http.getResponse());
                if (!isIngestResponseAcceptable(response, extractUploadId(payload))) {
                    PendingUploadQueue.markFailed(context, fingerprint, code, "Ingest mismatch parsed<accepted");
                    showUploadPendingNotification();
                    return false;
                }
            }
            PendingUploadQueue.markSentAndRemove(context, fingerprint);
            if ("summary".equals(kind) && localStorage != null) {
                localStorage.clearSummaryWeekCache();
            }
            refreshUploadNotificationState();
            return true;
        }

        PendingUploadQueue.markFailed(context, fingerprint, code == null ? 0 : code, http.getErrorMessage("Upload retry belum berhasil"));
        showUploadPendingNotification();
        return false;
    }

    private boolean queuePendingUpload(String kind, String url, String payload, String dedupeKey) {
        try {
            Context context = getContext();
            if (context == null) {
                return false;
            }
            return PendingUploadQueue.enqueue(context, PendingUploadQueue.createItem(kind, url, payload, dedupeKey));
        } catch (JSONException e) {
            LOG.error("Failed to queue pending upload", e);
            return false;
        }
    }

    private String summaryQueueKey(GBDevice device) {
        return "summary|" + employeeId + "|" + device.getAddress() + "|" + myData1.timeFrom + "|" + myData1.timeTo;
    }

    private String detailQueueKey(GBDevice device) {
        return "detail|" + employeeId + "|" + device.getAddress() + "|" + myData2.timeFrom + "|" + myData2.timeTo;
    }

    private String ingestQueueKey(GBDevice device, String uploadId, int chunkIndex) {
        return "ingest|" + employeeId + "|" + device.getAddress() + "|" + uploadId + "|" + chunkIndex;
    }

    private boolean shouldQueueUpload(Integer code) {
        if (code == null || code == 0) {
            return true;
        }

        return code == 408 || code == 429 || code == 500 || code == 502 || code == 503 || code == 504;
    }

    private void showUploadPendingNotification() {
        Context context = getContext();
        if (context != null) {
            if (PendingUploadQueue.isEmpty(context)) {
                GB.removeUploadFailedNotification(context);
                return;
            }
            GB.updateUploadFailedNotification(buildUploadPendingNotificationText(context), context);
        }
    }

    private void restorePendingQueueOnColdStart() {
        Context context = getContext();
        if (context == null || queueRestoreOnLaunch) {
            return;
        }

        queueRestoreOnLaunch = true;
        PendingUploadRetryManager.scheduleRetryIfNeeded(context);
        refreshUploadNotificationState();
    }

    private void refreshUploadNotificationState() {
        Context context = getContext();
        if (context != null) {
            if (PendingUploadQueue.isEmpty(context)) {
                GB.removeUploadFailedNotification(context);
                resetUploadProgressStatus();
            } else {
                showUploadPendingNotification();
            }
        }
    }

    private String buildUploadPendingNotificationText(Context context) {
        return PendingUploadRetryManager.buildNotificationText(context);
    }

    private void showUploadProgressOverlay(boolean show) {
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            activity.runOnUiThread(() -> {
                if (uploadProgressOverlay != null) {
                    uploadProgressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
                    
                    // Lock/unlock UI buttons
                    if (btnSync != null) btnSync.setEnabled(!show);
                    if (btnReload != null) btnReload.setEnabled(!show);
                    if (btnP5m != null) btnP5m.setEnabled(!show);
                    if (btnZona != null) btnZona.setEnabled(!show);
                    if (btnNotification != null) btnNotification.setEnabled(!show);
                    if (scrollView != null) scrollView.setEnabled(!show);
                    
                    if (!show) {
                        // Reset text when hiding
                        if (uploadProgressTitle != null) {
                            uploadProgressTitle.setText("Mengirim Data...");
                        }
                        if (uploadProgressText != null) {
                            uploadProgressText.setText("Mohon tunggu, jangan tutup aplikasi");
                        }
                        resetUploadProgressStatus();
                    }
                }
            });
        }
    }

    private void updateUploadProgress(String message, int current, int total) {
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            activity.runOnUiThread(() -> {
                if (uploadProgressText != null) {
                    uploadProgressText.setText(message);
                }
                if (uploadQueueStatus != null) {
                    if (total > 0) {
                        uploadQueueStatus.setVisibility(View.VISIBLE);
                        uploadQueueStatus.setText("Proses: " + current + " dari " + total + " data");
                    } else {
                        resetUploadProgressStatus();
                    }
                }
            });
        }
    }

    private void resetUploadProgressStatus() {
        if (uploadQueueStatus != null) {
            uploadQueueStatus.setText("");
            uploadQueueStatus.setVisibility(View.GONE);
        }
    }

    private void reloadData(View view) {
        Animation anim = new AlphaAnimation(1, 0.5f);
        anim.setDuration(50);
        anim.setRepeatMode(Animation.REVERSE);
        btnReload.startAnimation(anim);

        GBDevice activeDevice = resolvePrimaryWearableDevice();
        if (activeDevice != null) {
            Calendar today = GregorianCalendar.getInstance();
            now = (Calendar) today.clone();
            boolean fetchStarted = triggerRecordedDataFetch(activeDevice, true);
            refresh();
            if (fetchStarted) {
                queueWaitHandler.postDelayed(this::refresh, 15000L);
            }
        } else {
            toast(requireActivity(), "Device not found", Toast.LENGTH_SHORT, GB.ERROR);
        }
    }

    private boolean triggerRecordedDataFetch(GBDevice device, boolean force) {
        if (device == null || device.getDeviceCoordinator() == null || !device.getDeviceCoordinator().supportsActivityTracking()) {
            return false;
        }

        long nowMs = System.currentTimeMillis();
        if (!force && nowMs - lastRecordedDataFetchMs < RECORDED_DATA_FETCH_COOLDOWN_MS) {
            return false;
        }

        lastRecordedDataFetchMs = nowMs;
        if (!device.isConnected()) {
            if (recordedDataFetchPending) {
                return true;
            }
            recordedDataFetchPending = true;
            GBApplication.deviceService(device).connect();
            scheduleRecordedDataFetchWhenReady(device, 0);
            return true;
        }

        GBApplication.deviceService(device).onFetchRecordedData(RecordedDataTypes.TYPE_SYNC);
        return true;
    }

    private void scheduleRecordedDataFetchWhenReady(GBDevice device, int attempt) {
        final long[] delaysMs = new long[]{3000L, 8000L, 15000L};
        if (attempt >= delaysMs.length) {
            recordedDataFetchPending = false;
            return;
        }

        queueWaitHandler.postDelayed(() -> {
            if (device != null && (device.isConnected() || device.isInitialized())) {
                recordedDataFetchPending = false;
                GBApplication.deviceService(device).onFetchRecordedData(RecordedDataTypes.TYPE_SYNC);
                return;
            }

            scheduleRecordedDataFetchWhenReady(device, attempt + 1);
        }, delaysMs[attempt]);
    }

    private void showImage() {
        String localPhotoPath = localStorage.getLocalProfilePhotoPath();
        if (!localPhotoPath.isEmpty()) {
            Bitmap localPhoto = ImageUtils.decodeSampledBitmapFromFile(localPhotoPath, 320, 320);
            if (localPhoto != null) {
                imageProfile.setImageBitmap(localPhoto);
                imageProfile_.setImageBitmap(localPhoto);
                return;
            }
        }

        if (employeePhoto == null || employeePhoto.equals("null")) return;
        try {
            String fileName = FileUtils.makeValidFileName(employeePhoto.replace("avatar/", ""));
            File targetFile = new File(FileUtils.getExternalFilesDir(), fileName);
            if (targetFile.exists()) {
                Bitmap photo = ImageUtils.decodeSampledBitmapFromFile(targetFile.getPath(), 320, 320);
                imageProfile.setImageBitmap(photo);
                imageProfile_.setImageBitmap(photo);
            } else {
                downloadImage();
            }
        } catch (IOException e) {
            LOG.error("show image error: ", e);
        }
    }

    private void downloadImage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (URL imageUrl : ApiUrl.candidateUrls(getString(R.string.base_url).replace("/api", "/image/") + employeePhoto)) {
                        HttpURLConnection conn = null;
                        for (int attempt = 1; attempt <= MAX_NETWORK_ATTEMPTS; attempt++) {
                            try {
                                conn = (HttpURLConnection) imageUrl.openConnection();
                                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                                conn.setReadTimeout(READ_TIMEOUT_MS);
                                conn.setUseCaches(false);

                                int statusCode = conn.getResponseCode();
                                if (statusCode < 200 || statusCode > 299) {
                                    if (!shouldRetryNetwork(statusCode) || attempt == MAX_NETWORK_ATTEMPTS) {
                                        LOG.error("download image error: unexpected status {}", statusCode);
                                        break;
                                    }
                                    sleepBeforeRetry(attempt);
                                    continue;
                                }

                                Bitmap photo = ImageUtils.decodeSampledBitmapFromStream(conn.getInputStream(), 320, 320);
                                if (photo == null) {
                                    break;
                                }

                                String fileName = FileUtils.makeValidFileName(employeePhoto.replace("avatar/", ""));
                                File targetFile = new File(FileUtils.getExternalFilesDir(), fileName);
                                FileOutputStream fOut = new FileOutputStream(targetFile);
                                photo.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
                                fOut.flush();
                                fOut.close();
                                break;
                            } catch (Exception e) {
                                e.printStackTrace();
                                if (attempt == MAX_NETWORK_ATTEMPTS) {
                                    break;
                                }
                                sleepBeforeRetry(attempt);
                            } finally {
                                if (conn != null) {
                                    conn.disconnect();
                                    conn = null;
                                }
                            }
                        }
                    }
                } catch (MalformedURLException e) {
                    LOG.error("download image URL error: ", e);
                }

                Activity activity = getActivity();
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    activity.runOnUiThread(new Runnable() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            try {
                                String fileName = FileUtils.makeValidFileName(employeePhoto.replace("avatar/", ""));
                                File targetFile = new File(FileUtils.getExternalFilesDir(), fileName);
                                Bitmap photo = ImageUtils.decodeSampledBitmapFromFile(targetFile.getPath(), 320, 320);
                                imageProfile.setImageBitmap(photo);
                                imageProfile_.setImageBitmap(photo);
                            } catch (IOException e) {
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private boolean shouldRetryNetwork(int statusCode) {
        return statusCode == 408 || statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    private void sleepBeforeRetry(int attempt) {
        long delay = RETRY_BACKOFF_BASE_MS * attempt;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void take_share_screenshot(Context context, boolean share) {
        View rootView = getView();
        if (rootView == null || context == null) {
            return;
        }

        final TextView textNama_ = rootView.findViewById(R.id.textNama_);
        final TextView textNik_ = rootView.findViewById(R.id.textNik_);
        final TextView textDepartemen_ = rootView.findViewById(R.id.textDepartemen_);
        final TextView textMess_ = rootView.findViewById(R.id.textMess_);

        textNama_.setText(textNama.getText());
        textNik_.setText(textNik.getText());
        textDepartemen_.setText(textDepartemen.getText());
        textMess_.setText(textMess.getText());

        final LinearLayout layout = rootView.findViewById(R.id.my_dashboard);
        int width = layout.getWidth();
        int height = Math.max(layout.getHeight(), layout.getMeasuredHeight());
        if (width <= 0 || height <= 0) {
            int widthSpec = View.MeasureSpec.makeMeasureSpec(getResources().getDisplayMetrics().widthPixels, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            layout.measure(widthSpec, heightSpec);
            width = layout.getMeasuredWidth();
            height = layout.getMeasuredHeight();
            layout.layout(0, 0, width, height);
        }
        Bitmap screenShot = getScreenShot(layout, width, height, context);
        String fileName = FileUtils.makeValidFileName("Screenshot-" + "Dashboard-" + DateTimeUtils.formatIso8601(new Date(Calendar.getInstance().getTimeInMillis())) + ".png");

        try {
            File targetFile = new File(FileUtils.getExternalFilesDir(), fileName);
            FileOutputStream fOut = new FileOutputStream(targetFile);
            screenShot.compress(Bitmap.CompressFormat.PNG, 85, fOut);
            fOut.flush();
            fOut.close();
            if (share) {
                shareScreenshot(targetFile, context);
            }
            // GB.toast(getActivity(), "Screenshot saved", Toast.LENGTH_LONG, GB.INFO);
        } catch (IOException e) {
            LOG.error("Error getting screenshot", e);
        }
    }

    private void shareScreenshot(File targetFile, Context context) {
        Uri contentUri = FileProvider.getUriForFile(context,
                context.getApplicationContext().getPackageName() + ".screenshot_provider", targetFile);
        getActivity().grantUriPermission(
                context.getApplicationContext().getPackageName(),
                contentUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION
        );
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sharingIntent.setType("image/*");
        String shareBody = "Jam Tidur - " + textDate.getText() + " - " + textNik.getText() + " - " + textNama.getText();
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.step_streaks_achievements_sharing_title));
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, contentUri);

        try {
            startActivity(Intent.createChooser(sharingIntent, "Share via"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.activity_error_no_app_for_png, Toast.LENGTH_LONG).show();
        }
    }

    private static Bitmap getScreenShot(View view, int width, int height, Context context) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(GBApplication.getWindowBackgroundColor(context));
        view.draw(canvas);
        return bitmap;
    }

    private boolean isDeveloperMode() {
        if (localStorage == null) return false;
        String user = localStorage.getUser();
        return "developer".equals(user);
    }

    private void populateDeveloperSampleData() {
        if (!isDeveloperMode()) return;

        Activity act = getActivity();
        if (act != null && !act.isFinishing() && !act.isDestroyed()) {
            act.runOnUiThread(() -> {
                // Populate myData1 (today) with sample data
                myData1.stepsTotal = 8547;
                myData1.activeMinutesTotal = 124;
                myData1.heartRate = 72;
                myData1.sleepToday = 420; // 7 hours
                myData1.sleepYesterday = 60; // capped at 1 hour
                myData1.sleepRest = 30; // 30 min
                myData1.sleepFrom = (int) (System.currentTimeMillis() / 1000) - (8 * 3600); // 8 hours ago
                myData1.sleepTo = (int) (System.currentTimeMillis() / 1000) - (1 * 3600); // 1 hour ago
                
                // Set sleep details (required for upload validation)
                myData1.sleepTotalMinutes = 480; // today + capped yesterday
                myData1.lightSleepTotalMinutes = 240; // 4 hours light
                myData1.deepSleepTotalMinutes = 180; // 3 hours deep
                myData1.remSleepTotalMinutes = 0;
                myData1.awakeSleepTotalMinutes = 0;
                myData1.sleepWearableTotalMinutes = myData1.sleepTotalMinutes + myData1.awakeSleepTotalMinutes;

                // Populate myData2 (yesterday) with sample data
                myData2.stepsTotal = 7823;
                myData2.activeMinutesTotal = 98;
                myData2.heartRate = 70;
                
                // Set Fit to Work (all Yes for developer mode)
                isFit1 = 1; // Fit 1: Ya
                isFit2 = 1; // Fit 2: Ya
                isFit3 = 1; // Fit 3: Ya
                
                // Update button colors to reflect fit to work status
                btnFit1Ya.setBackgroundColor(getResources().getColor(R.color.chart_deep_sleep_dark));
                btnFit1Tdk.setBackgroundColor(getResources().getColor(R.color.chart_deep_sleep_light));
                btnFit2Ya.setBackgroundColor(getResources().getColor(R.color.chart_deep_sleep_dark));
                btnFit2Tdk.setBackgroundColor(getResources().getColor(R.color.chart_deep_sleep_light));
                btnFit3Ya.setBackgroundColor(getResources().getColor(R.color.chart_deep_sleep_dark));
                btnFit3Tdk.setBackgroundColor(getResources().getColor(R.color.chart_deep_sleep_light));
                
                // Set P5M (required for upload validation)
                String p5mKey = textNik.getText() + "_" + textDate.getText();
                localStorage.setP5M(p5mKey);

                // Generate sample JSON data for upload
                try {
                    // Sample activity data (hourly)
                    jsonActivity = new JSONArray();
                    long baseTime = System.currentTimeMillis() / 1000 - (24 * 3600);
                    for (int i = 0; i < 24; i++) {
                        JSONObject activity = new JSONObject();
                        activity.put("timestamp", baseTime + (i * 3600));
                        activity.put("steps", 300 + (int)(Math.random() * 200));
                        activity.put("intensity", 50 + (int)(Math.random() * 50));
                        activity.put("heart_rate", 65 + (int)(Math.random() * 20));
                        jsonActivity.put(activity);
                    }

                    // Sample sleep data
                    jsonSleep = new JSONArray();
                    JSONObject sleep = new JSONObject();
                    sleep.put("start", myData1.sleepFrom);
                    sleep.put("end", myData1.sleepTo);
                    sleep.put("deep_sleep", 180);
                    sleep.put("light_sleep", 240);
                    jsonSleep.put(sleep);

                    // Sample stress data
                    jsonStress = new JSONArray();
                    for (int i = 0; i < 10; i++) {
                        JSONObject stress = new JSONObject();
                        stress.put("timestamp", baseTime + (i * 7200));
                        stress.put("value", 20 + (int)(Math.random() * 40));
                        jsonStress.put(stress);
                    }

                    // Sample SpO2 data
                    jsonSpo2 = new JSONArray();
                    for (int i = 0; i < 5; i++) {
                        JSONObject spo2 = new JSONObject();
                        spo2.put("timestamp", baseTime + (i * 14400));
                        spo2.put("value", 95 + (int)(Math.random() * 4));
                        jsonSpo2.put(spo2);
                    }

                    // Sample heart rate data
                    jsonHeartRateMax = new JSONArray();
                    JSONObject hrMax = new JSONObject();
                    hrMax.put("timestamp", baseTime);
                    hrMax.put("value", 145);
                    jsonHeartRateMax.put(hrMax);

                    jsonHeartRateResting = new JSONArray();
                    JSONObject hrResting = new JSONObject();
                    hrResting.put("timestamp", baseTime);
                    hrResting.put("value", 58);
                    jsonHeartRateResting.put(hrResting);

                    jsonHeartRateManual = new JSONArray();

                } catch (JSONException e) {
                    LOG.error("Error generating sample data", e);
                }

                // Update UI
                draw();

                // Show toast
                toast(requireActivity(), "✅ Sample data generated for testing!", Toast.LENGTH_LONG, GB.INFO);
            });
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
        private long sleepWearableTotalMinutes;
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
            sleepWearableTotalMinutes = 0;
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
                    sleepWearableTotalMinutes == 0 &&
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

        private boolean isSelectedDevice(GBDevice device) {
            return device != null
                    && (showAllDevices || showDeviceList == null || showDeviceList.contains(device.getAddress()));
        }

        private GBDevice getPrimaryActivityDevice(List<GBDevice> devices) {
            GBDevice selectedInitialized = null;
            GBDevice anyInitialized = null;
            GBDevice selectedTracking = null;

            for (GBDevice dev : devices) {
                if (dev == null || dev.getDeviceCoordinator() == null) {
                    continue;
                }

                boolean selected = isSelectedDevice(dev);
                boolean supportsActivity = dev.getDeviceCoordinator().supportsActivityTracking();

                if (selected && supportsActivity && dev.getState() == GBDevice.State.INITIALIZED) {
                    selectedInitialized = dev;
                    break;
                }

                if (anyInitialized == null && supportsActivity && dev.getState() == GBDevice.State.INITIALIZED) {
                    anyInitialized = dev;
                }

                if (selectedTracking == null && selected && supportsActivity) {
                    selectedTracking = dev;
                }
            }

            if (selectedInitialized != null) {
                return selectedInitialized;
            }
            if (anyInitialized != null) {
                return anyInitialized;
            }
            return selectedTracking;
        }

        private int getStepsTotal() {
            List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
            int totalSteps = 0;
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                GBDevice dev = getPrimaryActivityDevice(devices);
                if (dev != null) {
                    totalSteps += (int) getDailyTotals(dev, dbHandler).getSteps();
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

                totalY = getOffCycleSleepTotals(provider, sleep1 - (12 * 3600), sleep1);

                Calendar restStart = (Calendar) today.clone();
                restStart.set(Calendar.HOUR_OF_DAY, 11);
                restStart.set(Calendar.MINUTE, 0);
                restStart.set(Calendar.SECOND, 0);
                Calendar restEnd = (Calendar) today.clone();
                restEnd.set(Calendar.HOUR_OF_DAY, 14);
                restEnd.set(Calendar.MINUTE, 0);
                restEnd.set(Calendar.SECOND, 0);
                ActivityAmounts amountRest = analysis.calculateActivityAmounts(provider.getAllActivitySamples(
                        (int) (restStart.getTimeInMillis() / 1000),
                        (int) (restEnd.getTimeInMillis() / 1000)
                ));
                totalR = getTotalsSleepForActivityAmounts(amountRest);

                if ((totalR[0] + totalR[1] + totalR[2]) > 60) {
                    totalR[0] = 60;
                    totalR[1] = 0;
                    totalR[2] = 0;
                }

                sleepType = "night";
            } else {
                today.add(Calendar.HOUR, 6);

                int sleep1 = (int) (today.getTimeInMillis() / 1000);
                totalY = getOffCycleSleepTotals(provider, sleep1 - (12 * 3600), sleep1);

                Calendar restStart = (Calendar) today.clone();
                restStart.add(Calendar.DAY_OF_MONTH, -1);
                restStart.set(Calendar.HOUR_OF_DAY, 23);
                restStart.set(Calendar.MINUTE, 0);
                restStart.set(Calendar.SECOND, 0);
                Calendar restEnd = (Calendar) today.clone();
                restEnd.set(Calendar.HOUR_OF_DAY, 2);
                restEnd.set(Calendar.MINUTE, 0);
                restEnd.set(Calendar.SECOND, 0);
                ActivityAmounts amountRest = analysis.calculateActivityAmounts(provider.getAllActivitySamples(
                        (int) (restStart.getTimeInMillis() / 1000),
                        (int) (restEnd.getTimeInMillis() / 1000)
                ));
                totalR = getTotalsSleepForActivityAmounts(amountRest);

                if ((totalR[0] + totalR[1] + totalR[2]) > 60) {
                    totalR[0] = 60;
                    totalR[1] = 0;
                    totalR[2] = 0;
                }

                sleepType = "day";
            }

            sleepFrom = (int) (today.getTimeInMillis() / 1000);
            int mainSleepWindowHours = Objects.equals(sleepType, "night") ? 18 : 12;
            sleepTo = sleepFrom + (mainSleepWindowHours * 3600);

            ActivityAmounts amountSleep = analysis.calculateActivityAmounts(provider.getAllActivitySamples(sleepFrom, sleepTo));
            long[] totalS = getTotalsSleepForActivityAmounts(amountSleep);

            totalT[0] = totalS[0];
            totalT[1] = totalS[1];
            totalT[2] = totalS[2];

            long wearableTotalMinutes = totalT[0] + totalT[1] + totalT[2];
            long sleepTodayMinutes = Math.max(0, wearableTotalMinutes - totalS[3]);
            long sleepYesterdayMinutes = totalY[0] + totalY[1] + totalY[2];
            long sleepRestMinutes = totalR[0] + totalR[1] + totalR[2];

            return new long[]{totalS[0], totalS[1], totalS[2], totalS[3], sleepTodayMinutes, sleepYesterdayMinutes, sleepRestMinutes, wearableTotalMinutes};
        }

        private long[] getOffCycleSleepTotals(SampleProvider<? extends ActivitySample> provider, int from, int to) {
            List<? extends ActivitySample> rawSamples = provider.getAllActivitySamples(from, to);
            List<ActivitySample> samples = new ArrayList<>(rawSamples);
            samples.sort(Comparator.comparingInt(ActivitySample::getTimestamp));

            long[] totals = new long[]{0, 0, 0, 0};
            boolean countSleep = true;
            int awakeGapSeconds = 0;

            if (!samples.isEmpty()) {
                ActivitySample first = samples.get(0);
                countSleep = !(first.getTimestamp() <= from + 60 && ActivityKind.isSleep(first.getKind()));
            }

            for (ActivitySample sample : samples) {
                if (sample.getTimestamp() < from || sample.getTimestamp() >= to) {
                    continue;
                }

                ActivityKind kind = sample.getKind();
                boolean sleep = ActivityKind.isSleep(kind);

                if (!countSleep) {
                    if (sleep) {
                        awakeGapSeconds = 0;
                    } else {
                        awakeGapSeconds += 60;
                        if (awakeGapSeconds >= 10 * 60) {
                            countSleep = true;
                        }
                    }
                    continue;
                }

                if (kind == ActivityKind.LIGHT_SLEEP || kind == ActivityKind.SLEEP_ANY) {
                    totals[0] += 1;
                } else if (kind == ActivityKind.DEEP_SLEEP) {
                    totals[1] += 1;
                } else if (kind == ActivityKind.REM_SLEEP) {
                    totals[2] += 1;
                } else if (kind == ActivityKind.AWAKE_SLEEP) {
                    totals[3] += 1;
                }
            }

            return totals;
        }

        private long capYesterdaySleepMinutes(long minutes) {
            if (minutes <= 0) {
                return 0;
            }
            return Math.min(minutes, 60);
        }

        private long getSleepMinutesTotal() {
            List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
            sleepTotalMinutes = 0;
            sleepWearableTotalMinutes = 0;
            lightSleepTotalMinutes = 0;
            deepSleepTotalMinutes = 0;
            remSleepTotalMinutes = 0;
            awakeSleepTotalMinutes = 0;
            sleepToday = 0;
            sleepYesterday = 0;
            sleepRest = 0;
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                GBDevice dev = getPrimaryActivityDevice(devices);
                if (dev != null) {
                    long[] sleep = getSleep(dev, dbHandler);
                    lightSleepTotalMinutes += sleep[0];
                    deepSleepTotalMinutes += sleep[1];
                    remSleepTotalMinutes += sleep[2];
                    awakeSleepTotalMinutes += sleep[3];
                    sleepToday += sleep[4];
                    sleepYesterday += sleep[5];
                    sleepRest += sleep[6];
                    long sleepWithoutAwake = sleep[4] + capYesterdaySleepMinutes(sleep[5]);
                    sleepTotalMinutes += sleepWithoutAwake;
                    if (sleep.length > 7 && sleep[7] > 0) {
                        sleepWearableTotalMinutes += sleep[7];
                    } else {
                        sleepWearableTotalMinutes += sleep[0] + sleep[1] + sleep[2];
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
                GBDevice dev = getPrimaryActivityDevice(devices);
                if (dev != null) {
                    final DailyTotals dailyTotals = getDailyTotals(dev, dbHandler);
                    if (dailyTotals.getSteps() > 0 && dailyTotals.getDistance() > 0) {
                        totalDistanceCm += dailyTotals.getDistance();
                    } else {
                        totalDistanceCm += dailyTotals.getSteps() * stepLength;
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
                GBDevice dev = getPrimaryActivityDevice(devices);
                if (dev != null) {
                    totalActiveMinutes += getActiveMinutes(dev, dbHandler);
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
                GBDevice dev = getPrimaryActivityDevice(devices);
                if (dev != null) {
                    List<? extends ActivitySample> activitySamples = getAllSamples(dbHandler, dev);
                    int hr = 0;
                    for (ActivitySample row : activitySamples) {
                        if (row.getHeartRate() > 0 && row.getHeartRate() < 255) {
                            hr = row.getHeartRate();
                        }
                    }
                    heartRate += hr;
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
                GBDevice dev = getPrimaryActivityDevice(devices);
                if (dev != null) {
                    int stressValue = getLatestStress(dev, dbHandler, getSleepMetricRangeFromMillis(), getSleepMetricRangeToMillis());
                    if (stressValue == 0) {
                        stressValue = getLatestStress(dev, dbHandler, timeFrom * 1000L, timeTo * 1000L);
                    }
                    stress += stressValue;
                }
            } catch (Exception e) {
                LOG.warn("Could not calculate total stress: ", e);
            }
            if (stress == 0) return "-";
            return stress + " " + getStressType(stress, new int[]{1, 30, 60, 80});
        }

        private int getLatestStress(GBDevice dev, DBHandler dbHandler, long fromMillis, long toMillis) {
            if (dev == null || fromMillis <= 0 || toMillis <= fromMillis) {
                return 0;
            }

            DeviceCoordinator coordinator = dev.getDeviceCoordinator();
            int stressValue = 0;
            if (coordinator != null && coordinator.supportsStressMeasurement()) {
                TimeSampleProvider<? extends StressSample> stressProvider = coordinator.getStressSampleProvider(dev, dbHandler.getDaoSession());
                if (stressProvider != null) {
                    List<? extends StressSample> stressSamples = stressProvider.getAllSamples(fromMillis, toMillis);
                    for (StressSample row : stressSamples) {
                        if (row.getStress() > 0) {
                            stressValue = row.getStress();
                        }
                    }
                }
            }

            if (stressValue == 0) {
                stressValue = getLatestIntFromActivitySamples(dev, dbHandler, fromMillis / 1000L, toMillis / 1000L, "getStress");
            }
            return stressValue;
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
                GBDevice dev = getPrimaryActivityDevice(devices);
                if (dev != null) {
                    int spo2Value = getLatestBloodOxygen(dev, dbHandler, getSleepMetricRangeFromMillis(), getSleepMetricRangeToMillis());
                    if (spo2Value == 0) {
                        spo2Value = getLatestBloodOxygen(dev, dbHandler, timeFrom * 1000L, timeTo * 1000L);
                    }
                    bloodOxygen += spo2Value;
                }
            } catch (Exception e) {
                LOG.warn("Could not calculate total blood oxygen: ", e);
            }
            return bloodOxygen;
        }

        private int getLatestBloodOxygen(GBDevice dev, DBHandler dbHandler, long fromMillis, long toMillis) {
            if (dev == null || fromMillis <= 0 || toMillis <= fromMillis) {
                return 0;
            }

            DeviceCoordinator coordinator = dev.getDeviceCoordinator();
            int spo2Value = 0;
            if (coordinator != null && coordinator.supportsSpo2(dev)) {
                TimeSampleProvider<? extends Spo2Sample> spo2Provider = coordinator.getSpo2SampleProvider(dev, dbHandler.getDaoSession());
                if (spo2Provider != null) {
                    List<? extends Spo2Sample> spo2Samples = spo2Provider.getAllSamples(fromMillis, toMillis);
                    for (Spo2Sample row : spo2Samples) {
                        if (row.getSpo2() > 0) {
                            spo2Value = row.getSpo2();
                        }
                    }
                }
            }

            if (spo2Value == 0) {
                spo2Value = getLatestIntFromActivitySamples(dev, dbHandler, fromMillis / 1000L, toMillis / 1000L, "getSpo2");
            }
            return spo2Value;
        }

        private int getLatestIntFromActivitySamples(GBDevice dev, DBHandler dbHandler, long fromSeconds, long toSeconds, String methodName) {
            if (dev == null || methodName == null || fromSeconds <= 0 || toSeconds <= fromSeconds) {
                return 0;
            }

            try {
                SampleProvider<? extends ActivitySample> provider = getProvider(dbHandler, dev);
                if (provider == null) {
                    return 0;
                }

                List<? extends ActivitySample> samples = provider.getAllActivitySamples((int) fromSeconds, (int) toSeconds);
                int latestValue = 0;
                for (ActivitySample row : samples) {
                    try {
                        java.lang.reflect.Method method = row.getClass().getMethod(methodName);
                        Object value = method.invoke(row);
                        if (value instanceof Number) {
                            int intValue = ((Number) value).intValue();
                            if (intValue > 0) {
                                latestValue = intValue;
                            }
                        }
                    } catch (Exception ignored) {
                        // Some activity sample classes do not expose SpO2/stress fields.
                    }
                }
                return latestValue;
            } catch (Exception e) {
                LOG.warn("Could not read {} from activity samples", methodName, e);
                return 0;
            }
        }

        private long getSleepMetricRangeFromMillis() {
            int rangeFrom = sleepFrom > 0 ? sleepFrom : timeFrom;
            return rangeFrom * 1000L;
        }

        private long getSleepMetricRangeToMillis() {
            int rangeTo = sleepTo > 0 ? sleepTo : timeTo;
            return rangeTo * 1000L;
        }

        private int getBloodPressure() {

            return bloodPressure;
        }

        private List<? extends ActivitySample> getAllSamples(DBHandler db, GBDevice device) {
            SampleProvider<? extends ActivitySample> provider = getProvider(db, device);
            return provider.getAllActivitySamples(timeFrom, timeTo);
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
}

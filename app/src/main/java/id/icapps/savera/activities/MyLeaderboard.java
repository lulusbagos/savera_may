package id.icapps.savera.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;

import id.icapps.savera.GBApplication;
import id.icapps.savera.LocalStorage;
import id.icapps.savera.R;
import id.icapps.savera.activities.charts.ActivityAnalysis;
import id.icapps.savera.database.DBHandler;
import id.icapps.savera.devices.SampleProvider;
import id.icapps.savera.devices.DeviceCoordinator;
import id.icapps.savera.impl.GBDevice;
import id.icapps.savera.model.ActivityAmounts;
import id.icapps.savera.model.ActivitySample;
import id.icapps.savera.model.DailyTotals;
import id.icapps.savera.util.DateTimeUtils;
import id.icapps.savera.util.FileUtils;
import id.icapps.savera.util.GB;

public class MyLeaderboard extends Fragment {
    private static final Logger LOG = LoggerFactory.getLogger(MyLeaderboard.class);
    private static final String PREFS_DEBT_SLEEP = "DEBT_SLEEP_STORAGE";
    private static final String KEY_WEEKLY_HISTORY = "weekly_history";
    private static final String KEY_DATA_CLEARED_V1 = "data_cleared_v1";
    private static final int WEEKLY_RETENTION_DAYS = 7;
    private static final int TARGET_SLEEP_MINUTES = 7 * 60;
    private static final int SLEEP_WINDOW_OFFSET_HOURS = 6; // Match dashboard sleep boundary.
    private static final int SLEEP_WINDOW_TOTAL_HOURS = 12;
    private static final int SUPPORT_SLEEP_CAP_MINUTES = 60;

    private TextView textPeriod, textTotal, textAverage, textRank, textEmployeeName2, textEmployeeNik2;
    private LinearLayout listView;
    private ProgressBar listProgress;
    private LocalStorage localStorage;
    private String employeeCode = "";
    private String employeeName = "";
    private final SimpleDateFormat keyDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat labelDateFormat = new SimpleDateFormat("EEE, dd MMM", new Locale("id", "ID"));

    private static class DebtSleepEntry {
        private String dateKey;
        private long timestamp;
        private long sleepMinutes;
        private long debtMinutes;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.my_leaderboard, container, false);

        textPeriod = view.findViewById(R.id.textPeriod);
        textTotal = view.findViewById(R.id.textTotal);
        textAverage = view.findViewById(R.id.textAverage);
        textRank = view.findViewById(R.id.textRank);
        listView = view.findViewById(R.id.listView);
        listProgress = view.findViewById(R.id.listProgress);
        textEmployeeName2 = view.findViewById(R.id.textEmployeeName2);
        textEmployeeNik2 = view.findViewById(R.id.textEmployeeNik2);

        localStorage = new LocalStorage(view.getContext());
        if (!localStorage.getEmployee().isEmpty() && !localStorage.getEmployee().isBlank()) {
            try {
                JSONObject jsonEmployee = new JSONObject(localStorage.getEmployee());
                employeeCode = safeString(jsonEmployee, "code", "");
                employeeName = safeString(jsonEmployee, "fullname", "");
            } catch (JSONException e) {
                LOG.warn("Failed to parse employee for leaderboard, continue with defaults", e);
                employeeCode = "";
                employeeName = "";
            }
        }

        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                assert getActivity() != null;
                ((HomeActivity) getActivity()).changeViewPagerPostition(0);
            }
        });

        ImageButton btnShare = view.findViewById(R.id.btnShare);

        View.OnClickListener shareListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                take_share_screenshot(getActivity(), true);
            }
        };
        btnShare.setOnClickListener(shareListener);

        ImageButton btnDownload = view.findViewById(R.id.btnDownload);
        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                take_share_screenshot(getActivity(), false);
            }
        });

        clearDebtSleepOnce();
        loadDebtSleep();

        return view;
    }

    private void clearDebtSleepOnce() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_DEBT_SLEEP, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_DATA_CLEARED_V1, false)) {
            prefs.edit().remove(KEY_WEEKLY_HISTORY).putBoolean(KEY_DATA_CLEARED_V1, true).apply();
        }
    }

    private void loadDebtSleep() {
        listProgress.setVisibility(View.VISIBLE);
        new Thread(() -> {
            List<DebtSleepEntry> entries = calculateWeeklyDebtSleepFromWearable();
            final List<DebtSleepEntry> finalEntries = entries;

            Activity activity = getActivity();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                activity.runOnUiThread(() -> {
                    renderDebtSleep(finalEntries);
                    listProgress.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private List<DebtSleepEntry> calculateWeeklyDebtSleepFromWearable() {
        List<DebtSleepEntry> result = new ArrayList<>();
        GBDevice trackedDevice = resolveTrackedDevice();
        if (trackedDevice == null) {
            return result;
        }

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            Calendar base = Calendar.getInstance();
            int nowTs = (int) (System.currentTimeMillis() / 1000L);
            for (int dayOffset = 0; dayOffset < WEEKLY_RETENTION_DAYS; dayOffset++) {
                Calendar day = (Calendar) base.clone();
                day.add(Calendar.DAY_OF_YEAR, -dayOffset);

                SleepWindowData sleepWindowData = calculateSleepWindowForDebtDay(trackedDevice, dbHandler, day, nowTs);
                long sleepMinutes = Math.max(0, sleepWindowData.sleepMinutes);

                // Only count collected wearable days.
                if (sleepWindowData.sampleCount <= 0) {
                    continue;
                }

                DebtSleepEntry entry = new DebtSleepEntry();
                entry.timestamp = day.getTimeInMillis();
                entry.dateKey = keyDateFormat.format(day.getTime());
                entry.sleepMinutes = sleepMinutes;
                entry.debtMinutes = Math.max(0, TARGET_SLEEP_MINUTES - sleepMinutes);
                result.add(entry);
            }
        } catch (Exception e) {
            LOG.warn("Failed to calculate weekly debt sleep", e);
        }

        sortEntriesDesc(result);
        return result;
    }

    private SleepWindowData calculateSleepWindowForDebtDay(GBDevice device, DBHandler dbHandler, Calendar day, int nowTs) {
        Calendar windowBase = (Calendar) day.clone();
        windowBase.set(Calendar.HOUR_OF_DAY, 0);
        windowBase.set(Calendar.MINUTE, 0);
        windowBase.set(Calendar.SECOND, 0);
        windowBase.set(Calendar.MILLISECOND, 0);

        Calendar sleepBase = (Calendar) windowBase.clone();
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (currentHour < 12) {
            sleepBase.add(Calendar.HOUR_OF_DAY, -SLEEP_WINDOW_OFFSET_HOURS);
        } else {
            sleepBase.add(Calendar.HOUR_OF_DAY, SLEEP_WINDOW_OFFSET_HOURS);
        }

        int sleepFrom = (int) (sleepBase.getTimeInMillis() / 1000L);
        int sleepTo = sleepFrom + (SLEEP_WINDOW_TOTAL_HOURS * 3600);
        Calendar supportStart = (Calendar) sleepBase.clone();
        supportStart.add(Calendar.HOUR_OF_DAY, -SLEEP_WINDOW_OFFSET_HOURS);
        Calendar supportEnd = (Calendar) sleepBase.clone();
        int supportFrom = (int) (supportStart.getTimeInMillis() / 1000L);
        int supportTo = (int) (supportEnd.getTimeInMillis() / 1000L);

        // For current day, avoid projecting future values.
        Calendar now = Calendar.getInstance();
        if (isSameDay(windowBase, now)) {
            sleepTo = Math.min(sleepTo, nowTs);
            supportTo = Math.min(supportTo, nowTs);
        }

        if (sleepTo <= sleepFrom && supportTo <= supportFrom) {
            return new SleepWindowData(0L, 0);
        }

        DeviceCoordinator coordinator = device.getDeviceCoordinator();
        SampleProvider<? extends ActivitySample> provider = coordinator.getSampleProvider(device, dbHandler.getDaoSession());
        ActivityAnalysis analysis = new ActivityAnalysis();

        List<? extends ActivitySample> sleepSamples = sleepTo > sleepFrom
                ? provider.getAllActivitySamples(sleepFrom, sleepTo)
                : Collections.emptyList();
        List<? extends ActivitySample> supportSamples = supportTo > supportFrom
                ? provider.getAllActivitySamples(supportFrom, supportTo)
                : Collections.emptyList();
        if (sleepSamples == null) {
            sleepSamples = Collections.emptyList();
        }
        if (supportSamples == null) {
            supportSamples = Collections.emptyList();
        }

        int sampleCount = sleepSamples.size() + supportSamples.size();
        if (sampleCount <= 0) {
            return new SleepWindowData(0L, 0);
        }

        long mainSleepMinutes = calculateSleepMinutes(analysis, sleepSamples);
        long supportSleepMinutes = Math.min(SUPPORT_SLEEP_CAP_MINUTES, calculateSleepMinutes(analysis, supportSamples));

        return new SleepWindowData(mainSleepMinutes + supportSleepMinutes, sampleCount);
    }

    private long calculateSleepMinutes(ActivityAnalysis analysis, List<? extends ActivitySample> samples) {
        if (samples == null || samples.isEmpty()) {
            return 0L;
        }

        ActivityAmounts amounts = analysis.calculateActivityAmounts(samples);
        long[] totals = DailyTotals.getTotalsSleepForActivityAmounts(amounts);
        return totals[0] + totals[1] + totals[2];
    }

    private boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private static class SleepWindowData {
        private final long sleepMinutes;
        private final int sampleCount;

        private SleepWindowData(long sleepMinutes, int sampleCount) {
            this.sleepMinutes = sleepMinutes;
            this.sampleCount = sampleCount;
        }
    }

    private GBDevice resolveTrackedDevice() {
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        for (GBDevice device : devices) {
            if (device != null && device.getDeviceCoordinator().supportsActivityTracking()) {
                return device;
            }
        }
        return null;
    }

    private void renderDebtSleep(List<DebtSleepEntry> entries) {
        textPeriod.setText("Debt Sleep - 7 Hari");
        String nameVal = employeeName != null && !employeeName.isEmpty() ? employeeName : "-";
        String nikVal = "NIK: " + (employeeCode != null && !employeeCode.isEmpty() ? employeeCode : "-");
        textEmployeeName2.setText(nameVal);
        textEmployeeNik2.setText(nikVal);

        long totalDebtBalance = 0;
        long totalSleep = 0;
        for (DebtSleepEntry entry : entries) {
            long sleepMinutes = Math.max(0, entry.sleepMinutes);
            long dailyBalance = TARGET_SLEEP_MINUTES - sleepMinutes;
            totalDebtBalance += dailyBalance;
            totalSleep += sleepMinutes;
        }

        long avgSleep = entries.isEmpty() ? 0 : totalSleep / entries.size();
        if (totalDebtBalance > 0) {
            textTotal.setText(formatMinutes(totalDebtBalance));
            textTotal.setTextColor(Color.parseColor("#B91C1C"));
        } else if (totalDebtBalance < 0) {
            textTotal.setText("0 menit");
            textTotal.setTextColor(Color.BLACK);
        } else {
            textTotal.setText("0 menit");
            textTotal.setTextColor(Color.BLACK);
        }
        textAverage.setText(formatMinutes(avgSleep));
        textRank.setText("Target 7 jam/hari - Data " + entries.size() + "/7 hari");

        listView.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (entries.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("Belum ada data tidur wearable untuk 7 hari terakhir.");
            empty.setTextColor(0xFF6B7280);
            empty.setTextSize(12f);
            listView.addView(empty);
            return;
        }

        for (DebtSleepEntry entry : entries) {
            View itemView = inflater.inflate(R.layout.my_debt_sleep_item, listView, false);
            TextView textDate = itemView.findViewById(R.id.textDebtDate);
            TextView textSleep = itemView.findViewById(R.id.textDebtSleep);
            TextView textDebt = itemView.findViewById(R.id.textDebtValue);
            TextView textItemName = itemView.findViewById(R.id.textDebtEmployeeName);
            TextView textItemNik = itemView.findViewById(R.id.textDebtEmployeeNik);

            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(entry.timestamp);
            textDate.setText(labelDateFormat.format(date.getTime()));
            textItemName.setVisibility(View.VISIBLE);
            textItemNik.setVisibility(View.VISIBLE);
            textItemName.setText(employeeName != null && !employeeName.isEmpty() ? employeeName : "-");
            textItemNik.setText("NIK: " + (employeeCode != null && !employeeCode.isEmpty() ? employeeCode : "-"));
            long sleepMinutes = Math.max(0, entry.sleepMinutes);
            long rawDelta = TARGET_SLEEP_MINUTES - sleepMinutes;
            long debtMinutes = Math.max(0, rawDelta);
            long paidMinutes = Math.min(TARGET_SLEEP_MINUTES, sleepMinutes);
            textSleep.setText("Target: 7 jam | Tidur efektif: " + formatMinutes(sleepMinutes) + " | Terbayar: " + formatMinutes(paidMinutes));

            if (rawDelta > 0) {
                textDebt.setText("Hutang: 7 jam - " + formatMinutes(sleepMinutes) + " = " + formatMinutes(debtMinutes));
                textDebt.setTextColor(Color.parseColor("#B91C1C"));
            } else {
                long extraMinutes = Math.abs(rawDelta);
                textDebt.setText("Hutang: 7 jam - " + formatMinutes(sleepMinutes) + " = 0 menit" + (extraMinutes > 0 ? " (Lebih " + formatMinutes(extraMinutes) + ")" : " (Pas Target)"));
                textDebt.setTextColor(Color.BLACK);
            }
            listView.addView(itemView);
        }
    }

    private String formatMinutes(long minutes) {
        long safeMinutes = Math.max(0, minutes);
        long hours = safeMinutes / 60;
        long mins = safeMinutes % 60;
        if (hours <= 0) {
            return mins + " menit";
        }
        if (mins == 0) {
            return hours + " jam";
        }
        return hours + " jam " + mins + " menit";
    }

    private void sortEntriesDesc(List<DebtSleepEntry> entries) {
        Collections.sort(entries, (a, b) -> Long.compare(b.timestamp, a.timestamp));
    }

    private List<DebtSleepEntry> readWeeklyDebtSleepFromLocal() {
        List<DebtSleepEntry> entries = new ArrayList<>();
        Context context = getContext();
        if (context == null) {
            return entries;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_DEBT_SLEEP, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_WEEKLY_HISTORY, "[]");
        boolean pruned = false;
        long now = System.currentTimeMillis();
        long retentionMs = WEEKLY_RETENTION_DAYS * 24L * 60L * 60L * 1000L;

        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) {
                    continue;
                }

                long timestamp = item.optLong("timestamp", 0L);
                if (timestamp <= 0L || (now - timestamp) > retentionMs) {
                    pruned = true;
                    continue;
                }

                DebtSleepEntry entry = new DebtSleepEntry();
                entry.dateKey = item.optString("date", "");
                entry.timestamp = timestamp;
                entry.sleepMinutes = item.optLong("sleep_minutes", 0L);
                entry.debtMinutes = item.optLong("debt_minutes", 0L);
                entries.add(entry);
            }
        } catch (JSONException e) {
            LOG.warn("Failed to read local debt sleep history", e);
        }

        sortEntriesDesc(entries);
        if (entries.size() > WEEKLY_RETENTION_DAYS) {
            entries = new ArrayList<>(entries.subList(0, WEEKLY_RETENTION_DAYS));
            pruned = true;
        }

        if (pruned) {
            storeWeeklyDebtSleep(entries);
        }

        return entries;
    }

    private void storeWeeklyDebtSleep(List<DebtSleepEntry> entries) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        sortEntriesDesc(entries);
        List<DebtSleepEntry> limitedEntries = entries;
        if (entries.size() > WEEKLY_RETENTION_DAYS) {
            limitedEntries = new ArrayList<>(entries.subList(0, WEEKLY_RETENTION_DAYS));
        }

        JSONArray array = new JSONArray();
        for (DebtSleepEntry entry : limitedEntries) {
            JSONObject item = new JSONObject();
            try {
                item.put("date", entry.dateKey);
                item.put("timestamp", entry.timestamp);
                item.put("sleep_minutes", entry.sleepMinutes);
                item.put("debt_minutes", entry.debtMinutes);
                array.put(item);
            } catch (JSONException ignored) {
            }
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_DEBT_SLEEP, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_WEEKLY_HISTORY, array.toString()).apply();
    }

    private void take_share_screenshot(Context context, boolean share) {
        final LinearLayout layoutInner = getView().findViewById(R.id.my_leaderboard_inner);
        int width = layoutInner.getWidth();
        int height = Math.max(layoutInner.getHeight(), layoutInner.getMeasuredHeight());
        if (width <= 0 || height <= 0) {
            int widthSpec = View.MeasureSpec.makeMeasureSpec(getResources().getDisplayMetrics().widthPixels, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            layoutInner.measure(widthSpec, heightSpec);
            width = layoutInner.getMeasuredWidth();
            height = layoutInner.getMeasuredHeight();
            layoutInner.layout(0, 0, width, height);
        }
        Bitmap screenShot = getScreenShot(layoutInner, width, height, context);
        String fileName = FileUtils.makeValidFileName("Screenshot-" + "DebtSleep-" + DateTimeUtils.formatIso8601(new Date(Calendar.getInstance().getTimeInMillis())) + ".png");

        try {
            File targetFile = new File(FileUtils.getExternalFilesDir(), fileName);
            FileOutputStream fOut = new FileOutputStream(targetFile);
            screenShot.compress(Bitmap.CompressFormat.PNG, 85, fOut);
            fOut.flush();
            fOut.close();
            if (share) {
                shareScreenshot(targetFile, context);
            }
            GB.toast(getActivity(), "Screenshot saved", Toast.LENGTH_LONG, GB.INFO);
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
        String shareBody = textPeriod.getText().toString() + " - " + employeeCode + " - " + employeeName;
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

    private String safeString(JSONObject object, String key, String fallback) {
        if (object == null) {
            return fallback;
        }

        String value = object.optString(key, fallback);
        if (value == null) {
            return fallback;
        }

        String normalized = value.trim();
        if (normalized.isEmpty() || "null".equalsIgnoreCase(normalized)) {
            return fallback;
        }

        return normalized;
    }
}

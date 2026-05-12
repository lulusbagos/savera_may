package id.icapps.savera.activities;

import static id.icapps.savera.util.GB.toast;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import id.icapps.savera.GBApplication;
import id.icapps.savera.Http;
import id.icapps.savera.LocalStorage;
import id.icapps.savera.R;
import id.icapps.savera.util.DateTimeUtils;
import id.icapps.savera.util.FileUtils;
import id.icapps.savera.util.GB;
import pl.droidsonroids.gif.GifImageView;

public class TicketActivity extends AppCompatActivity {
    private static final Logger LOG = LoggerFactory.getLogger(TicketActivity.class);
    private static final String UPLOAD_SUCCESS_TEXT = "Upload data sukses";
    private TextView textNama, textNik, textDepartemen, textShift, textArea, textPit, textHauler, textLoader, textTransport, textDate, textTime, textStatus, textMessage, textSleep, textTicketCode, textSleepRuleLabel, textSleepRuleResult;
    private GifImageView iconStatus;
    private LinearLayout loading;
    private LocalStorage localStorage;
    private int companyId;
    private int employeeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ticket);
        Objects.requireNonNull(getSupportActionBar()).hide();
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);

        textNama = findViewById(R.id.textNama);
        textNik = findViewById(R.id.textNik);
        textDepartemen = findViewById(R.id.textDepartemen);
        textShift = findViewById(R.id.textShift);
        textArea = findViewById(R.id.textArea);
        textPit = findViewById(R.id.textPit);
        textHauler = findViewById(R.id.textHauler);
        textLoader = findViewById(R.id.textLoader);
        textTransport = findViewById(R.id.textTransport);
        textDate = findViewById(R.id.textDate);
        textTime = findViewById(R.id.textTime);
        textStatus = findViewById(R.id.textStatus);
        textMessage = findViewById(R.id.textMessage);
        textSleep = findViewById(R.id.textSleep);
        textTicketCode = findViewById(R.id.textTicketCode);
        textSleepRuleLabel = findViewById(R.id.textSleepRuleLabel);
        textSleepRuleResult = findViewById(R.id.textSleepRuleResult);

        iconStatus = findViewById(R.id.iconStatus);

        localStorage = new LocalStorage(TicketActivity.this);
        textNama.setText("-");
        textNik.setText("-");
        textDepartemen.setText("-");
        textShift.setText("-");
        textArea.setText("-");
        textPit.setText("-");
        textHauler.setText("-");
        textLoader.setText("-");
        textTransport.setText("-");
        textDate.setText("-");
        textTime.setText("-");
        textSleep.setText("-");
        textMessage.setText("-");
        if (!localStorage.getEmployee().isEmpty() && !localStorage.getEmployee().isBlank()) {
            try {
                JSONObject jsonEmployee = new JSONObject(localStorage.getEmployee());
                textNama.setText(getSafeText(jsonEmployee, "fullname", "-"));
                textNik.setText(getSafeText(jsonEmployee, "code", "-"));
                textDepartemen.setText(getSafeText(jsonEmployee, "department_name", "-"));
                companyId = jsonEmployee.optInt("company_id", 0);
                employeeId = jsonEmployee.optInt("id", 0);
            } catch (JSONException e) {
                LOG.warn("Failed to parse local employee context for ticket", e);
            }
        }

        applyTicketCode();

        View topBar = findViewById(R.id.topBar);
        final int topBarBasePaddingTopPx = topBar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
            int statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), topBarBasePaddingTopPx + statusTop, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(topBar);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        ImageButton btnShare = findViewById(R.id.btnShare);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                take_share_screenshot(TicketActivity.this, true);
            }
        });

        ImageButton btnDownload = findViewById(R.id.btnDownload);
        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                take_share_screenshot(TicketActivity.this, false);
            }
        });

        getTicket();

        loading = findViewById(R.id.loading);
        loading.setVisibility(View.VISIBLE);
        loading.setAlpha(1.0f);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loading.animate()
                        .translationY(0)
                        .alpha(0.0f)
                        .setDuration(500)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                loading.setVisibility(View.GONE);
                            }
                        });
            }
        }, 8000);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }

    private void getTicket() {
        if (isDeveloperMode()) {
            applyDeveloperDemoTicket();
            return;
        }
        if (employeeId <= 0) {
            textStatus.setText("Data employee belum sinkron");
            textStatus.setTextColor(getResources().getColor(R.color.hrv_status_char_line_color));
            applyTicketCode();
            return;
        }

        String url = getString(R.string.base_url) + "/ticket/" + employeeId;

        new Thread(() -> {
            Http http = new Http(TicketActivity.this, url);
            http.setMethod("get");
            http.setToken(true);
            http.setBypassCache(true);
            http.setCacheTtlSeconds(0);
            http.send();

            runOnUiThread(() -> {
                Integer code = http.getStatusCode();
                if (code == 200) {
                    try {
                        JSONObject response = new JSONObject(http.getResponse());
                        if (response.has("shift") && !response.getString("shift").equals("null")) {
                            textShift.setText(response.getString("shift"));
                        }
                        applyFirstAvailableText(textArea, response,
                                "area",
                                "area_code",
                                "work_area",
                                "location_area");
                        applyFirstAvailableText(textPit, response,
                                "pit",
                                "pit_code",
                                "work_pit",
                                "location_pit");
                        if (response.has("hauler") && !response.getString("hauler").equals("null")) {
                            textHauler.setText(response.getString("hauler"));
                        }
                        if (response.has("loader") && !response.getString("loader").equals("null")) {
                            textLoader.setText(response.getString("loader"));
                        }
                        applyFirstAvailableText(textTransport, response,
                                "transport_code",
                                "kode_transport",
                                "transport",
                                "transportation");
                        if (response.has("date") && !response.getString("date").equals("null")) {
                            textDate.setText(response.getString("date"));
                        }
                        if (response.has("time") && !response.getString("time").equals("null")) {
                            textTime.setText(response.getString("time"));
                        }

                        // Prioritas: total sleep dari dashboard lokal, fallback ke API jika cache belum ada.
                        long sleepMinutes = localStorage.getSleepMinutes();
                        if (sleepMinutes < 0) {
                            sleepMinutes = extractSleepMinutes(response);
                        }
                        if (sleepMinutes >= 0) {
                            textSleep.setText(formatSleepDuration(sleepMinutes));
                            applySleepRuleDecision(sleepMinutes);
                        } else {
                            textSleep.setText("-");
                            textSleepRuleResult.setText("Data durasi tidur tidak ditemukan");
                        }

                        int fit1 = extractFitValue(response, "fit_to_work_q1", "is_fit1");
                        int fit2 = extractFitValue(response, "fit_to_work_q2", "is_fit2");
                        int fit3 = extractFitValue(response, "fit_to_work_q3", "is_fit3");
                        applyFitToWorkSummary(
                                fit1 >= 0 ? fit1 : localStorage.getFit1(),
                                fit2 >= 0 ? fit2 : localStorage.getFit2(),
                            fit3 >= 0 ? fit3 : localStorage.getFit3()
                        );
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (code == 422 || code == 401 || code == 404) {
                    try {
                        JSONObject response = new JSONObject(http.getResponse());
                        String msg = response.optString("message", "Gagal memuat data ticket.");
                        toast(TicketActivity.this, msg, Toast.LENGTH_LONG, GB.ERROR);
                    } catch (JSONException e) {
                        LOG.warn("Failed parsing ticket error response", e);
                        toast(TicketActivity.this, http.getErrorMessage("Gagal memuat data ticket."), Toast.LENGTH_LONG, GB.ERROR);
                    }
                } else {
                    toast(TicketActivity.this, http.getErrorMessage("Gagal memuat data ticket."), Toast.LENGTH_LONG, GB.ERROR);
                }
            });
        }).start();
    }

    private String getSafeText(JSONObject object, String key, String fallback) {
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

    private void take_share_screenshot(Context context, boolean share) {
        final NestedScrollView layout = findViewById(R.id.my_ticket);
        final LinearLayout layoutInner = findViewById(R.id.my_ticket_inner);
        int width = layoutInner.getWidth();
        int height = layoutInner.getHeight();
        if (height <= 0) {
            height = layout.getHeight();
        }
        Bitmap screenShot = getScreenShot(layoutInner, width, height, context);
        String fileName = FileUtils.makeValidFileName("Screenshot-" + "Ticket-" + DateTimeUtils.formatIso8601(new Date(Calendar.getInstance().getTimeInMillis())) + ".png");

        try {
            File targetFile = new File(FileUtils.getExternalFilesDir(), fileName);
            FileOutputStream fOut = new FileOutputStream(targetFile);
            screenShot.compress(Bitmap.CompressFormat.PNG, 85, fOut);
            fOut.flush();
            fOut.close();
            if (share) {
                shareScreenshot(targetFile, context);
            }
            GB.toast(this, "Screenshot saved", Toast.LENGTH_LONG, GB.INFO);
        } catch (IOException e) {
            LOG.error("Error getting screenshot", e);
        }
    }

    private void shareScreenshot(File targetFile, Context context) {
        Uri contentUri = FileProvider.getUriForFile(context,
                context.getApplicationContext().getPackageName() + ".screenshot_provider", targetFile);
        context.grantUriPermission(
                context.getApplicationContext().getPackageName(),
                contentUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION
        );
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sharingIntent.setType("image/*");
        String shareBody = "E-Ticket Temporary Assignment - " + textDate.getText() + " - " + textNik.getText() + " - " + textNama.getText();
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

    private void applyTicketCode() {
        String nik = textNik.getText() == null ? "-" : textNik.getText().toString().trim();
        if (nik.isEmpty()) {
            nik = "-";
        }
        Calendar now = Calendar.getInstance();
        String code = String.format(Locale.ROOT, "ETK-%04d%02d%02d-%s",
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH),
                nik.replaceAll("\\s+", ""));
        textTicketCode.setText(code);
    }

    private void applyFirstAvailableText(TextView target, JSONObject response, String... keys) {
        if (target == null || response == null || keys == null) {
            return;
        }
        for (String key : keys) {
            if (key == null || key.trim().isEmpty()) {
                continue;
            }
            if (response.has(key) && !response.isNull(key)) {
                String value = response.optString(key, "").trim();
                if (!value.isEmpty() && !"null".equalsIgnoreCase(value)) {
                    target.setText(value);
                    return;
                }
            }
        }
    }

    private long extractSleepMinutes(JSONObject response) {
        String[] minuteKeys = {
                "sleep_effective",
                "sleep_effective_minutes",
                "sleep",
                "sleep_minutes",
                "sleep_total_minutes",
                "total_sleep_minutes",
                "sleep_duration_minutes",
                "sleepMinute",
                "sleep_minutes_api"
        };
        for (String key : minuteKeys) {
            if (response.has(key) && !response.isNull(key)) {
                return response.optLong(key, -1);
            }
        }

        String[] durationKeys = {
                "sleep_duration",
                "sleep_total",
                "sleep_text"
        };
        for (String key : durationKeys) {
            if (response.has(key) && !response.isNull(key)) {
                String duration = response.optString(key, "").trim();
                if (duration.contains(":")) {
                    String[] parts = duration.split(":");
                    if (parts.length >= 2) {
                        try {
                            int hours = Integer.parseInt(parts[0].trim());
                            int minutes = Integer.parseInt(parts[1].trim());
                            return (hours * 60L) + minutes;
                        } catch (NumberFormatException ignored) {
                            // Keep fallback flow.
                        }
                    }
                } else if (duration.matches("^\\d+$")) {
                    try {
                        return Long.parseLong(duration);
                    } catch (NumberFormatException ignored) {
                        // Keep fallback flow.
                    }
                }
            }
        }
        return -1;
    }

    private int extractFitValue(JSONObject response, String primaryKey, String secondaryKey) {
        if (response.has(primaryKey) && !response.isNull(primaryKey)) {
            return response.optInt(primaryKey, -1);
        }
        if (response.has(secondaryKey) && !response.isNull(secondaryKey)) {
            return response.optInt(secondaryKey, -1);
        }
        return -1;
    }

    private void applyUploadSuccessState() {
        final int successColor = getResources().getColor(R.color.hrv_status_balanced);
        textStatus.setText(UPLOAD_SUCCESS_TEXT);
        textStatus.setTextColor(successColor);
        textSleepRuleLabel.setVisibility(View.GONE);
        textSleepRuleResult.setVisibility(View.GONE);
        iconStatus.setVisibility(View.GONE);
    }

    private void applyFitToWorkSummary(int fit1, int fit2, int fit3) {
        String fit1Label = fitAnswerLabel(fit1);
        String fit2Label = fitAnswerLabel(fit2);
        String fit3Label = fitAnswerLabel(fit3);

        boolean readyToWork = fit1 == 0 && fit2 == 0 && fit3 == 1;
        String fitDecision = readyToWork ? "Ya, siap bekerja" : "Tidak siap bekerja";
        String fitDetail = "1) Minum obat menyebabkan fatigue: " + fit1Label
                + "\n2) Ada masalah konsentrasi: " + fit2Label
                + "\n3) Siap bekerja aman: " + fit3Label
                + "\nKomitmen/Kesiapan Operator: " + fitDecision;
        textMessage.setText(fitDetail);
    }

    private String formatSleepDuration(long sleepMinutes) {
        long hours = sleepMinutes / 60L;
        long minutes = sleepMinutes % 60L;
        return String.format(Locale.ROOT, "%d jam %02d menit", hours, minutes);
    }

    private String fitAnswerLabel(int value) {
        if (value == 1) {
            return "ya";
        }
        if (value == 0) {
            return "tidak";
        }
        return "belum diisi";
    }

    private void applySleepRuleDecision(long sleepMinutes) {
        applyUploadSuccessState();
    }

    private boolean isDeveloperMode() {
        if (localStorage == null) {
            return false;
        }
        return "developer".equalsIgnoreCase(localStorage.getUser())
                && "developer_token_offline".equals(localStorage.getToken());
    }

    private void applyDeveloperDemoTicket() {
        if (textNama.getText() == null || textNama.getText().toString().trim().isEmpty() || "-".contentEquals(textNama.getText())) {
            textNama.setText("Developer Mode");
        }
        if (textNik.getText() == null || textNik.getText().toString().trim().isEmpty() || "-".contentEquals(textNik.getText())) {
            textNik.setText("DEV001");
        }
        textDepartemen.setText("Engineering");
        textShift.setText("Night Shift");
        textArea.setText("NORTH BLOCK");
        textPit.setText("PIT A1");
        textHauler.setText("HD-785-21");
        textLoader.setText("EX-305");
        textTransport.setText("UDU-BU03");

        Calendar now = Calendar.getInstance();
        textDate.setText(String.format(Locale.ROOT, "%1$td %1$tb %1$tY", now));
        textTime.setText(String.format(Locale.ROOT, "%1$tH:%1$tM", now));

        long demoSleepMinutes = 322; // 5 jam 22 menit
        textSleep.setText(formatSleepDuration(demoSleepMinutes));
        applySleepRuleDecision(demoSleepMinutes);
        applyFitToWorkSummary(0, 0, 1);
        applyTicketCode();
    }
}

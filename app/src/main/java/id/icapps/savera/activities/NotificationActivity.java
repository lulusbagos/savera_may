package id.icapps.savera.activities;

import static id.icapps.savera.util.GB.toast;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.Spanned;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;
import androidx.core.view.WindowCompat;
import androidx.core.widget.NestedScrollView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import id.icapps.savera.Http;
import id.icapps.savera.LocalStorage;
import id.icapps.savera.R;
import id.icapps.savera.util.GB;

public class NotificationActivity extends AppCompatActivity {
    private static final int MAX_RETRY = 2;
    private static final long RETRY_DELAY_MS = 2000L;

    private ProgressBar progress;
    private NestedScrollView scrollView;
    private LinearLayout listContainer;
    private TextView textEmpty;
    private TextView textHeadline;
    private LocalStorage localStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification_center);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);

        localStorage = new LocalStorage(this);
        progress = findViewById(R.id.progress);
        scrollView = findViewById(R.id.notificationScroll);
        listContainer = findViewById(R.id.notificationListContainer);
        textEmpty = findViewById(R.id.textEmpty);
        textHeadline = findViewById(R.id.textHeadline);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        ImageButton btnReload = findViewById(R.id.btnReload);
        btnReload.setOnClickListener(v -> fetchNotifications());

        fetchNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchNotifications();
    }

    private void fetchNotifications() {
        fetchNotificationsWithRetry(0);
    }

    private void fetchNotificationsWithRetry(int attempt) {
        bindLoading(true);
        String url = getString(R.string.base_url) + "/notifications";

        new Thread(() -> {
            Http http = new Http(NotificationActivity.this, url);
            http.setMethod("get");
            http.setToken(true);
            http.setBypassCache(true);
            http.send();

            int code = http.getStatusCode() == null ? 0 : http.getStatusCode();

            if (code >= 500 && attempt < MAX_RETRY) {
                try { Thread.sleep(RETRY_DELAY_MS * (attempt + 1)); } catch (InterruptedException ignored) {}
                fetchNotificationsWithRetry(attempt + 1);
                return;
            }

            runOnUiThread(() -> {
                bindLoading(false);
                if (code == 200) {
                    try {
                        JSONObject response = new JSONObject(http.getResponse());
                        JSONArray data = response.optJSONArray("data");
                        JSONObject meta = response.optJSONObject("meta");
                        bindNotifications(data, meta);
                    } catch (JSONException e) {
                        bindError(getString(R.string.notification_error_invalid_response));
                    }
                } else if (code == 401) {
                    forceLogout();
                } else if (code == 404) {
                    bindError(getString(R.string.notification_error_context));
                } else {
                    bindHttpError(http, R.string.notification_error_load);
                }
            });
        }).start();
    }

    private void bindNotifications(JSONArray rows, JSONObject meta) {
        listContainer.removeAllViews();

        int unreadCount = meta != null ? meta.optInt("unread_count", 0) : 0;
        textHeadline.setText(unreadCount > 0
                ? getString(R.string.notification_headline, unreadCount)
                : getString(R.string.notification_headline_empty));

        if (rows == null || rows.length() == 0) {
            textEmpty.setVisibility(View.VISIBLE);
            return;
        }

        textEmpty.setVisibility(View.GONE);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) {
                continue;
            }
            listContainer.addView(createNotificationCard(row, i == 0));
        }
    }

    private View createNotificationCard(JSONObject row, boolean first) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_premium_card);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        if (!first) {
            params.topMargin = dp(12);
        }
        card.setLayoutParams(params);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        title.setText(row.optString("title", getString(R.string.notification_title_default)));
        title.setTextSize(14);
        title.setTextColor(Color.parseColor("#111111"));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        header.addView(title);

        TextView status = new TextView(this);
        int readStatus = row.optInt("status", 0);
        status.setText(readStatus == 0 ? getString(R.string.notification_status_new) : getString(R.string.notification_status_read));
        status.setTextSize(10);
        status.setTextColor(readStatus == 0 ? Color.parseColor("#B45309") : Color.parseColor("#0C8A03"));
        header.addView(status);
        card.addView(header);

        TextView date = new TextView(this);
        date.setText(formatDate(row.optString("published_at", "")));
        date.setTextSize(11);
        date.setTextColor(Color.parseColor("#6B7280"));
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dateParams.topMargin = dp(6);
        date.setLayoutParams(dateParams);
        card.addView(date);

        TextView body = new TextView(this);
        body.setText(renderHtml(resolveNotificationBodyHtml(row)));
        body.setTextSize(12);
        body.setTextColor(Color.parseColor("#2E2E2E"));
        body.setAutoLinkMask(android.text.util.Linkify.WEB_URLS);
        body.setMovementMethod(LinkMovementMethod.getInstance());
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        bodyParams.topMargin = dp(10);
        body.setLayoutParams(bodyParams);
        card.addView(body);

        card.setOnClickListener(v -> markReadIfNeeded(row.optInt("id", 0), readStatus, status));

        return card;
    }

    private void markReadIfNeeded(int id, int currentStatus, TextView statusView) {
        if (id <= 0 || currentStatus != 0) {
            return;
        }

        String url = getString(R.string.base_url) + "/notifications/" + id + "/read";
        new Thread(() -> {
            Http http = new Http(NotificationActivity.this, url);
            http.setMethod("post");
            http.setData("{}");
            http.setToken(true);
            http.send();

            runOnUiThread(() -> {
                int code = http.getStatusCode() == null ? 0 : http.getStatusCode();
                if (code == 200) {
                    statusView.setText(getString(R.string.notification_status_read));
                    statusView.setTextColor(Color.parseColor("#0C8A03"));
                    fetchNotifications();
                } else if (code == 401) {
                    forceLogout();
                } else if (code == 404) {
                    toast(this, getString(R.string.notification_error_context), Toast.LENGTH_SHORT, GB.ERROR);
                }
            });
        }).start();
    }

    private void forceLogout() {
        localStorage.setToken("");
        localStorage.setSleepUploader(false);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void bindLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        scrollView.setAlpha(loading ? 0.55f : 1f);
    }

    private void bindHttpError(Http http, int fallbackResId) {
        try {
            JSONObject response = new JSONObject(http.getResponse());
            bindError(response.optString("message", getString(fallbackResId)));
        } catch (JSONException e) {
            bindError(getString(fallbackResId));
        }
    }

    private void bindError(String message) {
        textEmpty.setVisibility(View.VISIBLE);
        textEmpty.setText(message);
        toast(this, message, Toast.LENGTH_SHORT, GB.ERROR);
    }

    private Spanned renderHtml(String html) {
        String safeHtml = (html == null || html.isBlank()) ? "<p>-</p>" : html;
        return HtmlCompat.fromHtml(safeHtml, HtmlCompat.FROM_HTML_MODE_LEGACY);
    }

    private String resolveNotificationBodyHtml(JSONObject row) {
        String html = row.optString("message_html", "").trim();
        if (!html.isEmpty()) {
            return html;
        }

        String plain = row.optString("message", "").trim();
        if (!plain.isEmpty()) {
            return "<p>" + plain + "</p>";
        }

        return "<p>-</p>";
    }

    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) {
            return "-";
        }

        try {
            OffsetDateTime value = OffsetDateTime.parse(isoDate);
            return value.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", new Locale("id", "ID")));
        } catch (Exception ignored) {
            return isoDate;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

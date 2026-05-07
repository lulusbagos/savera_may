package id.icapps.savera.activities;

import static id.icapps.savera.util.GB.toast;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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
    private static final long NOTIFICATION_CACHE_MAX_AGE_MS = 5 * 60 * 1000L;

    private ProgressBar progress;
    private NestedScrollView scrollView;
    private LinearLayout listContainer;
    private TextView textEmpty;
    private TextView textHeadline;
    private LocalStorage localStorage;
    private boolean loadedOnce;
    private boolean resumedOnce;
    private boolean fetching;

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
        btnReload.setOnClickListener(v -> fetchNotifications(true));

        loadedOnce = bindCachedNotifications(false);
        fetchNotifications(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!resumedOnce) {
            resumedOnce = true;
            return;
        }

        if (!localStorage.hasFreshNotificationCache(NOTIFICATION_CACHE_MAX_AGE_MS)) {
            fetchNotifications(false);
        }
    }

    private void fetchNotifications(boolean forceNetwork) {
        if (fetching) {
            return;
        }

        if (!forceNetwork && localStorage.hasFreshNotificationCache(NOTIFICATION_CACHE_MAX_AGE_MS) && loadedOnce) {
            return;
        }

        fetchNotificationsWithRetry(0, forceNetwork);
    }

    private void fetchNotificationsWithRetry(int attempt, boolean forceNetwork) {
        fetching = true;
        bindLoading(!loadedOnce || forceNetwork);
        String url = getString(R.string.base_url) + "/notifications";

        new Thread(() -> {
            Http http = new Http(NotificationActivity.this, url);
            http.setMethod("get");
            http.setToken(true);
            http.setBypassCache(forceNetwork);
            http.setCacheTtlSeconds((int) (NOTIFICATION_CACHE_MAX_AGE_MS / 1000L));
            http.send();

            int code = http.getStatusCode() == null ? 0 : http.getStatusCode();

            if (code >= 500 && attempt < MAX_RETRY) {
                try { Thread.sleep(RETRY_DELAY_MS * (attempt + 1)); } catch (InterruptedException ignored) {}
                runOnUiThread(() -> fetchNotificationsWithRetry(attempt + 1, forceNetwork));
                return;
            }

            runOnUiThread(() -> {
                fetching = false;
                bindLoading(false);
                if (code == 200) {
                    try {
                        String responseBody = http.getResponse();
                        JSONObject response = new JSONObject(responseBody);
                        JSONArray data = response.optJSONArray("data");
                        JSONObject meta = response.optJSONObject("meta");
                        localStorage.setNotificationCache(responseBody);
                        bindNotifications(data, meta);
                        loadedOnce = true;
                    } catch (JSONException e) {
                        if (!bindCachedNotifications(true)) {
                            bindError(getString(R.string.notification_error_invalid_response));
                        }
                    }
                } else if (code == 401) {
                    forceLogout();
                } else if (code == 404) {
                    if (!bindCachedNotifications(true)) {
                        bindError(getString(R.string.notification_error_context));
                    }
                } else {
                    if (!bindCachedNotifications(true)) {
                        bindHttpError(http, R.string.notification_error_load);
                    }
                }
            });
        }).start();
    }

    private boolean bindCachedNotifications(boolean showToastOnInvalidCache) {
        String cached = localStorage.getNotificationCache();
        if (cached.trim().isEmpty()) {
            return false;
        }

        try {
            JSONObject response = new JSONObject(cached);
            bindNotifications(response.optJSONArray("data"), response.optJSONObject("meta"));
            loadedOnce = true;
            return true;
        } catch (JSONException e) {
            if (showToastOnInvalidCache) {
                toast(this, getString(R.string.notification_error_invalid_response), Toast.LENGTH_SHORT, GB.ERROR);
            }
            return false;
        }
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

        WebView body = new WebView(this);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(80)
        );
        bodyParams.topMargin = dp(10);
        body.setLayoutParams(bodyParams);
        configureNotificationWebView(body);
        body.loadDataWithBaseURL(null, buildNotificationHtml(row), "text/html", "UTF-8", null);
        card.addView(body);

        final int notificationId = row.optInt("id", 0);
        final int initialReadStatus = readStatus;
        View.OnClickListener readClick = v -> markReadIfNeeded(notificationId, initialReadStatus, status);
        card.setOnClickListener(readClick);
        body.setOnClickListener(readClick);

        return card;
    }

    private void configureNotificationWebView(WebView webView) {
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setDomStorageEnabled(false);
        settings.setDatabaseEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(false);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setDefaultTextEncodingName("UTF-8");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.postDelayed(() -> {
                    int contentHeight = Math.max(dp(40), Math.round(view.getContentHeight() * view.getScale()));
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
                    if (params.height != contentHeight) {
                        params.height = contentHeight;
                        view.setLayoutParams(params);
                    }
                }, 60);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }
        });
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
                    markCachedNotificationRead(id);
                    statusView.setText(getString(R.string.notification_status_read));
                    statusView.setTextColor(Color.parseColor("#0C8A03"));
                } else if (code == 401) {
                    forceLogout();
                } else if (code == 404) {
                    toast(this, getString(R.string.notification_error_context), Toast.LENGTH_SHORT, GB.ERROR);
                }
            });
        }).start();
    }

    private void markCachedNotificationRead(int id) {
        String cached = localStorage.getNotificationCache();
        if (cached.trim().isEmpty()) {
            return;
        }

        try {
            JSONObject response = new JSONObject(cached);
            JSONArray data = response.optJSONArray("data");
            boolean changed = false;

            if (data != null) {
                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.optJSONObject(i);
                    if (item != null && item.optInt("id", 0) == id && item.optInt("status", 0) == 0) {
                        item.put("status", 1);
                        changed = true;
                        break;
                    }
                }
            }

            if (changed) {
                JSONObject meta = response.optJSONObject("meta");
                if (meta != null) {
                    meta.put("unread_count", Math.max(0, meta.optInt("unread_count", 0) - 1));
                }
                localStorage.setNotificationCache(response.toString());
                bindCachedNotifications(false);
            }
        } catch (JSONException ignored) {
        }
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

    private String buildNotificationHtml(JSONObject row) {
        String fullHtml = row.optString("message_html_full", "").trim();
        if (!fullHtml.isEmpty()) {
            return wrapNotificationHtml(fullHtml);
        }

        String html = row.optString("message_html", "").trim();
        if (!html.isEmpty()) {
            return wrapNotificationHtml(html);
        }

        String plain = row.optString("message", "").trim();
        if (!plain.isEmpty()) {
            return wrapNotificationHtml("<p>" + escapeHtml(plain) + "</p>");
        }

        return wrapNotificationHtml("<p>-</p>");
    }

    private String wrapNotificationHtml(String html) {
        if (html == null || html.trim().isEmpty()) {
            html = "<p>-</p>";
        }

        String sanitized = stripScripts(html);
        if (sanitized.matches("(?is).*<html[\\s>].*")) {
            return sanitized;
        }

        return "<!doctype html><html><head>"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<style>"
                + "html,body{margin:0;padding:0;background:transparent;color:#0F172A;font-family:sans-serif;font-size:13px;line-height:1.35;}"
                + "p{margin:0 0 8px;} img,video{max-width:100%;height:auto;} table{max-width:100%;border-collapse:collapse;}"
                + "*{box-sizing:border-box;}"
                + "</style>"
                + "</head><body>"
                + sanitized
                + "</body></html>";
    }

    private String stripScripts(String html) {
        return html.replaceAll("(?is)<script.*?</script>", "");
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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

package id.icapps.savera.activities;

import static id.icapps.savera.util.GB.toast;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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

        WebView body = new WebView(this);
        configureNotificationWebView(body);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        bodyParams.topMargin = dp(10);
        body.setLayoutParams(bodyParams);
        body.loadDataWithBaseURL(null, resolveNotificationBodyDocument(row), "text/html", "UTF-8", null);
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
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setFocusable(false);
        webView.setFocusableInTouchMode(false);
        webView.setLongClickable(false);
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setDomStorageEnabled(false);
        settings.setDatabaseEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(false);
        settings.setTextZoom(100);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                resizeWebViewToContent(view);
            }
        });
    }

    private void resizeWebViewToContent(WebView webView) {
        webView.postDelayed(() -> {
            int contentHeight = (int) Math.ceil(webView.getContentHeight() * webView.getScale());
            int targetHeight = Math.max(dp(72), contentHeight + dp(8));

            if (webView.getLayoutParams() != null && webView.getLayoutParams().height != targetHeight) {
                webView.getLayoutParams().height = targetHeight;
                webView.requestLayout();
            }
        }, 150L);
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

    private String resolveNotificationBodyDocument(JSONObject row) {
        String fullHtml = row.optString("message_html_full", "").trim();
        if (!fullHtml.isEmpty()) {
            return fullHtml;
        }

        String html = row.optString("message_html", "").trim();
        if (!html.isEmpty()) {
            return buildNotificationHtmlDocument(html);
        }

        String plain = row.optString("message", "").trim();
        if (!plain.isEmpty()) {
            return buildNotificationHtmlDocument("<p>" + escapeHtml(plain) + "</p>");
        }

        return buildNotificationHtmlDocument("<p>-</p>");
    }

    private String buildNotificationHtmlDocument(String bodyHtml) {
        String safeBody = (bodyHtml == null || bodyHtml.isBlank()) ? "<p>-</p>" : bodyHtml;
        if (safeBody.toLowerCase(Locale.US).contains("<html")) {
            return safeBody;
        }

        return "<!doctype html><html><head>"
                + "<meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\">"
                + "<style>"
                + "*{box-sizing:border-box}html,body{margin:0;padding:0;background:transparent;color:#0f172a;"
                + "font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;font-size:13px;line-height:1.45}"
                + "body{overflow:hidden}.savera-notification{width:100%;padding:0}.sn-card{border:1px solid #bfdbfe;border-radius:18px;"
                + "padding:14px;background:linear-gradient(135deg,#ffffff 0%,#eff6ff 100%);box-shadow:0 14px 28px rgba(15,23,42,.08)}"
                + ".sn-rich p{margin:0 0 10px}.sn-rich a{color:#0369a1;font-weight:700;text-decoration:none}"
                + ".sn-rich table{width:100%;border-collapse:collapse;margin:8px 0}.sn-rich th,.sn-rich td{border:1px solid #e2e8f0;padding:8px;text-align:left}"
                + ".sn-rich img{max-width:100%;height:auto;border-radius:12px}"
                + "</style></head><body>" + safeBody + "</body></html>";
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

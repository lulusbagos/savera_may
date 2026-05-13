package id.icapps.savera.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import id.icapps.savera.Http;
import id.icapps.savera.LocalStorage;
import id.icapps.savera.externalevents.PendingUploadRetryReceiver;

public final class PendingUploadRetryManager {
    public static final String ACTION_RETRY_PENDING_UPLOADS = "id.icapps.savera.action.RETRY_PENDING_UPLOADS";
    public static final long INITIAL_RETRY_DELAY_MS = 60L * 1000L;
    private static final long NEXT_RETRY_DELAY_MS = 5L * 60L * 1000L;
    private static final int MAX_ITEMS_PER_RUN = 3;
    private static final long GAP_BETWEEN_ITEMS_MS = 1200L;
    private static final String APP_VERSION_STAMP = "Savera 9";
    private static final int REQUEST_CODE_RETRY = 42043;
    private static final Logger LOG = LoggerFactory.getLogger(PendingUploadRetryManager.class);

    private PendingUploadRetryManager() {
    }

    public static void scheduleRetryIfNeeded(Context context) {
        Context appContext = applicationContext(context);
        if (appContext == null) {
            return;
        }
        if (PendingUploadQueue.isEmpty(appContext)) {
            cancelRetry(appContext);
            GB.removeUploadFailedNotification(appContext);
            return;
        }
        scheduleRetry(appContext, INITIAL_RETRY_DELAY_MS);
        GB.updateUploadFailedNotification(buildNotificationText(appContext), appContext);
    }

    public static void scheduleRetry(Context context, long delayMs) {
        Context appContext = applicationContext(context);
        if (appContext == null) {
            return;
        }

        try {
            AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                return;
            }

            long triggerAtMs = System.currentTimeMillis() + Math.max(delayMs, INITIAL_RETRY_DELAY_MS);
            PendingIntent pendingIntent = retryPendingIntent(appContext, PendingIntent.FLAG_UPDATE_CURRENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent);
            }
        } catch (Exception e) {
            LOG.warn("Unable to schedule pending upload retry", e);
        }
    }

    public static void cancelRetry(Context context) {
        Context appContext = applicationContext(context);
        if (appContext == null) {
            return;
        }

        try {
            AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                return;
            }
            PendingIntent pendingIntent = retryPendingIntent(appContext, PendingIntent.FLAG_NO_CREATE);
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
            }
        } catch (Exception e) {
            LOG.warn("Unable to cancel pending upload retry", e);
        }
    }

    public static boolean flush(Context context) {
        Context appContext = applicationContext(context);
        if (appContext == null) {
            return false;
        }

        if (PendingUploadQueue.isEmpty(appContext)) {
            cancelRetry(appContext);
            GB.removeUploadFailedNotification(appContext);
            return true;
        }

        if (!isNetworkAvailable(appContext)) {
            scheduleRetry(appContext, NEXT_RETRY_DELAY_MS);
            GB.updateUploadFailedNotification(buildNotificationText(appContext), appContext);
            return false;
        }

        JSONArray queue = PendingUploadQueue.snapshot(appContext);
        int processed = 0;
        boolean allProcessedInThisRun = true;
        for (int i = 0; i < queue.length(); i++) {
            JSONObject item = queue.optJSONObject(i);
            if (item == null) {
                continue;
            }

            if (PendingUploadQueue.STATUS_SENDING.equals(item.optString("status", ""))) {
                continue;
            }

            if (processed >= MAX_ITEMS_PER_RUN) {
                allProcessedInThisRun = false;
                break;
            }

            if (!sendNow(appContext, item)) {
                allProcessedInThisRun = false;
                break;
            }

            processed++;
            sleepQuietly(GAP_BETWEEN_ITEMS_MS);
        }

        if (PendingUploadQueue.isEmpty(appContext)) {
            cancelRetry(appContext);
            GB.removeUploadFailedNotification(appContext);
            return true;
        }

        scheduleRetry(appContext, allProcessedInThisRun ? INITIAL_RETRY_DELAY_MS : NEXT_RETRY_DELAY_MS);
        GB.updateUploadFailedNotification(buildNotificationText(appContext), appContext);
        return false;
    }

    public static boolean sendNow(Context context, JSONObject item) {
        Context appContext = applicationContext(context);
        if (appContext == null || item == null) {
            return false;
        }

        String kind = item.optString("kind", "");
        String url = item.optString("url", "");
        String payload = item.optString("payload", "");
        String fingerprint = item.optString("fingerprint", "");
        if (url.isEmpty() || payload.isEmpty() || fingerprint.isEmpty()) {
            PendingUploadQueue.removeByFingerprint(appContext, fingerprint);
            return true;
        }

        if ("ingest".equals(kind)) {
            PendingUploadQueue.removeByFingerprint(appContext, fingerprint);
            return true;
        }

        PendingUploadQueue.markSending(appContext, fingerprint);
        Http http = new Http(appContext, url);
        http.setMethod("post");
        http.setToken(true);
        http.setHeader("X-App-Version", APP_VERSION_STAMP);
        http.setData(payload);
        http.setConnectTimeoutMs(8000);
        http.setReadTimeoutMs(15000);
        http.setMaxAttempts(1);
        http.send();

        Integer code = http.getStatusCode();
        if (code != null && (code == 200 || code == 201)) {
            PendingUploadQueue.markSentAndRemove(appContext, fingerprint);
            if ("summary".equals(kind)) {
                new LocalStorage(appContext).clearSummaryWeekCache();
            }
            return true;
        }

        PendingUploadQueue.markFailed(
                appContext,
                fingerprint,
                code == null ? 0 : code,
                http.getErrorMessage("Upload retry belum berhasil")
        );
        return false;
    }

    public static String buildNotificationText(Context context) {
        JSONObject summary = PendingUploadQueue.summary(context);
        int pending = summary.optInt("pending", 0);
        int sending = summary.optInt("sending", 0);
        int failed = summary.optInt("failed", 0);
        int total = summary.optInt("total", pending + sending + failed);
        return "Upload tersimpan aman. Total " + total
                + ", pending " + pending
                + ", sending " + sending
                + ", gagal " + failed
                + ". Akan dicoba otomatis tiap 5 menit.";
    }

    private static PendingIntent retryPendingIntent(Context context, int flags) {
        Intent intent = new Intent(context, PendingUploadRetryReceiver.class);
        intent.setAction(ACTION_RETRY_PENDING_UPLOADS);
        intent.setPackage(context.getPackageName());
        return PendingIntentUtils.getBroadcast(context, REQUEST_CODE_RETRY, intent, flags, false);
    }

    private static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return false;
            }
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private static Context applicationContext(Context context) {
        if (context == null) {
            return null;
        }
        Context appContext = context.getApplicationContext();
        return appContext == null ? context : appContext;
    }

    private static void sleepQuietly(long delayMs) {
        try {
            Thread.sleep(Math.max(delayMs, 0L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

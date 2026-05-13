package id.icapps.savera.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class PendingUploadQueue {
    private static final String PREFS_NAME = "STORAGE_PENDING_UPLOAD_QUEUE";
    private static final String KEY_QUEUE = "QUEUE";
    private static final String KEY_HISTORY = "HISTORY";
    private static final int MAX_QUEUE_ITEMS = 120;
    private static final int MAX_HISTORY_ITEMS = 40;
    private static final long ITEM_TTL_MS = 14L * 24L * 60L * 60L * 1000L;
    private static final long HISTORY_TTL_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final long STALE_SENDING_MS = 5L * 60L * 1000L;
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_SENDING = "sending";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_SENT = "sent";

    private PendingUploadQueue() {
    }

    public static JSONObject createItem(String kind, String url, String payload, String dedupeKey) throws JSONException {
        long now = System.currentTimeMillis();
        JSONObject item = new JSONObject();
        item.put("kind", kind);
        item.put("url", url);
        item.put("payload", payload);
        item.put("created_at", now);
        item.put("updated_at", now);
        item.put("status", STATUS_PENDING);
        item.put("attempts", 0);
        item.put("fingerprint", fingerprint(kind, dedupeKey));
        return item;
    }

    public static synchronized boolean enqueue(Context context, JSONObject item) {
        try {
            JSONArray queue = compactQueue(context, readQueue(context), true);
            String fingerprint = item.getString("fingerprint");
            if (contains(queue, fingerprint)) {
                return false;
            }
            if (queue.length() >= MAX_QUEUE_ITEMS) {
                queue = trimToCapacity(queue, MAX_QUEUE_ITEMS - 1);
            }
            queue.put(item);
            writeQueue(context, queue);
            PendingUploadRetryManager.scheduleRetry(context, PendingUploadRetryManager.INITIAL_RETRY_DELAY_MS);
            return true;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized JSONArray snapshot(Context context) {
        return compactQueue(context, readQueue(context), true);
    }

    public static synchronized void markSending(Context context, String fingerprint) {
        mutateQueueItem(context, fingerprint, item -> {
            long now = System.currentTimeMillis();
            item.put("status", STATUS_SENDING);
            item.put("updated_at", now);
            item.put("last_attempt_at", now);
            item.put("attempts", item.optInt("attempts", 0) + 1);
            return true;
        });
    }

    public static synchronized void markFailed(Context context, String fingerprint, int httpStatus, String errorMessage) {
        mutateQueueItem(context, fingerprint, item -> {
            long now = System.currentTimeMillis();
            item.put("status", STATUS_FAILED);
            item.put("updated_at", now);
            item.put("last_error_at", now);
            if (httpStatus > 0) {
                item.put("last_http_status", httpStatus);
            }
            if (errorMessage != null && !errorMessage.isEmpty()) {
                item.put("last_error", errorMessage);
            }
            appendHistory(context, item, STATUS_FAILED);
            return true;
        });
    }

    public static synchronized void markSentAndRemove(Context context, String fingerprint) {
        JSONArray queue = compactQueue(context, readQueue(context), true);
        JSONArray updated = new JSONArray();
        for (int i = 0; i < queue.length(); i++) {
            JSONObject item = queue.optJSONObject(i);
            if (item == null) {
                continue;
            }

            if (fingerprint.equals(item.optString("fingerprint", ""))) {
                appendHistory(context, item, STATUS_SENT);
                continue;
            }

            updated.put(item);
        }
        writeQueue(context, updated);
    }

    public static synchronized void removeByFingerprint(Context context, String fingerprint) {
        JSONArray queue = compactQueue(context, readQueue(context), true);
        JSONArray updated = new JSONArray();
        for (int i = 0; i < queue.length(); i++) {
            JSONObject item = queue.optJSONObject(i);
            if (item == null) {
                continue;
            }
            if (!fingerprint.equals(item.optString("fingerprint", ""))) {
                updated.put(item);
            }
        }
        writeQueue(context, updated);
    }

    public static synchronized boolean isEmpty(Context context) {
        return compactQueue(context, readQueue(context), true).length() == 0;
    }

    public static synchronized void reset(Context context, boolean clearHistory) {
        writeQueue(context, new JSONArray());
        if (clearHistory) {
            writeHistory(context, new JSONArray());
        }
    }

    public static synchronized JSONObject summary(Context context) {
        JSONArray queue = compactQueue(context, readQueue(context), true);
        JSONArray history = compactHistory(context, readHistory(context), true);
        JSONObject summary = new JSONObject();
        int pending = 0;
        int sending = 0;
        int failed = 0;

        for (int i = 0; i < queue.length(); i++) {
            JSONObject item = queue.optJSONObject(i);
            if (item == null) {
                continue;
            }

            String status = item.optString("status", STATUS_PENDING);
            if (STATUS_SENDING.equals(status)) {
                sending++;
            } else if (STATUS_FAILED.equals(status)) {
                failed++;
            } else {
                pending++;
            }
        }

        JSONObject latest = history.optJSONObject(0);
        try {
            summary.put("total", queue.length());
            summary.put("pending", pending);
            summary.put("sending", sending);
            summary.put("failed", failed);
            summary.put("history_count", history.length());
            if (latest != null) {
                summary.put("last_status", latest.optString("status", ""));
                summary.put("last_kind", latest.optString("kind", ""));
                summary.put("last_time", latest.optLong("updated_at", 0L));
                summary.put("last_http_status", latest.optInt("last_http_status", 0));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return summary;
    }

    private static JSONArray readQueue(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_QUEUE, "[]");
        try {
            return new JSONArray(raw);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private static void writeQueue(Context context, JSONArray queue) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_QUEUE, queue.toString()).apply();
    }

    private static JSONArray readHistory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_HISTORY, "[]");
        try {
            return new JSONArray(raw);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private static void writeHistory(Context context, JSONArray history) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_HISTORY, history.toString()).apply();
    }

    private static boolean contains(JSONArray queue, String fingerprint) {
        for (int i = 0; i < queue.length(); i++) {
            JSONObject item = queue.optJSONObject(i);
            if (item != null && fingerprint.equals(item.optString("fingerprint", ""))) {
                return true;
            }
        }
        return false;
    }

    public static String fingerprint(String kind, String dedupeKey) {
        return Integer.toHexString((kind + "|" + dedupeKey).hashCode());
    }

    private static JSONArray compactQueue(Context context, JSONArray queue, boolean persistChanges) {
        JSONArray updated = new JSONArray();
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (int i = 0; i < queue.length(); i++) {
            JSONObject item = queue.optJSONObject(i);
            if (item == null) {
                changed = true;
                continue;
            }

            long createdAt = item.optLong("created_at", now);
            if (now - createdAt > ITEM_TTL_MS) {
                changed = true;
                continue;
            }

            String status = item.optString("status", "");
            long updatedAt = item.optLong("updated_at", createdAt);
            if (!item.has("status")) {
                try {
                    item.put("status", STATUS_PENDING);
                    item.put("updated_at", createdAt);
                    item.put("attempts", item.optInt("attempts", 0));
                    changed = true;
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            if (STATUS_SENDING.equals(status) && now - updatedAt > STALE_SENDING_MS) {
                try {
                    item.put("status", STATUS_PENDING);
                    item.put("updated_at", now);
                    changed = true;
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            updated.put(item);
        }

        if (persistChanges && changed) {
            writeQueue(context, updated);
        }

        return updated;
    }

    private static JSONArray compactHistory(Context context, JSONArray history, boolean persistChanges) {
        JSONArray updated = new JSONArray();
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (int i = 0; i < history.length(); i++) {
            JSONObject item = history.optJSONObject(i);
            if (item == null) {
                changed = true;
                continue;
            }

            long updatedAt = item.optLong("updated_at", now);
            if (now - updatedAt > HISTORY_TTL_MS) {
                changed = true;
                continue;
            }

            updated.put(item);
        }

        if (updated.length() > MAX_HISTORY_ITEMS) {
            updated = trimHistory(updated, MAX_HISTORY_ITEMS);
            changed = true;
        }

        if (persistChanges && changed) {
            writeHistory(context, updated);
        }

        return updated;
    }

    private static JSONArray trimToCapacity(JSONArray queue, int keepFromTail) {
        if (keepFromTail <= 0 || queue.length() <= keepFromTail) {
            return queue;
        }

        JSONArray trimmed = new JSONArray();
        int start = Math.max(queue.length() - keepFromTail, 0);
        for (int i = start; i < queue.length(); i++) {
            JSONObject item = queue.optJSONObject(i);
            if (item != null) {
                trimmed.put(item);
            }
        }
        return trimmed;
    }

    private static JSONArray trimHistory(JSONArray history, int keepFromHead) {
        if (keepFromHead <= 0 || history.length() <= keepFromHead) {
            return history;
        }

        JSONArray trimmed = new JSONArray();
        for (int i = 0; i < keepFromHead; i++) {
            JSONObject item = history.optJSONObject(i);
            if (item != null) {
                trimmed.put(item);
            }
        }
        return trimmed;
    }

    private static void mutateQueueItem(Context context, String fingerprint, QueueItemMutator mutator) {
        JSONArray queue = compactQueue(context, readQueue(context), true);
        boolean changed = false;
        for (int i = 0; i < queue.length(); i++) {
            JSONObject item = queue.optJSONObject(i);
            if (item == null) {
                continue;
            }

            if (!fingerprint.equals(item.optString("fingerprint", ""))) {
                continue;
            }

            try {
                changed = mutator.mutate(item) || changed;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        if (changed) {
            writeQueue(context, queue);
        }
    }

    private static void appendHistory(Context context, JSONObject sourceItem, String status) {
        JSONArray history = compactHistory(context, readHistory(context), false);
        JSONObject snapshot = new JSONObject();
        long now = System.currentTimeMillis();
        try {
            snapshot.put("fingerprint", sourceItem.optString("fingerprint", ""));
            snapshot.put("kind", sourceItem.optString("kind", ""));
            snapshot.put("status", status);
            snapshot.put("attempts", sourceItem.optInt("attempts", 0));
            snapshot.put("updated_at", now);
            snapshot.put("last_http_status", sourceItem.optInt("last_http_status", 0));
            snapshot.put("last_error", sourceItem.optString("last_error", ""));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JSONArray updated = new JSONArray();
        updated.put(snapshot);
        for (int i = 0; i < history.length() && updated.length() < MAX_HISTORY_ITEMS; i++) {
            JSONObject item = history.optJSONObject(i);
            if (item != null) {
                updated.put(item);
            }
        }
        writeHistory(context, updated);
    }

    private interface QueueItemMutator {
        boolean mutate(JSONObject item) throws JSONException;
    }

}

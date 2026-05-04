package id.icapps.savera.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import id.icapps.savera.test.TestBase;

public class PendingUploadQueueTest extends TestBase {
    private SharedPreferences prefs;

    @Before
    public void clearQueueStorage() {
        prefs = getContext().getSharedPreferences("STORAGE_PENDING_UPLOAD_QUEUE", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
    }

    @Test
    public void repeatedUploadTapForSamePayloadOnlyQueuesOneItem() throws Exception {
        JSONObject first = PendingUploadQueue.createItem(
                "summary",
                "https://example.test/api/summary",
                "{\"steps\":1200}",
                "summary|emp-1|171000|171100"
        );
        JSONObject second = PendingUploadQueue.createItem(
                "summary",
                "https://example.test/api/summary",
                "{\"steps\":1200}",
                "summary|emp-1|171000|171100"
        );

        assertTrue(PendingUploadQueue.enqueue(getContext(), first));
        assertFalse(PendingUploadQueue.enqueue(getContext(), second));

        JSONArray queue = PendingUploadQueue.snapshot(getContext());
        assertEquals(1, queue.length());
        assertEquals("pending", queue.getJSONObject(0).getString("status"));
    }

    @Test
    public void queueStaysBoundedWhenUserKeepsTriggeringDifferentUploads() throws Exception {
        for (int i = 0; i < 130; i++) {
            assertTrue(PendingUploadQueue.enqueue(
                    getContext(),
                    PendingUploadQueue.createItem(
                            "detail",
                            "https://example.test/api/detail",
                            "{\"index\":" + i + "}",
                            "detail|emp-1|" + i
                    )
            ));
        }

        JSONArray queue = PendingUploadQueue.snapshot(getContext());
        assertEquals(120, queue.length());
    }

    @Test
    public void staleSendingItemReturnsToPendingOnNextSnapshot() throws Exception {
        long staleTime = System.currentTimeMillis() - (6L * 60L * 1000L);
        JSONArray rawQueue = new JSONArray();
        rawQueue.put(new JSONObject()
                .put("kind", "summary")
                .put("url", "https://example.test/api/summary")
                .put("payload", "{\"steps\":1200}")
                .put("created_at", staleTime)
                .put("updated_at", staleTime)
                .put("status", PendingUploadQueue.STATUS_SENDING)
                .put("attempts", 1)
                .put("fingerprint", "stale-item"));

        prefs.edit().putString("QUEUE", rawQueue.toString()).commit();

        JSONArray queue = PendingUploadQueue.snapshot(getContext());
        assertEquals(1, queue.length());
        assertEquals(PendingUploadQueue.STATUS_PENDING, queue.getJSONObject(0).getString("status"));
    }
}

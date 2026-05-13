package id.icapps.savera.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import id.icapps.savera.util.PendingUploadRetryManager;

public class PendingUploadRetryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? "" : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            PendingUploadRetryManager.scheduleRetryIfNeeded(context);
            return;
        }

        final PendingResult pendingResult = goAsync();
        final Context appContext = context == null ? null : context.getApplicationContext();
        new Thread(() -> {
            try {
                PendingUploadRetryManager.flush(appContext);
            } finally {
                pendingResult.finish();
            }
        }, "savera-pending-upload-retry").start();
    }
}

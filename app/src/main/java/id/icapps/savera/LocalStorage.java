package id.icapps.savera;

import android.content.Context;
import android.content.SharedPreferences;

public class LocalStorage {
    private static final String KEY_TOKEN = "TOKEN";
    private static final String KEY_DEVICE = "DEVICE";
    private static final String KEY_VERSION = "VERSION";
    private static final String KEY_EMPLOYEE = "EMPLOYEE";
    private static final String KEY_SHIFT = "SHIFT";
    private static final String KEY_USER = "USER";
    private static final String KEY_P5M = "P5M";
    private static final String KEY_P5M_ANSWERS = "P5M_ANSWERS";
    private static final String KEY_ADMIN = "ADMIN";
    private static final String KEY_SLEEP_UPLOADER = "SLEEP_UPLOADER";
    private static final String KEY_API_PUBLIC_URL = "API_PUBLIC_URL";
    private static final String KEY_API_LOCAL_URL = "API_LOCAL_URL";
    private static final String KEY_API_PREFERRED_ROUTE = "API_PREFERRED_ROUTE";
    private static final String KEY_API_ACTIVE_BASE_URL = "API_ACTIVE_BASE_URL";
    private static final String KEY_API_CONFIG_SYNCED_AT = "API_CONFIG_SYNCED_AT";
    private static final String KEY_USER_CONTEXT_SYNCED_AT = "USER_CONTEXT_SYNCED_AT";
    private static final String KEY_COMPANY_CODE = "COMPANY_CODE";
    private static final String KEY_LOCAL_PROFILE_PHOTO_PATH = "LOCAL_PROFILE_PHOTO_PATH";
    private static final String KEY_PASSWORD = "PASSWORD";
    private static final String KEY_REMEMBER_ME = "REMEMBER_ME";
    private static final String KEY_SLEEP_MINUTES = "SLEEP_MINUTES";
    private static final String KEY_FIT_1 = "FIT_1";
    private static final String KEY_FIT_2 = "FIT_2";
    private static final String KEY_FIT_3 = "FIT_3";
    private static final String KEY_FIT_4 = "FIT_4";
    private static final String KEY_FIT_5 = "FIT_5";
    private static final String KEY_LAST_SLEEP_SNAPSHOT = "LAST_SLEEP_SNAPSHOT";
    private static final String KEY_NOTIFICATION_CACHE = "NOTIFICATION_CACHE";
    private static final String KEY_NOTIFICATION_CACHE_SYNCED_AT = "NOTIFICATION_CACHE_SYNCED_AT";

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    Context context;
    String token;
    String device;
    String version;
    String employee;
    String shift;
    String user;
    String password;
    String p5m;
    boolean admin;

    public LocalStorage(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences("STORAGE_LOGIN_API", Context.MODE_PRIVATE);
        this.editor = sharedPreferences.edit();
    }

    public String getToken() {
        this.token = sharedPreferences.getString(KEY_TOKEN, "");
        return this.token;
    }

    public void setToken(String token) {
        editor.putString(KEY_TOKEN, token);
        editor.commit();
        this.token = token;
    }

    public String getDevice() {
        this.device = sharedPreferences.getString(KEY_DEVICE, "");
        return this.device;
    }

    public void setDevice(String device) {
        editor.putString(KEY_DEVICE, device);
        editor.commit();
        this.device = device;
    }

    public String getVersion() {
        this.version = sharedPreferences.getString(KEY_VERSION, "");
        return this.version;
    }

    public void setVersion(String version) {
        editor.putString(KEY_VERSION, version);
        editor.commit();
        this.version = version;
    }

    public String getEmployee() {
        this.employee = sharedPreferences.getString(KEY_EMPLOYEE, "");
        return this.employee;
    }

    public void setEmployee(String employee) {
        editor.putString(KEY_EMPLOYEE, employee);
        editor.commit();
        this.employee = employee;
    }

    public String getShift() {
        this.shift = sharedPreferences.getString(KEY_SHIFT, "");
        return this.shift;
    }

    public void setShift(String shift) {
        editor.putString(KEY_SHIFT, shift);
        editor.commit();
        this.shift = shift;
    }

    public String getUser() {
        this.user = sharedPreferences.getString(KEY_USER, "");
        return this.user;
    }

    public void setUser(String user) {
        editor.putString(KEY_USER, user);
        editor.commit();
        this.user = user;
    }

    public String getPassword() {
        this.password = sharedPreferences.getString(KEY_PASSWORD, "");
        return this.password == null ? "" : this.password;
    }

    public void setPassword(String password) {
        editor.putString(KEY_PASSWORD, password == null ? "" : password);
        editor.commit();
        this.password = password == null ? "" : password;
    }

    public boolean getRememberMe() {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
    }

    public void setRememberMe(boolean rememberMe) {
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe);
        editor.commit();
    }

    public void clearRememberedCredentials() {
        SharedPreferences.Editor clearEditor = sharedPreferences.edit();
        clearEditor.putBoolean(KEY_REMEMBER_ME, false);
        clearEditor.putString(KEY_PASSWORD, "");
        clearEditor.commit();
    }

    public String getP5M() {
        this.p5m = sharedPreferences.getString(KEY_P5M, "");
        return this.p5m;
    }

    public void setP5M(String p5m) {
        editor.putString(KEY_P5M, p5m);
        editor.commit();
        this.p5m = p5m;
    }

    public String getP5MAnswers() {
        return sharedPreferences.getString(KEY_P5M_ANSWERS, "");
    }

    public void setP5MAnswers(String p5mAnswers) {
        editor.putString(KEY_P5M_ANSWERS, p5mAnswers == null ? "" : p5mAnswers);
        editor.commit();
    }

    public long getSleepMinutes() {
        return sharedPreferences.getLong(KEY_SLEEP_MINUTES, -1L);
    }

    public void setSleepMinutes(long minutes) {
        editor.putLong(KEY_SLEEP_MINUTES, minutes);
        editor.commit();
    }

    public int getFit1() {
        return sharedPreferences.getInt(KEY_FIT_1, -1);
    }

    public void setFit1(int value) {
        editor.putInt(KEY_FIT_1, value);
        editor.commit();
    }

    public int getFit2() {
        return sharedPreferences.getInt(KEY_FIT_2, -1);
    }

    public void setFit2(int value) {
        editor.putInt(KEY_FIT_2, value);
        editor.commit();
    }

    public int getFit3() {
        return sharedPreferences.getInt(KEY_FIT_3, -1);
    }

    public void setFit3(int value) {
        editor.putInt(KEY_FIT_3, value);
        editor.commit();
    }

    public int getFit4() {
        return sharedPreferences.getInt(KEY_FIT_4, -1);
    }

    public void setFit4(int value) {
        editor.putInt(KEY_FIT_4, value);
        editor.commit();
    }

    public int getFit5() {
        return sharedPreferences.getInt(KEY_FIT_5, -1);
    }

    public void setFit5(int value) {
        editor.putInt(KEY_FIT_5, value);
        editor.commit();
    }

    public String getLastSleepSnapshotKey() {
        String value = sharedPreferences.getString(KEY_LAST_SLEEP_SNAPSHOT, "");
        return value == null ? "" : value;
    }

    public void setLastSleepSnapshotKey(String value) {
        editor.putString(KEY_LAST_SLEEP_SNAPSHOT, value == null ? "" : value);
        editor.commit();
    }

    public String getNotificationCache() {
        String value = sharedPreferences.getString(KEY_NOTIFICATION_CACHE, "");
        return value == null ? "" : value;
    }

    public long getNotificationCacheSyncedAt() {
        return sharedPreferences.getLong(KEY_NOTIFICATION_CACHE_SYNCED_AT, 0L);
    }

    public void setNotificationCache(String responseJson) {
        if (responseJson == null || responseJson.trim().isEmpty()) {
            return;
        }

        editor.putString(KEY_NOTIFICATION_CACHE, responseJson);
        editor.putLong(KEY_NOTIFICATION_CACHE_SYNCED_AT, System.currentTimeMillis());
        editor.commit();
    }

    public boolean hasFreshNotificationCache(long maxAgeMs) {
        if (maxAgeMs <= 0L) {
            return false;
        }

        long syncedAt = getNotificationCacheSyncedAt();
        return syncedAt > 0L && (System.currentTimeMillis() - syncedAt) <= maxAgeMs;
    }

    public boolean getAdmin() {
        this.admin = sharedPreferences.getBoolean(KEY_ADMIN, false);
        return this.admin;
    }

    public void setAdmin(boolean admin) {
        editor.putBoolean(KEY_ADMIN, admin);
        editor.commit();
        this.admin = admin;
    }

    public boolean getSleepUploader() {
        return sharedPreferences.getBoolean(KEY_SLEEP_UPLOADER, false);
    }

    public void setSleepUploader(boolean enabled) {
        editor.putBoolean(KEY_SLEEP_UPLOADER, enabled);
        editor.commit();
    }

    public String getApiPublicUrl() {
        return sanitizeBaseUrl(sharedPreferences.getString(KEY_API_PUBLIC_URL, context.getString(R.string.base_url_public)));
    }

    public void setApiPublicUrl(String url) {
        editor.putString(KEY_API_PUBLIC_URL, sanitizeBaseUrl(url));
        editor.commit();
    }

    public String getApiLocalUrl() {
        return sanitizeBaseUrl(sharedPreferences.getString(KEY_API_LOCAL_URL, context.getString(R.string.base_url_local)));
    }

    public void setApiLocalUrl(String url) {
        editor.putString(KEY_API_LOCAL_URL, sanitizeBaseUrl(url));
        editor.commit();
    }

    public String getApiPreferredRoute() {
        String preferredRoute = sharedPreferences.getString(KEY_API_PREFERRED_ROUTE, context.getString(R.string.base_url_preferred_route));
        return normalizePreferredRoute(preferredRoute);
    }

    public void setApiPreferredRoute(String route) {
        editor.putString(KEY_API_PREFERRED_ROUTE, normalizePreferredRoute(route));
        editor.commit();
    }

    public String getApiActiveBaseUrl() {
        return sanitizeBaseUrl(sharedPreferences.getString(KEY_API_ACTIVE_BASE_URL, ""));
    }

    public void setApiActiveBaseUrl(String url) {
        editor.putString(KEY_API_ACTIVE_BASE_URL, sanitizeBaseUrl(url));
        editor.commit();
    }

    public boolean hasSyncedApiConfig() {
        return sharedPreferences.getLong(KEY_API_CONFIG_SYNCED_AT, 0L) > 0L;
    }

    public void syncApiConfig(String publicUrl, String localUrl, String preferredRoute, String activeBaseUrl) {
        SharedPreferences.Editor syncEditor = sharedPreferences.edit();
        boolean changed = false;

        String sanitizedPublicUrl = sanitizeBaseUrl(publicUrl);
        if (!sanitizedPublicUrl.isEmpty()) {
            syncEditor.putString(KEY_API_PUBLIC_URL, sanitizedPublicUrl);
            changed = true;
        }

        String sanitizedLocalUrl = sanitizeBaseUrl(localUrl);
        if (!sanitizedLocalUrl.isEmpty()) {
            syncEditor.putString(KEY_API_LOCAL_URL, sanitizedLocalUrl);
            changed = true;
        }

        String normalizedPreferredRoute = normalizePreferredRoute(preferredRoute);
        if (!normalizedPreferredRoute.isEmpty()) {
            syncEditor.putString(KEY_API_PREFERRED_ROUTE, normalizedPreferredRoute);
            changed = true;
        }

        String sanitizedActiveBaseUrl = sanitizeBaseUrl(activeBaseUrl);
        if (!sanitizedActiveBaseUrl.isEmpty()) {
            syncEditor.putString(KEY_API_ACTIVE_BASE_URL, sanitizedActiveBaseUrl);
            changed = true;
        }

        if (changed) {
            syncEditor.putLong(KEY_API_CONFIG_SYNCED_AT, System.currentTimeMillis());
            syncEditor.commit();
        }
    }

    public long getUserContextSyncedAt() {
        return sharedPreferences.getLong(KEY_USER_CONTEXT_SYNCED_AT, 0L);
    }

    public void markUserContextSynced() {
        editor.putLong(KEY_USER_CONTEXT_SYNCED_AT, System.currentTimeMillis());
        editor.commit();
    }

    public boolean hasFreshUserContext(long maxAgeMs) {
        if (maxAgeMs <= 0L) {
            return false;
        }

        long syncedAt = getUserContextSyncedAt();
        return syncedAt > 0L && (System.currentTimeMillis() - syncedAt) <= maxAgeMs;
    }

    public String getCompanyCode() {
        String storedCompany = getStoredCompanyCode();
        if (!storedCompany.isEmpty()) {
            return storedCompany;
        }
        return getConfiguredCompanyCode();
    }

    public String getStoredCompanyCode() {
        return sanitizeCompanyCode(sharedPreferences.getString(KEY_COMPANY_CODE, ""));
    }

    public String getConfiguredCompanyCode() {
        return sanitizeCompanyCode(context.getString(R.string.base_company));
    }

    public void setCompanyCode(String companyCode) {
        String sanitized = sanitizeCompanyCode(companyCode);
        if (sanitized.isEmpty()) {
            return;
        }
        editor.putString(KEY_COMPANY_CODE, sanitized);
        editor.commit();
    }

    public String getLocalProfilePhotoPath() {
        String path = sharedPreferences.getString(KEY_LOCAL_PROFILE_PHOTO_PATH, "");
        return path == null ? "" : path.trim();
    }

    public void setLocalProfilePhotoPath(String path) {
        String sanitized = path == null ? "" : path.trim();
        editor.putString(KEY_LOCAL_PROFILE_PHOTO_PATH, sanitized);
        editor.commit();
    }

    private String sanitizeBaseUrl(String url) {
        if (url == null) {
            return "";
        }

        return url.trim().replaceAll("/+$", "");
    }

    private String normalizePreferredRoute(String route) {
        if (route == null) {
            return "public";
        }

        return "local".equalsIgnoreCase(route.trim()) ? "local" : "public";
    }

    private String sanitizeCompanyCode(String companyCode) {
        if (companyCode == null) {
            return "";
        }
        return companyCode.trim().toUpperCase();
    }
}

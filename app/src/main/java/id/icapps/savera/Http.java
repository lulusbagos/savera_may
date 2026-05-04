package id.icapps.savera;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import id.icapps.savera.util.ApiEndpointResolver;
import id.icapps.savera.util.ApiUrl;

public class Http {
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_BASE_MS = 750L;
    private static final long SLOW_PUBLIC_RESPONSE_MS = 4000L;
    private static final String CACHE_PREFS = "STORAGE_HTTP_CACHE";

    Context context;
    private String url, method = "GET", data = null, response = null;
    private Integer statusCode = 0;
    private Boolean token = false;
    private boolean bypassCache = false;
    private int cacheTtlSeconds = 0;
    private boolean useEndpointResolver = true;
    private boolean includeCompanyHeader = true;
    private String companyHeaderValue = null;
    private int connectTimeoutMs = CONNECT_TIMEOUT_MS;
    private int readTimeoutMs = READ_TIMEOUT_MS;
    private int maxAttempts = MAX_ATTEMPTS;
    private final Map<String, String> extraHeaders = new LinkedHashMap<>();
    private LocalStorage localStorage;

    public Http(Context context, String url) {
        this.context = context;
        this.url = url;
        this.localStorage = new LocalStorage(context);
    }

    public void setMethod(String method) {
        this.method = method.toUpperCase();
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setToken(Boolean token) {
        this.token = token;
    }

    public void setBypassCache(boolean bypassCache) {
        this.bypassCache = bypassCache;
    }

    public void setCacheTtlSeconds(int cacheTtlSeconds) {
        this.cacheTtlSeconds = Math.max(cacheTtlSeconds, 0);
    }

    public void setUseEndpointResolver(boolean useEndpointResolver) {
        this.useEndpointResolver = useEndpointResolver;
    }

    public void setIncludeCompanyHeader(boolean includeCompanyHeader) {
        this.includeCompanyHeader = includeCompanyHeader;
    }

    public void setCompanyHeaderValue(String companyHeaderValue) {
        this.companyHeaderValue = companyHeaderValue;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = Math.max(connectTimeoutMs, 1000);
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = Math.max(readTimeoutMs, 1000);
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = Math.max(maxAttempts, 1);
    }

    public void setHeader(String name, String value) {
        if (name == null) {
            return;
        }

        String normalizedName = name.trim();
        if (normalizedName.isEmpty()) {
            return;
        }

        if (value == null || value.trim().isEmpty()) {
            this.extraHeaders.remove(normalizedName);
            return;
        }

        this.extraHeaders.put(normalizedName, value.trim());
    }

    public String getResponse() {
        return this.response;
    }

    public Integer getStatusCode() {
        return this.statusCode;
    }

    public void send() {
        this.response = null;
        this.statusCode = 0;

        IOException lastException = null;
        RuntimeException lastRuntimeException = null;
        if (loadCachedResponse()) {
            return;
        }

        try {
            List<URL> candidateUrls;
            if (this.useEndpointResolver) {
                candidateUrls = ApiEndpointResolver.candidateUrls(this.context, this.url);
            } else {
                // Keep protocol fallback (https <-> http) without switching host/routes.
                candidateUrls = ApiUrl.candidateUrls(this.url);
            }

            for (URL candidateUrl : candidateUrls) {
                List<String> companyCandidates = resolveCompanyHeaderCandidates();
                for (String companyCandidate : companyCandidates) {
                    for (int attempt = 1; attempt <= this.maxAttempts; attempt++) {
                    HttpURLConnection conn = null;
                    try {
                        long startedAt = System.currentTimeMillis();
                        conn = (HttpURLConnection) candidateUrl.openConnection();
                        conn.setRequestMethod(this.method);
                        conn.setConnectTimeout(this.connectTimeoutMs);
                        conn.setReadTimeout(this.readTimeoutMs);
                        conn.setUseCaches(false);
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setRequestProperty("Accept", "application/json");
                        conn.setRequestProperty("Connection", "Keep-Alive");
                        for (Map.Entry<String, String> extraHeader : this.extraHeaders.entrySet()) {
                            conn.setRequestProperty(extraHeader.getKey(), extraHeader.getValue());
                        }
                        if (this.includeCompanyHeader) {
                            if (companyCandidate != null && !companyCandidate.isEmpty()) {
                                conn.setRequestProperty("company", companyCandidate);
                            }
                        }

                        if (this.token) {
                            conn.setRequestProperty("Authorization", "Bearer " + this.localStorage.getToken());
                        }

                        if (!this.method.equals("GET")) {
                            conn.setDoOutput(true);
                        }

                        if (this.data != null) {
                            OutputStream os = conn.getOutputStream();
                            os.write(this.data.getBytes(StandardCharsets.UTF_8));
                            os.flush();
                            os.close();
                        }

                        this.statusCode = conn.getResponseCode();
                        long durationMs = System.currentTimeMillis() - startedAt;
                        syncApiConfig(conn, candidateUrl, durationMs);

                        InputStreamReader isr;
                        if (this.statusCode >= 200 && this.statusCode <= 299) {
                            isr = new InputStreamReader(conn.getInputStream());
                        } else if (conn.getErrorStream() != null) {
                            isr = new InputStreamReader(conn.getErrorStream());
                        } else {
                            isr = null;
                        }

                        if (isr != null) {
                            BufferedReader br = new BufferedReader(isr);
                            StringBuffer sb = new StringBuffer();
                            String line;
                            while ((line = br.readLine()) != null) {
                                sb.append(line);
                            }
                            br.close();
                            this.response = sb.toString();
                        } else {
                            this.response = null;
                        }

                        if (BuildConfig.DEBUG) {
                            Log.d("HttpURL", "Url: " + candidateUrl);
                            Log.d("HttpURL", "status: " + this.statusCode);
                            Log.d("HttpURL", "company: " + (companyCandidate == null ? "<none>" : companyCandidate));
                            for (Map.Entry<String, List<String>> header : conn.getHeaderFields().entrySet()) {
                                Log.d("HttpURL", header.getKey() + ": " + header.getValue());
                            }
                            if (this.data != null) Log.d("HttpURL", "request: " + this.data);
                            if (this.response != null) Log.d("HttpURL", "response: " + this.response);
                        }

                        if (this.statusCode != null && this.statusCode >= 200 && this.statusCode <= 299 && companyCandidate != null && !companyCandidate.isEmpty()) {
                            this.localStorage.setCompanyCode(companyCandidate);
                        }

                        if (containsCompanyNotFound() && hasNextCompanyCandidate(companyCandidates, companyCandidate)) {
                            break;
                        }

                        if (!shouldRetry(this.statusCode) || attempt == this.maxAttempts) {
                            storeCachedResponse();
                            return;
                        }
                    } catch (IOException e) {
                        lastException = e;
                        if (attempt == this.maxAttempts) {
                            break;
                        }
                    } catch (RuntimeException e) {
                        lastRuntimeException = e;
                        if (BuildConfig.DEBUG) {
                            Log.w("HttpURL", "Runtime error for URL " + candidateUrl + ": " + e.getMessage());
                        }
                        // Runtime URL/TLS errors are not recoverable by retrying same candidate.
                        break;
                    } finally {
                        if (conn != null) {
                            conn.disconnect();
                        }
                    }

                    sleepBeforeRetry(attempt);
                }

                    if (containsCompanyNotFound() && hasNextCompanyCandidate(companyCandidates, companyCandidate)) {
                        continue;
                    }

                    if (this.statusCode != null && this.statusCode >= 200 && this.statusCode <= 299) {
                        return;
                    }

                    if (containsCompanyNotFound() && !hasNextCompanyCandidate(companyCandidates, companyCandidate)) {
                        storeCachedResponse();
                        return;
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }

        if (lastException != null) {
            lastException.printStackTrace();
        }
        if (lastRuntimeException != null) {
            lastRuntimeException.printStackTrace();
        }
    }

    private boolean loadCachedResponse() {
        if (!shouldCache()) {
            return false;
        }

        SharedPreferences prefs = context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String cacheKey = cacheKey();
        long cachedAt = prefs.getLong(cacheKey + ":ts", 0L);
        long cacheAgeMs = System.currentTimeMillis() - cachedAt;
        long cacheTtlMs = cacheTtlSeconds * 1000L;

        if (cachedAt <= 0L || cacheAgeMs > cacheTtlMs) {
            return false;
        }

        this.response = prefs.getString(cacheKey + ":body", null);
        if (this.response == null) {
            return false;
        }

        this.statusCode = 200;
        if (BuildConfig.DEBUG) {
            Log.d("HttpURL", "Cache hit: " + this.url);
        }
        return true;
    }

    private void storeCachedResponse() {
        if (!shouldCache() || this.statusCode == null || this.statusCode != 200 || this.response == null) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(cacheKey() + ":body", this.response)
                .putLong(cacheKey() + ":ts", System.currentTimeMillis())
                .apply();
    }

    private boolean shouldCache() {
        return !bypassCache && "GET".equals(this.method) && this.cacheTtlSeconds > 0;
    }

    private String cacheKey() {
        String tokenPart = this.token ? "token:" + hashOrEmpty(this.localStorage.getToken()) : "public";
        return this.method + "|" + this.url + "|" + tokenPart + "|company:" + this.localStorage.getCompanyCode();
    }

    private String resolveCompanyHeader() {
        if (this.companyHeaderValue != null && !this.companyHeaderValue.trim().isEmpty()) {
            return this.companyHeaderValue.trim().toUpperCase();
        }

        String storedCompany = this.localStorage.getStoredCompanyCode();
        if (!storedCompany.isEmpty()) {
            return storedCompany;
        }

        return this.localStorage.getConfiguredCompanyCode();
    }

    private List<String> resolveCompanyHeaderCandidates() {
        List<String> candidates = new ArrayList<>();
        if (!this.includeCompanyHeader) {
            candidates.add("");
            return candidates;
        }

        if (this.companyHeaderValue != null && !this.companyHeaderValue.trim().isEmpty()) {
            candidates.add(this.companyHeaderValue.trim().toUpperCase(Locale.ROOT));
            return candidates;
        }

        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        ordered.add(this.localStorage.getStoredCompanyCode());
        ordered.add(this.localStorage.getConfiguredCompanyCode());
        ordered.add("INDEXIM");
        ordered.add("SAVERA");
        ordered.add("UDU");
        ordered.add("");

        for (String value : ordered) {
            if (value == null) {
                continue;
            }
            candidates.add(value.trim().toUpperCase(Locale.ROOT));
        }

        if (candidates.isEmpty()) {
            candidates.add("");
        }

        return candidates;
    }

    private boolean containsCompanyNotFound() {
        return this.statusCode != null
                && this.statusCode == 404
                && this.response != null
                && this.response.toLowerCase(Locale.ROOT).contains("company not found");
    }

    private boolean hasNextCompanyCandidate(List<String> candidates, String current) {
        int index = candidates.indexOf(current);
        return index >= 0 && index + 1 < candidates.size();
    }

    private String hashOrEmpty(String value) {
        if (value == null || value.isEmpty()) {
            return "empty";
        }

        return Integer.toHexString(value.hashCode());
    }

    private boolean shouldRetry(int statusCode) {
        return statusCode == 408 || statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    private void syncApiConfig(HttpURLConnection conn, URL candidateUrl, long durationMs) {
        if (conn == null || candidateUrl == null) {
            return;
        }

        String publicBaseUrl = headerValue(conn, "X-Savera-Public-Base-Url");
        String localBaseUrl = headerValue(conn, "X-Savera-Local-Base-Url");
        String preferredRoute = headerValue(conn, "X-Savera-Preferred-Route");
        String activeBaseUrl = ApiEndpointResolver.apiBaseUrl(candidateUrl);

        this.localStorage.syncApiConfig(publicBaseUrl, localBaseUrl, preferredRoute, activeBaseUrl);

        boolean requestSucceeded = this.statusCode != null && this.statusCode >= 200 && this.statusCode <= 299;
        boolean usedPublicRoute = !publicBaseUrl.isEmpty() && publicBaseUrl.equalsIgnoreCase(activeBaseUrl);
        boolean hasLocalRoute = !localBaseUrl.isEmpty();
        if (requestSucceeded && usedPublicRoute && hasLocalRoute && durationMs >= SLOW_PUBLIC_RESPONSE_MS) {
            this.localStorage.setApiActiveBaseUrl(localBaseUrl);
        }
    }

    private String headerValue(HttpURLConnection conn, String name) {
        String value = conn.getHeaderField(name);
        return value == null ? "" : value.trim();
    }

    private void sleepBeforeRetry(int attempt) {
        long delay = RETRY_BACKOFF_BASE_MS * attempt;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

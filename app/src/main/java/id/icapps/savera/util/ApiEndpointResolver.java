package id.icapps.savera.util;

import android.content.Context;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import id.icapps.savera.LocalStorage;

public final class ApiEndpointResolver {
    private ApiEndpointResolver() {
    }

    public static List<URL> candidateUrls(Context context, String rawUrl) throws MalformedURLException {
        URL originalUrl = new URL(rawUrl);
        LocalStorage localStorage = new LocalStorage(context);

        String publicBaseUrl = sanitizeBaseUrl(localStorage.getApiPublicUrl());
        String localBaseUrl = sanitizeBaseUrl(localStorage.getApiLocalUrl());
        String activeBaseUrl = sanitizeBaseUrl(localStorage.getApiActiveBaseUrl());
        String rawBaseUrl = sanitizeBaseUrl(apiBaseUrl(originalUrl));
        boolean hasSyncedConfig = localStorage.hasSyncedApiConfig();
        String preferredRoute = localStorage.getApiPreferredRoute();

        LinkedHashSet<String> orderedBaseUrls = new LinkedHashSet<>();

        if (hasSyncedConfig && "local".equals(preferredRoute) && !localBaseUrl.isEmpty()) {
            orderedBaseUrls.add(localBaseUrl);
        }

        if (!activeBaseUrl.isEmpty()) {
            orderedBaseUrls.add(activeBaseUrl);
        }

        if (!publicBaseUrl.isEmpty()) {
            orderedBaseUrls.add(publicBaseUrl);
        }

        if (!localBaseUrl.isEmpty()) {
            orderedBaseUrls.add(localBaseUrl);
        }

        if (!rawBaseUrl.isEmpty()) {
            orderedBaseUrls.add(rawBaseUrl);
        }

        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        for (String baseUrl : orderedBaseUrls) {
            if (baseUrl.isEmpty()) {
                continue;
            }

            String candidateUrl = rebuildUrl(baseUrl, originalUrl);
            for (URL url : ApiUrl.candidateUrls(candidateUrl)) {
                candidates.add(url.toString());
            }
        }

        List<URL> resolved = new ArrayList<>(candidates.size());
        for (String candidate : candidates) {
            resolved.add(new URL(candidate));
        }

        return resolved;
    }

    public static String apiBaseUrl(URL url) {
        if (url == null) {
            return "";
        }

        String path = url.getPath();
        int apiIndex = path.indexOf("/api");
        String apiPath = apiIndex >= 0 ? path.substring(0, apiIndex + 4) : "";

        return origin(url) + apiPath;
    }

    private static String rebuildUrl(String baseUrl, URL originalUrl) {
        StringBuilder rebuilt = new StringBuilder(baseUrl);
        rebuilt.append(relativeApiPath(originalUrl));

        String query = originalUrl.getQuery();
        if (query != null && !query.isEmpty()) {
            rebuilt.append('?').append(query);
        }

        return rebuilt.toString();
    }

    private static String relativeApiPath(URL originalUrl) {
        String path = originalUrl.getPath();
        int apiIndex = path.indexOf("/api");

        if (apiIndex < 0) {
            return path;
        }

        String relativePath = path.substring(apiIndex + 4);
        return relativePath.isEmpty() ? "" : relativePath;
    }

    private static String origin(URL url) {
        StringBuilder origin = new StringBuilder();
        origin.append(url.getProtocol()).append("://").append(url.getHost());
        if (url.getPort() > 0) {
            origin.append(':').append(url.getPort());
        }
        return origin.toString();
    }

    private static String sanitizeBaseUrl(String url) {
        if (url == null) {
            return "";
        }

        return url.trim().replaceAll("/+$", "");
    }
}

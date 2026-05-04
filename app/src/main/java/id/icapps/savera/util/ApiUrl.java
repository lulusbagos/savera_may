package id.icapps.savera.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class ApiUrl {
    private ApiUrl() {
    }

    public static List<URL> candidateUrls(String rawUrl) throws MalformedURLException {
        List<URL> urls = new ArrayList<>(2);
        URL primary = new URL(rawUrl);
        urls.add(primary);

        if (isPrivateHost(primary.getHost())) {
            return urls;
        }

        URL fallback = buildFallback(primary);
        if (fallback != null && !fallback.toString().equals(primary.toString())) {
            urls.add(fallback);
        }

        return urls;
    }

    private static URL buildFallback(URL primary) throws MalformedURLException {
        String protocol = primary.getProtocol();
        if ("https".equalsIgnoreCase(protocol)) {
            return buildUrl("http", primary);
        }

        if ("http".equalsIgnoreCase(protocol)) {
            return buildUrl("https", primary);
        }

        return null;
    }

    private static URL buildUrl(String protocol, URL primary) throws MalformedURLException {
        int port = primary.getPort();
        String file = primary.getFile();
        if (port > 0) {
            return new URL(protocol, primary.getHost(), port, file);
        }

        return new URL(protocol, primary.getHost(), file);
    }

    private static boolean isPrivateHost(String host) {
        return host == null
                || host.equalsIgnoreCase("localhost")
                || host.startsWith("10.")
                || host.startsWith("192.168.")
                || host.startsWith("127.")
                || host.startsWith("172.16.")
                || host.startsWith("172.17.")
                || host.startsWith("172.18.")
                || host.startsWith("172.19.")
                || host.startsWith("172.20.")
                || host.startsWith("172.21.")
                || host.startsWith("172.22.")
                || host.startsWith("172.23.")
                || host.startsWith("172.24.")
                || host.startsWith("172.25.")
                || host.startsWith("172.26.")
                || host.startsWith("172.27.")
                || host.startsWith("172.28.")
                || host.startsWith("172.29.")
                || host.startsWith("172.30.")
                || host.startsWith("172.31.");
    }
}

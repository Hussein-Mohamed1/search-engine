package cu.searchengine.Crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

public class URLNormalizer {
    private static final Logger logger = LoggerFactory.getLogger(URLNormalizer.class);

    public String normalize(String rawURL) {
        try {
            if (rawURL == null || rawURL.isEmpty()) return "";

            // Lowercase URL
            String lowerCaseURL = rawURL.toLowerCase();
            URI uri = new URI(lowerCaseURL);

            // Normalize scheme (http/https -> https)
            String scheme = uri.getScheme();
            if (scheme == null) scheme = "https";
            if (scheme.equals("http") || scheme.equals("https")) scheme = "https";

            // Remove "www." prefix
            String host = uri.getHost();
            if (host != null && host.startsWith("www.")) {
                host = host.substring(4);
            }

            // Remove default ports
            int port = uri.getPort();
            boolean isDefaultPort = (port == 80 || port == 443 || port == -1);

            // Normalize path: remove trailing slash (unless it's just "/")
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            } else if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            // Remove fragments
            // (already handled by URI, as getPath() does not include fragment)

            // Normalize query parameters: sort and filter empty
            String normalizedQuery = null;
            String query = uri.getQuery();
            if (query != null && !query.isEmpty()) {
                String[] params = query.split("&");
                Arrays.sort(params);
                StringBuilder queryBuilder = new StringBuilder();
                for (String param : params) {
                    if (!param.isEmpty() && !param.equals("=")) {
                        if (queryBuilder.length() > 0) queryBuilder.append("&");
                        queryBuilder.append(param);
                    }
                }
                normalizedQuery = queryBuilder.length() > 0 ? queryBuilder.toString() : null;
            }

            // Rebuild normalized URL
            StringBuilder result = new StringBuilder();
            result.append(scheme).append("://").append(host);
            if (port != -1 && !isDefaultPort) {
                result.append(":").append(port);
            }
            result.append(path);
            if (normalizedQuery != null) {
                result.append("?").append(normalizedQuery);
            }
            return result.toString();

        } catch (URISyntaxException e) {
            logger.debug("Invalid URL: {}", rawURL);
            return "";
        }
    }

    public static void main(String[] args) {
        String rawURL = "http://example.com:8080/path/to/resource";
        String raw = "http://example.com:8080/path/to/resource#section1/file";

        try {
            URLNormalizer normalizer = new URLNormalizer();
            String normalizedURL = normalizer.normalize(raw);
            logger.info(normalizedURL);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

}
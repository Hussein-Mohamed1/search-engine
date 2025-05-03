package cu.searchengine.Crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class URLNormalizer {
    private static final Logger logger = LoggerFactory.getLogger(URLNormalizer.class);

    // Set of protocols we want to accept
    private static final Set<String> VALID_PROTOCOLS = new HashSet<>(Arrays.asList("http", "https"));

    // Pattern to detect non-ASCII characters in URLs
    private static final Pattern NON_ASCII_PATTERN = Pattern.compile("[^\\x00-\\x7F]");

    // Known problematic URL patterns
    private static final Set<String> PROBLEMATIC_PATTERNS = new HashSet<>(Arrays.asList("mailto:", "javascript:", "tel:", "ftp:", "file:", "#", "data:", "about:"));

    public String normalize(String rawURL) {
        if (rawURL == null || rawURL.isEmpty()) return "";

        // Quick check for problematic URL patterns
        for (String pattern : PROBLEMATIC_PATTERNS) {
            if (rawURL.contains(pattern)) {
                logger.debug("Skipping URL with problematic pattern: {}", rawURL);
                return "";
            }
        }

        // Check for non-ASCII characters
        if (NON_ASCII_PATTERN.matcher(rawURL).find()) {
            try {
                // Try to encode the URL properly
                URI uri = new URI(rawURL);
                rawURL = uri.toASCIIString();
            } catch (URISyntaxException e) {
                logger.debug("URL contains non-ASCII characters and cannot be parsed: {}", rawURL);
                return "";
            }
        }

        try {
            // First, try to parse as is
            String url = rawURL.trim();

            // If URL doesn't start with a protocol, add https://
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            // Lowercase URL
            String lowerCaseURL = url.toLowerCase();
            URI uri = new URI(lowerCaseURL);

            // Validate protocol
            String scheme = uri.getScheme();
            if (scheme == null || !VALID_PROTOCOLS.contains(scheme)) {
                logger.debug("Invalid protocol in URL: {}", rawURL);
                return "";
            }

            // Always use https
            scheme = "https";

            // Get host and validate
            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                logger.debug("Invalid host in URL: {}", rawURL);
                return "";
            }

            // Remove "www." prefix
            if (host.startsWith("www.")) {
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
            logger.debug("Invalid URL structure: {} - {}", rawURL, e.getMessage());
            return "";
        } catch (Exception e) {
            logger.debug("Error normalizing URL {}: {}", rawURL, e.getMessage());
            return "";
        }
    }

    public static void main(String[] args) {
        URLNormalizer normalizer = new URLNormalizer();

        // Test cases
        String[] testUrls = {"http://example.com:8080/path/to/resource", "http://example.com:8080/path/to/resource#section1/file", "https://pa.wikisource.org/wiki/ਮੁੱਖ_ਸਫ਼ਾ", "www.example.com", "example.com/path", "mailto:user@example.com", "javascript:alert('test')", "https://example.com/path with spaces", "https://example.com/path%20with%20encoded%20spaces"};

        for (String url : testUrls) {
            System.out.println("Original: " + url);
            System.out.println("Normalized: " + normalizer.normalize(url));
            System.out.println();
        }
    }
}
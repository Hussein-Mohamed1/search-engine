package Crawler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

public class URLNormalizer {
    public String normalize(String rawURL) {
        try {
            if (rawURL == null || rawURL.isEmpty()) return "";

            String normalizedURL="";

            // Lowercase URL
            String lowerCaseURL = rawURL.toLowerCase();
            URI uri = new URI(lowerCaseURL);

            // Remove the default port number
            String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
            String noPortURL = lowerCaseURL.replace(port, "");

            // Normalize the scheme
            String scheme = uri.getScheme();
            if(scheme.equals("http") || scheme.equals("https"))
             normalizedURL = noPortURL.replace(scheme, "https");

            // Remove fragments
            if(uri.getFragment() != null) {
                String fragment = uri.getFragment();
                fragment = "#" + fragment ;
                normalizedURL = noPortURL.replace(fragment, "");
            }
            // Normalize query parameters
            if (uri.getQuery() != null) {
                String[] params = uri.getQuery().split("&");
                Arrays.sort(params);
                String sortedQuery = String.join("&", params);

                return normalizedURL.replace(uri.getQuery(), sortedQuery);
            }
            return normalizedURL;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + rawURL);
        }
    }

    public static void main(String[] args) {
        String rawURL = "http://example.com:8080/path/to/resource";
        String raw = "http://example.com:8080/path/to/resource#section1/file";

        try {
            URLNormalizer normalizer = new URLNormalizer();
            String normalizedURL = normalizer.normalize(raw);
            System.out.println(normalizedURL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

package cu.searchengine.utils;
public class normailzeUrl {
    public static String normalizeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        // Remove fragment (everything after #)
        url = url.split("#")[0];

        // Remove trailing slash (if any)
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        // Convert to lowercase
        return url.toLowerCase();
    }

    //! only for testing
    public static void main(String[] args) {
        // Test cases
        String[] testUrls = {
                "https://Example.com/Page#section",
                "http://TEST.COM/",
                "https://www.example.com/about/",
                "HTTPS://Example.com#fragment",
                "http://example.com/#",
                "http://example.com//double-slash/",
                "https://EXAMPLE.COM:443/Home" // Should normalize without default port
        };

        for (String url : testUrls) {
            System.out.println("Original: " + url);
            System.out.println("Normalized: " + normalizeUrl(url));
            System.out.println("---------------");
        }
    }
}

package cu.searchengine.Crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RobotsTxtParser {
    private static final Logger logger = LoggerFactory.getLogger(RobotsTxtParser.class);

    // Each user agent with a list of its prohibited paths Domain ->(userAgent, disallowedPaths)
    private final HashMap<String, HashMap<String, List<String>>> disallowedPolicyMap;
    private final HashMap<String, HashMap<String, List<String>>> allowedPolicyMap;
    private final HashMap<String, Boolean> robotsTxtLoaded;
    private final HashMap<String, Integer> robotsTxtFailCount;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    public RobotsTxtParser() {
        disallowedPolicyMap = new HashMap<>();
        allowedPolicyMap = new HashMap<>();
        robotsTxtLoaded = new HashMap<>();
        robotsTxtFailCount = new HashMap<>();
    }

    // Checks if a path can be crawled
    public boolean isAllowed(String fullURL, String userAgent) {
        boolean isAllowedCrawling = true;

        try {
            URL url = new URL(fullURL);
            String domain = url.getHost();
            String path = url.getPath();
            if (path.isEmpty()) {
                path = "/";
            }
            String currentUserAgent = userAgent;

            // If we haven't tried to load robots.txt for this domain, do it now
            if (!robotsTxtLoaded.containsKey(domain)) {
                loadRobotsTxt(domain);
            }

            // If no rules exist for this domain, allow crawling
            if (disallowedPolicyMap.get(domain) == null) {
                return true;
            }

            List<String> disallowedCurrentAgent = disallowedPolicyMap.get(domain).get(userAgent);

            if (disallowedCurrentAgent == null) {
                disallowedCurrentAgent = disallowedPolicyMap.get(domain).get("*");   // get disallowed paths for all agents
                currentUserAgent = "*";
            }

            if (disallowedCurrentAgent == null) {
                return true; // no rules for this agent nor all agents, crawling allowed
            }

            // Check if the path is prohibited for all agents
            List<String> disallowedAllAgents = disallowedPolicyMap.get(domain).get("*");
            if (disallowedAllAgents != null) {
                for (String directive : disallowedAllAgents) {
                    // Proper path matching - check if path starts with directive or matches exactly
                    if (pathMatches(path, directive)) {
                        logger.debug("Blocked by all agents '{}' for path '{}'", directive, path);
                        isAllowedCrawling = false;
                        break;
                    }
                }
            }

            if (!currentUserAgent.equals("*")) {
                // Check if the path is prohibited for this certain user agent and wasn't blocked for all agents
                if (isAllowedCrawling) {
                    for (String directive : disallowedCurrentAgent) {
                        if (pathMatches(path, directive)) {
                            logger.debug("Blocked by robots.txt: '{}' for path '{}'", directive, path);
                            isAllowedCrawling = false;
                            break;
                        }
                    }
                } else {
                    // Check if the path is prohibited for all agents but accessible for this certain user agent
                    HashMap<String, List<String>> policy = allowedPolicyMap.get(domain);
                    if (policy == null) return isAllowedCrawling;

                    List<String> allowedCurrentAgent = policy.get(userAgent);
                    if (allowedCurrentAgent == null) return isAllowedCrawling;

                    for (String directive : allowedCurrentAgent) {
                        // More specific Allow directives take precedence over Disallow
                        if (pathMatches(path, directive)) {
                            isAllowedCrawling = true;
                            logger.debug("Allowed by specific rule: '{}' for path '{}'", directive, path);
                            break;
                        }
                    }
                }
            }
        } catch (MalformedURLException e) {
            logger.error("Malformed URL: {}", fullURL);
            return false;
        }
        return isAllowedCrawling;
    }

    private boolean pathMatches(String path, String directive) {
        if (directive.isEmpty()) {
            return false;
        }

        if (directive.equals("/") && path.equals("/")) {
            return true;
        }

        return path.startsWith(directive);
    }

    public boolean isLoaded(String url) {
        try {
            URI uri = URI.create(url);
            String domain = uri.getHost();
            return robotsTxtLoaded.getOrDefault(domain, false);
        } catch (Exception e) {
            logger.error("Error checking if robots.txt is loaded for URL: {}, error: {}", url, e.getMessage());
            return false;
        }
    }

    // Adds a rule to a certain domain of a certain agent
    public void addPolicy(String domain, String userAgent, String rule, String policy) {
        if (policy.equalsIgnoreCase("disallow")) {
            disallowedPolicyMap.putIfAbsent(domain, new HashMap<>());
            disallowedPolicyMap.get(domain).putIfAbsent(userAgent, new ArrayList<>());
            disallowedPolicyMap.get(domain).get(userAgent).add(rule);
            logger.debug("Added Disallow rule for domain: {}, agent: {}, path: {}", domain, userAgent, rule);
        }
        if (policy.equalsIgnoreCase("allow")) {
            allowedPolicyMap.putIfAbsent(domain, new HashMap<>());
            allowedPolicyMap.get(domain).putIfAbsent(userAgent, new ArrayList<>());
            allowedPolicyMap.get(domain).get(userAgent).add(rule);
            logger.debug("Added Allow rule for domain: {}, agent: {}, path: {}", domain, userAgent, rule);
        }
    }

    // Loads robots.txt for a given domain
    public void loadRobotsTxt(String domain) {
        // If we've already tried and failed multiple times, don't keep trying
        int failCount = robotsTxtFailCount.getOrDefault(domain, 0);
        if (failCount >= MAX_RETRY_ATTEMPTS) {
            logger.debug("Skipping robots.txt load for {} after {} failed attempts", domain, failCount);
            robotsTxtLoaded.put(domain, true); // Mark as "loaded" to prevent further attempts
            return;
        }

        try {
            // Extract domain if full URL is given
            if (domain.startsWith("http")) {
                domain = new URL(domain).getHost();
            }

            // Try HTTPS first
            boolean success = tryLoadRobotsTxt("https://" + domain + "/robots.txt", domain);

            // If HTTPS fails, try HTTP
            if (!success) {
                success = tryLoadRobotsTxt("http://" + domain + "/robots.txt", domain);
            }

            // If we couldn't load robots.txt, increment the fail count
            if (!success) {
                robotsTxtFailCount.put(domain, failCount + 1);
                // If robots.txt doesn't exist or can't be accessed, assume everything is allowed
                if (failCount + 1 >= MAX_RETRY_ATTEMPTS) {
                    robotsTxtLoaded.put(domain, true);
                    logger.debug("Assuming all crawling allowed for {} after {} failed attempts", domain, failCount + 1);
                }
            }
        } catch (Exception e) {
            robotsTxtFailCount.put(domain, failCount + 1);
            logger.debug("Error loading robots.txt file for {}: {}", domain, e.getMessage());
        }
    }

    private boolean tryLoadRobotsTxt(String urlString, String domain) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(urlString);

            if (urlString.startsWith("https://")) {
                connection = (HttpsURLConnection) url.openConnection();
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);  // 5 seconds timeout
            connection.setReadTimeout(5000);     // 5 seconds timeout
            connection.connect();

            int responseCode = connection.getResponseCode();

            // 2xx status codes indicate success
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                parseRobotsTxt(reader, domain);
                robotsTxtLoaded.put(domain, true);
                logger.debug("Successfully loaded robots.txt from {}", urlString);
                return true;
            } else if (responseCode == 404) {
                // If robots.txt doesn't exist, assume everything is allowed
                robotsTxtLoaded.put(domain, true);
                logger.debug("No robots.txt found at {}, assuming all crawling allowed", urlString);
                return true;
            } else {
                logger.debug("Failed to load robots.txt from {}, HTTP status: {}", urlString, responseCode);
                return false;
            }
        } catch (Exception e) {
            logger.debug("Error accessing robots.txt at {}: {}", urlString, e.getMessage());
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.error("Error closing reader: {}", e.getMessage());
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void parseRobotsTxt(BufferedReader reader, String domain) throws IOException {
        List<String> currentUserAgents = new ArrayList<>();
        String line;
        boolean inUserAgentSection = false;

        while ((line = reader.readLine()) != null) {
            // Skip comments and empty lines
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                if (line.isEmpty() && inUserAgentSection) {
                    // Empty line signals the end of a user agent section
                    inUserAgentSection = false;
                    currentUserAgents.clear();
                }
                continue;
            }

            // Check if line defines a user agent
            if (line.toLowerCase().startsWith("user-agent:")) {
                String[] parts = line.split(":", 2);
                if (parts.length > 1) {
                    String userAgent = parts[1].trim();

                    // If we were already in a user agent section and encounter another one,
                    // it's part of the same group unless separated by an empty line
                    if (!inUserAgentSection) {
                        currentUserAgents.clear();
                        inUserAgentSection = true;
                    }

                    currentUserAgents.add(userAgent);
                }
            }
            // Check for disallow and allow directives
            else if (line.toLowerCase().startsWith("disallow:") || line.toLowerCase().startsWith("allow:")) {
                if (currentUserAgents.isEmpty()) {
                    continue; // Skip rules not associated with a user agent
                }

                String[] parts = line.split(":", 2);
                if (parts.length > 1) {
                    String directive = parts[0].trim();
                    String path = parts[1].trim();

                    // Apply this rule to all current user agents
                    for (String userAgent : currentUserAgents) {
                        addPolicy(domain, userAgent, path, directive);
                    }
                }
            }
        }
    }

    // Get statistics about how many URLs are blocked
    public int getDisallowedRulesCount(String domain) {
        if (!disallowedPolicyMap.containsKey(domain)) {
            return 0;
        }

        int count = 0;
        for (List<String> rules : disallowedPolicyMap.get(domain).values()) {
            count += rules.size();
        }
        return count;
    }

    // Get statistics about robots.txt loading
    public int getFailedDomainsCount() {
        return (int) robotsTxtFailCount.values().stream().filter(count -> count >= MAX_RETRY_ATTEMPTS).count();
    }

    public static void main(String[] args) {
        String domain = "www.netflix.com";
        RobotsTxtParser parser = new RobotsTxtParser();
        parser.loadRobotsTxt(domain);

        String testUrl = "https://www.netflix.com/anan";
        String userAgent = "SeznamBot";

        boolean allowed = parser.isAllowed(testUrl, userAgent);
        logger.info("URL: {} is {} for user agent: {}", testUrl, allowed ? "Allowed" : "Not Allowed", userAgent);

        logger.info("Domain {} has {} disallowed rules", domain, parser.getDisallowedRulesCount(domain));
    }
}
package cu.searchengine.Crawler;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RobotsTxtParser {
    //each user agent with a list of it's prohibited paths  Domain ->(userAgent, disallowedPaths)
    private final HashMap<String, HashMap<String, List<String>>> disallowedPolicyMap = new HashMap<>();
    private final HashMap<String, HashMap<String, List<String>>> allowedPolicyMap = new HashMap<>();

    // checks if a path can be crawled
    public boolean isAllowed(String fullURL, String userAgent) {
        boolean isAllowedCarwling = true;

        try {
            URL url = new URL(fullURL);
            String domain = url.getHost();
            String path = url.getPath();
            String currentUserAgent = userAgent;

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

            // check if the path is prohibited for all agents
            List<String> disallowedAllAgents = disallowedPolicyMap.get(domain).get("*");
            if (disallowedAllAgents != null) {
                for (String directive : disallowedAllAgents) {
                    if (path.contains(directive)) {
                        System.out.println("Blocked by all agents " + directive);
                        isAllowedCarwling = false;
                        break;
                    }
                }
            }

            if (!currentUserAgent.equals("*")) {
                // Check if the path is prohibited for this certain user agent and wasn't blocked for all agents
                if (isAllowedCarwling) {
                    for (String directive : disallowedCurrentAgent) {
                        if (path.contains(directive)) {
                            System.out.println("Blocked by robots.txt: " + directive);
                            isAllowedCarwling = false;
                            break;
                        }
                    }
                } else {
                    // Check if the path is prohibited for all agents but accessible this certain user agent
                    HashMap<String, List<String>> policy = allowedPolicyMap.get(domain);
                    if (policy == null) return isAllowedCarwling;

                    List<String> allowedCurrentAgent = policy.get(userAgent);
                    if (allowedCurrentAgent == null) return isAllowedCarwling;

                    for (String directive : allowedCurrentAgent) {
                        if (path.contains(directive) && !directive.equals("/")) {
                            isAllowedCarwling = true;
                            break;
                        }
                    }

                }
            }
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL: " + fullURL);
            return false;
        }
        return isAllowedCarwling;
    }

    // adds a rule to a certain domain of a certain agent
    public void addPolicy(String domain, String userAgent, String rule, String policy) {
        if (policy.equalsIgnoreCase("disallow")) {
            disallowedPolicyMap.putIfAbsent(domain, new HashMap<>());
            disallowedPolicyMap.get(domain).putIfAbsent(userAgent, new ArrayList<>());
            disallowedPolicyMap.get(domain).get(userAgent).add(rule);
        }
        if (policy.equalsIgnoreCase("allow")) {
            allowedPolicyMap.putIfAbsent(domain, new HashMap<>());
            allowedPolicyMap.get(domain).putIfAbsent(userAgent, new ArrayList<>());
            allowedPolicyMap.get(domain).get(userAgent).add(rule);
        }
    }

    // loads robots.txt for a given domain
    public void loadRobotsTxt(String domain) {
        try {
            if (domain.startsWith("http")) {
                domain = new URL(domain).getHost(); // Extract domain if full URL is given
            }
            URL url = new URL("https://" + domain + "/robots.txt");

            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            if (connection.getResponseCode() == 200) {  // Check if robots.txt exists
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                List<String> userAgentsGroup = new ArrayList<>();
                String currentUserAgent = "";
                String line;
                boolean previousLineEmpty = false;

                while ((line = reader.readLine()) != null) {
                    // check if a new agent is mentioned
                    if (line.startsWith("User-agent")) {
                        if (previousLineEmpty) {
                            //start a new group
                            userAgentsGroup.clear();
                        }
                        currentUserAgent = line.split(":")[1].trim();
                        userAgentsGroup.add(currentUserAgent);
                    }

                    // check if a new disallowed path is mentioned
                    if (line.startsWith("Disallow:") || line.startsWith("Allow:")) {
                        String[] temp = line.split(":");
                        if (Array.getLength(temp) > 1) {
                            String path = temp[1].trim();
                            for (String userAgent : userAgentsGroup) {
                                addPolicy(domain, userAgent, path, temp[0]);
                            }
                        }
                    }
                    //to keep track of user agents group
                    previousLineEmpty = line.isEmpty();
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading robots.txt file " + e.getMessage());
        }
    }


    public static void main(String[] args) {
        String domain = "https://www.netflix.com/robots.txt";
        RobotsTxtParser parser = new RobotsTxtParser();
        parser.loadRobotsTxt(domain);
        System.out.println(parser.isAllowed("https://www.netflix.com/tudum", "Yahoo Pipes 1.0"));
    }
}
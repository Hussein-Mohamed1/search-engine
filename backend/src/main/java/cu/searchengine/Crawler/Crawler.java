package cu.searchengine.Crawler;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import cu.searchengine.utils.ResourceReader;

import java.io.*;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.core.io.DefaultResourceLoader;


public class Crawler {
    private final HashSet<String> visitedURLSet;
    private final Queue<String> urlQueue;
    private final RobotsTxtParser robotsParser;
    private final URLNormalizer normalizer;
    private final ResourceReader resourceReader;
    private final String userAgent;
    private final int pageCount;
    private final ExecutorService executorService;

    public Crawler(String userAgent, int pgCount ,int numberOfThreads) {
        this.userAgent = userAgent;
        this.pageCount = pgCount;
        visitedURLSet = new HashSet<>();
        urlQueue = new LinkedList<>();
        normalizer = new URLNormalizer();
        robotsParser = new RobotsTxtParser();
        resourceReader = new ResourceReader(new DefaultResourceLoader());
        executorService = Executors.newFixedThreadPool(numberOfThreads);
    }

    void loadSeeds() {
        try {
            String content = resourceReader.loadResourceAsString("classpath:static/seeds.txt");
            String[] lines = content.split("\\n");
            for (String line : lines) {
                line = line.trim();
                if (isValidURL(line)) {
                    urlQueue.add(line);
                    visitedURLSet.add(normalizer.normalize(line));
                } else {
                    System.out.println("Skipping invalid URL: " + line);
                }
            }
        } catch (Exception ex) {
            System.err.println("Error reading from classpath: " + ex.getMessage());
        }
    }

    boolean isValidURL(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            System.out.println("Skipping invalid URL: " + url);
            return false;
        }
    }

    boolean headRequest(String url) {

        try {
            Connection.Response response = Jsoup.connect(url).method(Connection.Method.HEAD).execute();

            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 400) {
                System.out.println("URL is accessible. status: " + statusCode);
                return true;
            } else {
                System.out.println("URL is not accessible. Status: " + statusCode);
                return false;
            }

        } catch (IOException e) {
            System.out.println("Error during HEAD request: " + e.getMessage());
            return false;
        }
    }

    void crawl() {
        loadSeeds();
        int currentPage = 0;

        while (!urlQueue.isEmpty() && currentPage < pageCount) {
            String url = urlQueue.remove();
            String normalizedURL = normalizer.normalize(url);

            // load the urls robots.txt for the domain (normalized link)
            robotsParser.loadRobotsTxt(normalizedURL);

            if (!robotsParser.isAllowed(url, userAgent)) {
                System.out.println("Crawling disallowed for URL: " + url);
                continue;
            }
            try {
                // make a head request first before loading the page content
                if (!headRequest(url)) continue;

                // get html content of the url's page
                Document doc = Jsoup.connect(url).get();

                System.out.println("======>Crawling URL: " + url);

                // extract the links from it
                Elements links = doc.select("a");
                for (Element link : links) {
                    String linkURL = link.attr("abs:href");
                    System.out.println("obtained link " + linkURL);
                    String normalizedLinkURL = normalizer.normalize(linkURL);
                    if (normalizedLinkURL == null || normalizedLinkURL.isEmpty()) continue;

                    if (!visitedURLSet.contains(normalizedLinkURL)) {
                        urlQueue.add(normalizedLinkURL);
                        visitedURLSet.add(normalizedLinkURL);
                    }
                }
                // page is crawled
                currentPage++;
            } catch (org.jsoup.HttpStatusException e) {
                System.out.println("HTTP error fetching URL. Status=" + e.getStatusCode() + ", URL=[" + url + "]");
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

    }

    public static void main(String[] args) {
        try {
            Crawler crawler = new Crawler("nemo",10,10000);
            crawler.loadSeeds();
            crawler.crawl();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
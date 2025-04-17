package Crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utils.ResourceReader;

import java.io.*;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import org.springframework.core.io.DefaultResourceLoader;


public class Crawler {
    private static final HashSet<String> visitedURLSet = new HashSet<>();
    private static final URLNormalizer normalizer = new URLNormalizer();
    private static final Queue<String> urlQueue = new LinkedList<>();
    private final ResourceReader resourceReader;

    public Crawler() {
        resourceReader = new ResourceReader(new DefaultResourceLoader());
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

    void crawl() {
        try {
            loadSeeds();
            while (!urlQueue.isEmpty()) {
                String url = urlQueue.remove();
                Document doc = Jsoup.connect(url).get();
                Elements links = doc.select("a");
                for (Element link : links) {
                    String linkURL = link.attr("abs:href");
                    System.out.println(linkURL);
                    String normalizedLinkURL = normalizer.normalize(linkURL);
                    if (normalizedLinkURL == null || normalizedLinkURL.isEmpty()) continue;
                    {
                    }

                    if (!visitedURLSet.contains(normalizedLinkURL)) {
                        urlQueue.add(normalizedLinkURL);
                        visitedURLSet.add(normalizedLinkURL);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            Crawler crawler = new Crawler();
            crawler.crawl();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
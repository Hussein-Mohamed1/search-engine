package cu.searchengine.Crawler;

import cu.searchengine.model.Documents;
import cu.searchengine.repository.DocumentsRepository;
import cu.searchengine.service.DocumentService;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import cu.searchengine.utils.ResourceReader;
import cu.searchengine.model.WebDocument;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;

public class Crawler implements Runnable {
    private final ConcurrentHashMap<String, Boolean> visitedURLSet;
    private final BlockingQueue<String> urlQueue;
    private final URLNormalizer normalizer;
    private final RobotsTxtParser robotsParser;
    private final ResourceReader resourceReader;
    private final String userAgent;
    private final int MAX_PAGE_COUNT;
    private final int WAIT_QUEUE_CAPACITY;
    private final AtomicInteger currentPage;
    private final ExecutorService executorService;
    private final List<WebDocument> webDocuments;
    private final HashMap<String, Boolean> pages404;
    private final DocumentService documentService;

    public Crawler(String userAgent, int pgCount, int numberOfThreads, int queueCapacity, DocumentService doucumentService) {
        this.userAgent = userAgent;
        this.MAX_PAGE_COUNT = pgCount;
        this.documentService = doucumentService;
        this.currentPage = new AtomicInteger(0);
        this.visitedURLSet = new ConcurrentHashMap<>();
        this.urlQueue = new LinkedBlockingQueue<>();
        this.normalizer = new URLNormalizer();
        this.robotsParser = new RobotsTxtParser();
        this.resourceReader = new ResourceReader(new DefaultResourceLoader());
        this.WAIT_QUEUE_CAPACITY = queueCapacity;
        this.executorService = new ThreadPoolExecutor(
                numberOfThreads,
                numberOfThreads,
                10L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(this.WAIT_QUEUE_CAPACITY),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        this.webDocuments = new ArrayList<>();
        this.pages404 = new HashMap<>();

        loadSeeds();
    }

    @Override
    public void run() {
        while (!urlQueue.isEmpty() || currentPage.get() < MAX_PAGE_COUNT) {
            String url = urlQueue.poll();
            if (url == null) continue;

            String threadName = Thread.currentThread().getName();
            String normalizedURL = normalizer.normalize(url);

            if (!robotsParser.isLoaded(normalizedURL)) {
                System.out.println(threadName + " - Loading robots.txt for: " + normalizedURL);
                robotsParser.loadRobotsTxt(normalizedURL);
            }


            if (!robotsParser.isAllowed(url, userAgent)) {
                System.out.println(threadName + " - Crawling disallowed by robots.txt: " + url);
                continue;
            }

            try {
                if (!headRequest(url)) continue;

                if (currentPage.incrementAndGet() > MAX_PAGE_COUNT) {
                    currentPage.decrementAndGet();
                    return;
                }

                processPage(normalizedURL);

            } catch (Exception e) {
                System.out.println(threadName + " - Error: " + e.getMessage());
            }
        }
        System.out.println(Thread.currentThread().getName() + " finished.");
    }

    private void loadSeeds() {
        try {
            String content = resourceReader.loadResourceAsString("classpath:static/seeds.txt");
            String[] lines = content.split("\\n");
            for (String line : lines) {
                line = line.trim();
                if (isValidURL(line)) {
                    urlQueue.add(line);
                    visitedURLSet.put(normalizer.normalize(line), true);
                } else {
                    System.out.println("Skipping invalid URL: " + line);
                }
            }
        } catch (Exception ex) {
            System.err.println("Error reading from classpath: " + ex.getMessage());
        }
    }

    private boolean isValidURL(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            System.out.println("Skipping invalid URL: " + url);
            return false;
        }
    }

    private boolean headRequest(String url) {
        try {
            Connection.Response response = Jsoup.connect(url).method(Connection.Method.HEAD).timeout(2000).execute();
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 400) {
//                System.out.println("URL is accessible. status: " + statusCode);
                return true;
            } else {
//                System.out.println("URL is not accessible. Status: " + statusCode);
                pages404.put(url, true);
                return false;
            }
        } catch (IOException e) {
//            System.out.println("Error during HEAD request: " + e.getMessage());
            return false;
        }
    }

    private void addURLToQueue(String normalizedLinkURL) {
        if (visitedURLSet.putIfAbsent(normalizedLinkURL, true) == null) {
            urlQueue.add(normalizedLinkURL);
        }
    }

    private void parseDocument(Document doc) {
        String title = doc.title();
        String url = doc.baseUri();

        if (pages404.get(url) != null) return;

        String content = doc.select("div, p").text();

        // todo remove empty one
        List<String> mainHeadings = doc.select("h1").parallelStream()
                .map(Element::text)
                .filter(text -> !text.trim().isEmpty())
                .toList();

        List<String> subHeadings = doc.select("h2, h3, h4, h5, h6").parallelStream()
                .map(Element::text)
                .filter(text -> !text.trim().isEmpty())
                .toList();

        List<String> links = doc.select("a").parallelStream()
                .map(link -> {
                    String linkURL = link.attr("href");
                    if (!linkURL.startsWith("http")) {
                        linkURL = doc.baseUri() + linkURL;
                    }
                    return linkURL;
                })
                .filter(text -> !text.trim().isEmpty())
                .toList();

        // todo modify mainheading in webdocument file to be a list
        documentService.add(new Documents(url, title, mainHeadings, subHeadings, content, links));
    }

    private void processPage(String url) throws IOException {
        Document doc = Jsoup.connect(url).timeout(2000).get();
        parseDocument(doc);
        System.out.println("Thread " + Thread.currentThread().getName() + "======> Crawling URL: " + url);

        Elements links = doc.select("a");
        for (Element link : links) {
            String linkURL = link.attr("abs:href");
            String normalizedLinkURL = normalizer.normalize(linkURL);
            if (normalizedLinkURL == null || normalizedLinkURL.isEmpty()) continue;

            addURLToQueue(normalizedLinkURL);
        }
    }

    private void shutdownExecutorService() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
                System.out.println("Forcing shutdown...");
                executorService.shutdownNow();
            } else {
                System.out.println("All tasks completed.");
            }
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted while waiting for termination.");
            executorService.shutdownNow();
        }
    }

    public void crawl() {
        int numThreads = ((ThreadPoolExecutor) executorService).getCorePoolSize();
        for (int i = 0; i < numThreads; i++) {
            executorService.submit(this);
        }
        shutdownExecutorService();
    }

    void print() {
        System.out.println("PageCount: " + currentPage.get());
        System.out.println("URLQueue: " + urlQueue.size());
        System.out.println("VisitedURLSet: " + visitedURLSet.size());
        for (WebDocument webDocument : webDocuments) {
            System.out.println(webDocument);
        }
    }

    // todo change scheduling time (we can divide the 6k pages)
    @Scheduled(fixedRate = 300000)
    public void schedulueCrawling() {
        System.out.println("\nStarting scheduled crawling...\n");
        this.crawl();
        System.out.println("\nEnd of scheduled crawling...\n");
        this.print();
    }
}


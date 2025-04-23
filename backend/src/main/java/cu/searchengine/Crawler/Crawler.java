package cu.searchengine.Crawler;

import cu.searchengine.model.Documents;
import cu.searchengine.model.WebDocument;
import cu.searchengine.service.DocumentService;
import cu.searchengine.utils.ResourceReader;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The crawler maintains a single set:
 * <p>
 * visitedURLSet: URLs we've seen anywhere (links we've discovered)
 * - Populated initially from the database
 * - Prevents re-queueing the same URLs
 * - Preserves discovery history across runs
 * - SHOULD NOT be reset after crawling
 * <p>
 * We rely on MongoDB's unique constraint on URL to prevent duplicate documents
 * from being inserted in the database.
 */

@Component
public class Crawler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Crawler.class);
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
    private final List<Documents> buffer = new CopyOnWriteArrayList<>();
    static final int GLOBAL_TIMEOUT = 10_000;

    @Autowired
    public Crawler(DocumentService documentService) {
        // Set default values for other fields as needed
        this("lumos", // userAgent
                1000, // MAX_PAGE_COUNT
                50, // numberOfThreads
                1000, // queueCapacity
                documentService);
    }

    public Crawler(String userAgent, int pgCount, int numberOfThreads, int queueCapacity, DocumentService documentService) {
        this.userAgent = userAgent;
        this.MAX_PAGE_COUNT = pgCount;
        this.documentService = documentService;
        this.currentPage = new AtomicInteger(0);
        this.visitedURLSet = new ConcurrentHashMap<>();
        this.urlQueue = new LinkedBlockingQueue<>();
        this.normalizer = new URLNormalizer();
        this.robotsParser = new RobotsTxtParser();
        this.resourceReader = new ResourceReader(new DefaultResourceLoader());
        this.WAIT_QUEUE_CAPACITY = queueCapacity;
        this.executorService = new ThreadPoolExecutor(numberOfThreads, numberOfThreads, 10L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(this.WAIT_QUEUE_CAPACITY), new ThreadPoolExecutor.CallerRunsPolicy());
        this.webDocuments = new ArrayList<>();
        this.pages404 = new HashMap<>();

        logger.info("Crawler initialized with userAgent={}, maxPages={}, threads={}, queueCapacity={}", userAgent, pgCount, numberOfThreads, queueCapacity);

        // Load visited URLs from DB to persist across runs
        loadVisitedUrlsFromDb();
        loadSeeds();
    }

    private void loadVisitedUrlsFromDb() {
        try {
            List<Documents> allDocs = documentService.getAllDocuments();
            for (Documents doc : allDocs) {
                String normalized = normalizer.normalize(doc.getUrl());
                visitedURLSet.put(normalized, true);
            }
            logger.info("Loaded {} visited URLs from DB", visitedURLSet.size());
        } catch (Exception e) {
            logger.error("Failed to load visited URLs from DB, {}", e.getMessage());
        }
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        logger.debug("[{}] Crawler thread started.", threadName);
        long startTime = System.currentTimeMillis();

        // Change the while condition to prevent possible thread starvation
        // Note: In previous condition, !urlQueue.isEmpty() || currentPage.get() < MAX_PAGE_COUNT,
        // if crawling is over and queue is empty but currentPage.get() < MAX_PAGE_COUNT is true, this thread will get starved
        while (!urlQueue.isEmpty() && currentPage.get() < MAX_PAGE_COUNT) {
            String url = urlQueue.poll();
            if (url == null) continue;

            String normalizedURL = normalizer.normalize(url);

            if (!robotsParser.isLoaded(normalizedURL)) {
                logger.debug("[{}] Loading robots.txt for: {}", threadName, normalizedURL);
                robotsParser.loadRobotsTxt(normalizedURL);
            }

            if (!robotsParser.isAllowed(url, userAgent)) {
                logger.debug("[{}] Crawling disallowed by robots.txt: {}", threadName, url);
                continue;
            }

            try {
                if (!headRequest(url)) continue;

                // This block ensures the crawler does not exceed the maximum allowed pages.
                // It increments the page count, checks if it exceeds the limit, and if so, decrements and exits.
                if (currentPage.incrementAndGet() > MAX_PAGE_COUNT) {
                    currentPage.decrementAndGet();
                    return;
                }

                logger.debug("[{}] Processing page: {}", threadName, normalizedURL);
                processPage(normalizedURL);

            } catch (Exception e) {
                logger.error("[{}] Error: {}", threadName, e.getMessage());
            }
        }
        logger.info("[{}] finished. Elapsed: {} ms", threadName, System.currentTimeMillis() - startTime);
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
                    logger.debug("Skipping invalid URL: {}", line);
                }
            }
        } catch (Exception ex) {
            logger.error("Error reading from classpath: {}", ex.getMessage());
        }
    }

    private boolean isValidURL(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            logger.error("Skipping invalid URL: {}", url);
            return false;
        }
    }

    private boolean headRequest(String url) {
        try {
            Connection.Response response = Jsoup.connect(url).method(Connection.Method.OPTIONS).timeout(GLOBAL_TIMEOUT).execute();
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 400) {
                logger.debug("URL is accessible. status: {}", statusCode);
                return true;
            } else {
                logger.debug("URL is not accessible. Status: {}", statusCode);
                pages404.put(url, true);
                return false;
            }
        } catch (IOException e) {
            logger.debug("Error during HEAD request: {}", e.getMessage());
            return false;
        }
    }

    // if a given url isn't in the visitedUrlSet then putIfAbsent inserts it and returns null
    // consequently it gets added to the urlQueue to get processed
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

        List<String> mainHeadings = doc.select("h1").parallelStream().map(Element::text).filter(text -> !text.trim().isEmpty()).toList();

        List<String> subHeadings = doc.select("h2, h3, h4, h5, h6").parallelStream().map(Element::text).filter(text -> !text.trim().isEmpty()).toList();

        List<String> links = doc.select("a").parallelStream().map(link -> {
            String linkURL = link.attr("href");
            if (!linkURL.startsWith("http")) {
                linkURL = doc.baseUri() + linkURL;
            }
            return linkURL;
        }).filter(text -> !text.trim().isEmpty()).toList();

        // The correct approach - Always add current document to buffer
        // By this point, we've already decided to process this URL
        // It passed the robots.txt check, HEAD request, and is part of our crawl
        // Also, Don't pay attention to duplicate insertions since the url is the key
        buffer.add(new Documents(url, title, mainHeadings, subHeadings, content, links));

        // save documents in batches of 100
        if (buffer.size() >= 100) {
            flushBuffer();
        }
    }

    // Saves a batch of documents
    private synchronized void flushBuffer() {
        if (buffer.isEmpty()) return;
        try {
            documentService.addAll(buffer);
        } catch (Exception e) {
            // Ignore duplicate key errors, log others
            if (!e.getMessage().contains("duplicate key")) {
                logger.error("Bulk insert error,{}", e.getMessage());
            }
        }
        buffer.clear();
    }

    private void processPage(String url) throws IOException {
        // Increase timeout to 10 seconds (10000 ms)
        Document doc = Jsoup.connect(url).timeout(GLOBAL_TIMEOUT).get();
        parseDocument(doc);
        logger.debug("Thread {}: Crawling URL: {}", Thread.currentThread().getName(), url);

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
                logger.warn("Forcing shutdown of crawler thread pool...");
                executorService.shutdownNow();
            } else {
                logger.info("All crawler tasks completed.");
            }
        } catch (InterruptedException e) {
            logger.error("Thread interrupted while waiting for termination. {}", e.getMessage());
            executorService.shutdownNow();
        }
    }

    public void crawl() {
        int numThreads = ((ThreadPoolExecutor) executorService).getCorePoolSize();
        logger.info("Starting crawl with {} threads. URL queue size: {}", numThreads, urlQueue.size());
        long start = System.currentTimeMillis();
        for (int i = 0; i < numThreads; i++) {
            executorService.submit(this);
        }
        shutdownExecutorService();

        // Make sure to flush any remaining documents in buffer
        flushBuffer();

        // Clear the URL queue once at the end
        urlQueue.clear();

        logger.info("Crawling finished in {} ms", System.currentTimeMillis() - start);
    }

    void print() {
        logger.info("PageCount: {}", currentPage.get());
        logger.info("URLQueue: {}", urlQueue.size());
        logger.info("VisitedURLSet: {}", visitedURLSet.size());
        for (WebDocument webDocument : webDocuments) {
            logger.info(String.valueOf(webDocument));
        }
    }


}

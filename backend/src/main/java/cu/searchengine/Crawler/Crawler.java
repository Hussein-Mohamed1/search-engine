package cu.searchengine.Crawler;

import cu.searchengine.model.Documents;
import cu.searchengine.service.DocumentService;
import cu.searchengine.utils.ResourceReader;
import lombok.Setter;
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

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
    private final HashMap<String, Boolean> pages404;
    private final DocumentService documentService;
    private final List<Documents> buffer = new CopyOnWriteArrayList<>();
    static final int GLOBAL_TIMEOUT = 10_000;

    // File name for serialization
    private static final String STATE_FILE = "crawler_state.ser";

    // Checkpoint frequency (save state every X pages)
    private static final int CHECKPOINT_FREQUENCY = 100;

    @Autowired
    public Crawler(DocumentService documentService) {
        // Set default values for other fields as needed
        this("Mozilla/5.0 (Windows NT 11.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.6998.166 Safari/537.36", // userAgent
                6000, // MAX_PAGE_COUNT
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
        this.pages404 = new HashMap<>();

        logger.info("Crawler initialized with userAgent={}, maxPages={}, threads={}, queueCapacity={}", userAgent, pgCount, numberOfThreads, queueCapacity);

        // Try to restore previous state or load initial state if restoration fails
        if (!restoreState()) {
            // Load visited URLs from DB to persist across runs
            loadVisitedUrlsFromDb();
            loadSeeds();
        }

        // Add shutdown hook to save state when application exits
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveState));
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
//                if (!headRequest(url)) continue;

                // This block ensures the crawler does not exceed the maximum allowed pages.
                // It increments the page count, checks if it exceeds the limit, and if so, decrements and exits.
                int currentCount = currentPage.incrementAndGet();
                if (currentCount > MAX_PAGE_COUNT) {
                    currentPage.decrementAndGet();
                    return;
                }

                // Checkpoint state periodically
                if (currentCount % CHECKPOINT_FREQUENCY == 0) {
                    synchronized (this) {
                        saveState();
                    }
                }

                logger.debug("[{}] Processing page: {}", threadName, normalizedURL);
                processPage(normalizedURL);

            } catch (Exception e) {
                logger.debug("[{}] Error: {}", threadName, e.getMessage());
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
                logger.info("URL is not accessible. Status: {}", statusCode);
                pages404.put(url, true);
                return false;
            }
        } catch (IOException e) {
            logger.info("Error during HEAD request: {}", e.getMessage());
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

        // --- Populate webGraph (outgoing links as IDs) using Java streams ---
        HashSet<Integer> webGraph = links.stream()
                .map(normalizer::normalize)
                .filter(normalized -> normalized != null && !normalized.isEmpty())
                .map(String::hashCode)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);

        Documents document = new Documents(url, title, mainHeadings, subHeadings, content, links);
        document.setWebGraph(webGraph); // set webGraph as outgoing link IDs

        // The correct approach - Always add current document to buffer
        buffer.add(document);

        // save documents in batches of 100
        if (buffer.size() >= 100) {
            flushBuffer();
        }
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

        // Save state before finishing
        saveState();

        // Clear the URL queue once at the end
        urlQueue.clear();

        logger.info("Crawling finished in {} ms", System.currentTimeMillis() - start);
    }

    void print() {
        logger.info("PageCount: {}", currentPage.get());
        logger.info("URLQueue: {}", urlQueue.size());
        logger.info("VisitedURLSet: {}", visitedURLSet.size());
    }

    /**
     * Saves the current state of the crawler to disk for recovery
     */
    public synchronized void saveState() {
        logger.info("Saving crawler state: {} pages crawled, {} URLs in queue", currentPage.get(), urlQueue.size());
        List<String> queueArray = new ArrayList<>(urlQueue);
        // Wrap all state data in a single object
        CrawlerState fullState = new CrawlerState(
                currentPage.get(),
                queueArray,
                new HashMap<>(visitedURLSet)
        );

        try (FileOutputStream fos = new FileOutputStream(STATE_FILE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(fullState);
            logger.info("Full crawler state saved successfully ({} URLs, {} visited)", fullState.urlQueue.size(), fullState.visitedURLs.size());
        } catch (IOException e) {
            logger.error("Failed to save crawler state: {}", e.getMessage());
        }
    }

    /**
     * Restores the crawler state from disk
     * @return true if state was successfully restored, false otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean restoreState() {
        File stateFile = new File(STATE_FILE);

        if (!stateFile.exists()) {
            logger.info("No previous state found, starting fresh crawl");
            return false;
        }

        try (FileInputStream fis = new FileInputStream(STATE_FILE);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            CrawlerState fullState = (CrawlerState) ois.readObject();
            currentPage.set(fullState.getCurrentPage());

            urlQueue.clear();
            urlQueue.addAll(fullState.getUrlQueue());

            visitedURLSet.clear();
            visitedURLSet.putAll(fullState.getVisitedURLs());

            logger.info("Restored crawler state: {} pages, {} queued URLs, {} visited URLs",
                    currentPage.get(), urlQueue.size(), visitedURLSet.size());

            return true;

        } catch (Exception e) {
            logger.error("Failed to restore crawler state: {}", e.getMessage());
            return false;
        }
    }


    /**
     * Serializable class to store crawler state
     */
    private static class CrawlerState implements Serializable {
        private static final long serialVersionUID = 1L;
        @Setter
        private int currentPage;
        private List<String> urlQueue;
        private HashMap<String, Boolean> visitedURLs;

        public CrawlerState(int currentPage, List<String> urlQueue, HashMap<String, Boolean> visitedURLs) {
            this.currentPage = currentPage;
            this.urlQueue = urlQueue;
            this.visitedURLs = visitedURLs;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public List<String> getUrlQueue() {
            return urlQueue;
        }

        public HashMap<String, Boolean> getVisitedURLs() {
            return visitedURLs;
        }
    }
}
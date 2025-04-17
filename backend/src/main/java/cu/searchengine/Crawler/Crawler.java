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
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.core.io.DefaultResourceLoader;

public class Crawler implements Runnable {
    private final ConcurrentHashMap<String, Boolean> visitedURLSet;
    private final BlockingQueue<String> urlQueue;
    private final URLNormalizer normalizer;
    private final RobotsTxtParser robotsParser;
    private final ResourceReader resourceReader;
    private final String userAgent;
    private final int MAX_PAGE_COUNT;
    private final AtomicInteger currentPage;
    private final ExecutorService executorService;
    private final List<WebDocument> webDocuments;
    private final DocumentService documentService;

    public Crawler(String userAgent, int pgCount, int numberOfThreads, DocumentService doucumentService) {
        this.userAgent = userAgent;
        this.MAX_PAGE_COUNT = pgCount;
        this.documentService = doucumentService;
        this.currentPage = new AtomicInteger(0);
        this.visitedURLSet = new ConcurrentHashMap<>();
        this.urlQueue = new LinkedBlockingQueue<>();
        this.normalizer = new URLNormalizer();
        this.robotsParser = new RobotsTxtParser();
        this.resourceReader = new ResourceReader(new DefaultResourceLoader());
        this.executorService = Executors.newFixedThreadPool(numberOfThreads);
        this.webDocuments = new ArrayList<>();

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

    private void addURLToQueue(String normalizedLinkURL) {
        if (visitedURLSet.putIfAbsent(normalizedLinkURL, true) == null) {
            urlQueue.add(normalizedLinkURL);
        }
    }

    private void parseDocument(Document doc) {
        System.out.println(" - Parsing " + doc.title());
        String title = doc.title();
        String url = doc.baseUri();
        String mainHeading = doc.select("h1").text();
        List<String> subHeadings = new ArrayList<>();
        List<String> links = new ArrayList<>();

        Elements headers = doc.select("h2, h3, h4, h5, h6");

        for (Element header : headers) {
            subHeadings.add(header.text());
        }

        Elements linkElements = doc.select("a");
        for (Element link : linkElements) {
            String linkURL = link.attr("href");

            if (!link.attr("href").contains("http"))
                linkURL = doc.baseUri() + link.attr("href");

            // todo handle 404 pages
            links.add(linkURL);
        }


        String content = doc.select("div, p").text();
        webDocuments.add(new WebDocument(url, title, mainHeading, subHeadings, content, links));
        documentService.add(new Documents(url, title, mainHeading, subHeadings, content, links));

        //========> testing
        System.out.println("title:" + title);
        System.out.println(" - Parsing " + url);
        System.out.println("mainHeading:" + mainHeading);
        System.out.println("subHeadings:" + subHeadings);
        System.out.println("links:" + links);
//        System.out.print(" - Parsing " + content);

    }

    //todo should i add @Getter
    public List<WebDocument> getWebDocuments() {
        return webDocuments;
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

    private void crawl() {
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
    }

    public static void main(String[] args) {
        int numberOfThreads = Runtime.getRuntime().availableProcessors() * 2;
//        System.out.println("Number of threads: " + numberOfThreads);
        DocumentsRepository documentsRepository = null;
        DocumentService documentService1 = new DocumentService(documentsRepository);
        Crawler crawler = new Crawler("nemo", 200, numberOfThreads , documentService1);

        Document doc = null;
        try {
            doc = Jsoup.connect("https://spring.io/projects/spring-boot").timeout(2000).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        crawler.parseDocument(doc);
//        crawler.crawl();
//        crawler.print();

    }
}



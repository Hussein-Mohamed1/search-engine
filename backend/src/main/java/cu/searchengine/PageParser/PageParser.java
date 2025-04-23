package cu.searchengine.PageParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PageParser {
    private static final Logger logger = LoggerFactory.getLogger(PageParser.class);

    public List<String> getLinks(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            Elements linksElement = doc.select("a");
            List<String> linkList = new ArrayList<>();
            for (Element link : linksElement) {
                String linkURL = link.attr("abs:href");
                linkList.add(linkURL);
            }
            return linkList;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    public void getDocument(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            String title = doc.title();
            String mainHeading = doc.head().text();
            String subHeading = doc.head().text();
            String content = doc.body().text();
            List<String> links = getLinks(url);

        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            Document doc = Jsoup.connect("https://jsoup.org/").get();
            Elements Heading = doc.select("h2");
            for (Element head : Heading) {
                logger.debug(head.text());
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SemanticCrawler {
    private String startingUrl;
    private String targetUrl, targetTitle;
    private String token;

    //Note: to avoid skewing the semantic API's ratings, we remove " - Wikipedia" from all titles being processed
    public SemanticCrawler(String startingUrl, String targetUrl, String token) throws IOException {
        this.startingUrl = startingUrl;
        this.targetUrl = targetUrl;
        this.token = token;
        this.targetTitle = Jsoup.connect(targetUrl).get().title().replace(" - Wikipedia", "");
    }

    public void beginCrawling() {
        semanticCrawl(startingUrl, new ArrayList<String>(), token);
    }

    public void semanticCrawl(String url, ArrayList<String> visitedUrls, String token){
        if (url.equals(targetUrl) || visitedUrls.contains(url)) {
            requestDoc(url, visitedUrls);
            System.exit(0);
        }
        var document = requestDoc(url, visitedUrls);
        if (document == null) return;
        Elements docLinks = document.select("a");
        var map = new HashMap<String, String>();
        for (Element link : docLinks) {
            //map the text of the url to the actual url link
            map.put(
                    link.text().replace(" - Wikipedia", ""),
                    link.absUrl("href")
            );
        }
        try {
            // Send the candidates and target up to the API to get the closest URL
            List<String> candidateTexts = map.keySet().stream().toList();
            candidateTexts = Main.semanticSortTexts(targetTitle, candidateTexts, token);
            if (candidateTexts == null) return;
            for (String text : candidateTexts) {
                // Crawl into the URL if it hasn't already been visited
                String nextUrl = map.get(text);
                if (nextUrl == null || nextUrl.isBlank() || !nextUrl.startsWith("https://en.wikipedia.org/wiki/"))
                    continue;
                if (!visitedUrls.contains(nextUrl)) {
                    semanticCrawl(nextUrl, visitedUrls, token);
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private Document requestDoc(String url, ArrayList<String> visitedUrls) {
        var connection = Jsoup.connect(url);
        try {
            var document = connection.get();
            if (connection.response().statusCode() != 200) return null;
            visitedUrls.add(url);
            System.out.println("---------------------------------------------------------------------");
            System.out.println("Step " + visitedUrls.size());
            System.out.println("Link: " + url + "\nTitle: " + document.title().replace(" - Wikipedia", ""));
            return document;
        } catch (IOException e) {
            System.err.println("Could not get document: " + e.getMessage());
        }
        return null;
    }

}
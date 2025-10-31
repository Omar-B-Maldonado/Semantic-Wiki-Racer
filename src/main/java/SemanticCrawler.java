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
    private String token; //crawler hangs onto the token b/c it needs to constantly ask the API to sort links
    private final ArrayList<String> visitedUrls;

    //Note: to avoid skewing the semantic API's ratings, we remove " - Wikipedia" from all titles being processed
    public SemanticCrawler(String startingUrl, String targetUrl, String token) throws IOException {
        this.startingUrl = startingUrl;
        this.targetUrl = targetUrl;
        this.targetTitle = formatTitle(Jsoup.connect(targetUrl).get().title());
        this.token = token;
        this.visitedUrls = new ArrayList<>();
    }

    public void beginCrawling() {
        semanticCrawl(startingUrl, token);
    }
    // Recursive method
    private void semanticCrawl(String url, String token){
        var document = requestDoc(url);
        if (document == null) return;
        else printCurrentPage(url, document); // For visual evidence!
        if (url.equals(targetUrl)) { // Base case
            System.exit(0);
        }
        HashMap<String, String> map = mapTitlesToHyperlinks(document);
        List<String> candidateTitles = map.keySet().stream().toList();

        try {// Sort the candidate titles in relation to the target title via the Oracle API
            candidateTitles = SemanticAPIClient.semanticSort(candidateTitles, targetTitle, token);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        for (String title : candidateTitles) {
            String nextUrl = map.get(title);
            if (!visitedUrls.contains(nextUrl)) {
                semanticCrawl(nextUrl, token);
            }
        }
    }

    private Document requestDoc(String url) {
        var connection = Jsoup.connect(url);
        try {
            var document = connection.get();
            int statusCode = connection.response().statusCode();
            if (statusCode != 200) {
                throw new IOException("Connection Status Code: " + statusCode);
            } else {
                visitedUrls.add(url);
                return document;
            }
        } catch (IOException e) {
            System.err.println("Could not get document: " + e.getMessage());
            return null;
        }
    }
    /**
     * Map the title of the wiki page url to the actual url link
     * Ex, for: <a href="https://en.wikipedia.org/wiki/WhateverTitle">Whatever Title - Wikipedia</a>
     * map -> { "Whatever Title" : "https://en.wikipedia.org/wiki/WhateverTitle" } */
    private HashMap<String, String> mapTitlesToHyperlinks(Document doc) {
        Elements docLinks = doc.select("a");
        var map = new HashMap<String, String>();
        for (Element link : docLinks) {
            String urlTitle = formatTitle(link.text());
            String fullUrl = link.absUrl("href");
            if (fullUrl.startsWith("https://en.wikipedia.org/wiki/") && !urlTitle.isBlank()) {
                map.put(urlTitle, fullUrl);
            }
        }
        return map;
    }

    private void printCurrentPage(String url, Document doc) {
        System.out.println("---------------------------------------------------------------------");
        System.out.println("Step " + visitedUrls.size() + ":");
        System.out.println("Link: " + url + "\nTitle: " + formatTitle(doc.title()));
    }

    private String formatTitle(String title) {
        return title.replace(" - Wikipedia", "");
    }
}
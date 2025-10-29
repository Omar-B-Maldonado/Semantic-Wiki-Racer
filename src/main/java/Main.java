import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import io.github.cdimascio.dotenv.Dotenv;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
    public static HttpClient   CLIENT       = HttpClient.newHttpClient();
    public static Dotenv       ENV_FILE     = Dotenv.load();
    public static String       AUTH_BASE_URL= ENV_FILE.get("OML_AUTH_BASE_URL");
    public static String       OML_BASE_URL = ENV_FILE.get("OML_SERVICE_BASE_URL");
    public static ObjectMapper JSON_MAPPER  = new ObjectMapper();
    public static HttpResponse.BodyHandler<String> STRING_BODYHANDLER = HttpResponse.BodyHandlers.ofString();

    private static String getToken(String password) throws IOException, InterruptedException {
        var payloadString = JSON_MAPPER.writeValueAsString(Map.of(
                "grant_type", "password",
                "username", "admin",
                "password", password
        ));
        var request = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_BASE_URL + "api/oauth2/v1/token"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payloadString, StandardCharsets.UTF_8))
                .build();
        var response = CLIENT.send(request, STRING_BODYHANDLER);
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get token: " + response.body());
        }
        Map<String, Object> json = JSON_MAPPER.readValue(response.body(), Map.class);
        return json.get("accessToken").toString();
    }

    /* from a list of unsorted strings, sorts the strings by how
     * semantically-similar they are to the target string,
     * where the first element is most similar and the last element is the least */
    public static List<String> semanticSortTexts(String target, List<String> texts, String token) throws IOException, InterruptedException {
        //The API has a max batchsize, so we must send our text in batches
        final int MAX_BATCH_SIZE = 1000;
        List<String> sortedTexts = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += MAX_BATCH_SIZE) {
            int end = Math.min(i + MAX_BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(i, end);

            var payloadString = JSON_MAPPER.writeValueAsString(Map.of(
                    "language", "AMERICAN",
                    "probe", target,
                    "sortDirection", "DESC",
                    "textList", batch
            ));
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(OML_BASE_URL + "v1/cognitive-text/similarity"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payloadString, StandardCharsets.UTF_8))
                    .build();
            var response = CLIENT.send(request, STRING_BODYHANDLER);
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to get semantic similarities: " + response.body());
            }
            /* the json is stored as a list of mappings from String to (String) Object,
             * where each mapping contains {text, similarityScore} */
            List<Map<String, Object>> json = JSON_MAPPER.readValue(response.body(), List.class);
            if (json.isEmpty()) return null;
            /* the JSON response from the API looks like this:
                [
                    { //most relevant mapping
                        "text"              :   "vegetable soup",
                        "similarityScore"   :   0.60
                    }
                    ...                                             //more mappings
                    { //least relevant mapping
                        "text"              : "jason vorhees",
                        "similarityScore"   : 0.00002
                    }
                ]
            */
            /* our results are stored in descending order by similarityValue...
             * so we can just return the keys of the mapping */
            for (Map<String, Object> entry : json) {
                sortedTexts.add((String) entry.get("text"));
            }
        }
        return sortedTexts;
    }

    public static void main(String[] args) {
        var console = System.console();
        if (console == null) {
            System.err.println("No console available");
            System.exit(1);
        }
        System.out.println("Oracle Cloud Infrastructure AI Database...");
        String password = new String(console.readPassword("Admin Password: "));
        String startingUrl = console.readLine("Enter a starting URL from Wikipedia: ");
        String targetUrl = console.readLine("Enter the end-goal URL from Wikipedia: ");
        try {
            String token = getToken(password);
            password = ""; // Clear the password if successful
            var crawler = new SemanticCrawler(startingUrl, targetUrl, token);
            crawler.beginCrawling();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            password = ""; // Clear the password if error
        }
    }
}

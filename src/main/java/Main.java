import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import io.github.cdimascio.dotenv.Dotenv;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
    public static HttpClient   CLIENT       = HttpClient.newHttpClient();
    public static Dotenv       ENV_FILE     = Dotenv.load();
    public static String       AUTH_BASE_URL     = ENV_FILE.get("OML_AUTH_BASE_URL");
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

    /* from a list of hyperlinks, returns the link that is
     * most semantically-similar to the target string, along with its similarity score between 0 and 1
     * returns {link, similarityValue}, where a value of 0 is least similar and 1 is most*/
    public static List<String> getMostRelatedLink(String target, List<String> links, String token) throws IOException, InterruptedException {
        var payloadString = JSON_MAPPER.writeValueAsString(Map.of(
                "language", "AMERICAN",
                "probe", target,
                "sortDirection", "DESC",
                "textList", links
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
        //the json is stored as a list of mappings from String to (String) Object
        List<Map<String, Object>> json = JSON_MAPPER.readValue(response.body(), List.class);
        if (json.isEmpty()) return null;

        /* our results are stored in descending order by similarityValue...
         * so the first mapping {text : similarityValue}
         * should contain the most similar text */
        return List.of(
                json.getFirst().get("text").toString(),
                json.getFirst().get("similarity").toString()
        );
    }

    public static void main(String[] args) {
        var console = System.console();
        if (console == null) {
            System.err.println("No console available");
            System.exit(1);
        }
        System.out.println("Oracle Cloud Infrastructure AI Database...");
        String password = new String(console.readPassword("Admin Password: "));
        try {
            String token = getToken(password);
            List<String> results = getMostRelatedLink("Cookie", List.of("Milk", "Dough", "Chocolate", "Oven"), token);
            System.out.println(results);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            password = ""; //clear the password
        }
    }
}

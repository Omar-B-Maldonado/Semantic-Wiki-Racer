import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface SemanticAPIClient {
    HttpClient   CLIENT       = HttpClient.newHttpClient();
    Dotenv       ENV_FILE     = Dotenv.load();
    String       AUTH_BASE_URL= ENV_FILE.get("OML_AUTH_BASE_URL");
    String       OML_BASE_URL = ENV_FILE.get("OML_SERVICE_BASE_URL");
    ObjectMapper JSON_MAPPER  = new ObjectMapper();
    HttpResponse.BodyHandler<String> STRING_BODYHANDLER = HttpResponse.BodyHandlers.ofString();

    static String getToken(String password) throws IOException, InterruptedException {
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

    /**
     * Sorts the given list of strings by semantic similarity to the target string,
     * where the first element is most similar and the last element is the least */
    static List<String> semanticSort(List<String> texts, String target, String token)
            throws IOException, InterruptedException {

        //The API has a max batchsize... for simplicity, we'll only send what can fit in a batch
        final int MAX_BATCH_SIZE = 1000;
        int batchSize = Math.min(texts.size(), MAX_BATCH_SIZE);

        List<String> sortedTexts = new ArrayList<>();

        List<String> unsortedBatch = texts.subList(0, batchSize);

        List<Map<String,Object>> sortedBatch = fetchSemanticSimilarities(unsortedBatch, target, token);

        // Extract just the sorted texts (no need for scores)
        for (Map<String, Object> entry : sortedBatch) {
            sortedTexts.add((String) entry.get("text"));
        }
        return sortedTexts;
    }
    /**
     * Sends a batch of text entries to the Oracle API and
     * returns a list of mappings, with each map following
     * this format  {
     *                  "text"              :   "vegetable soup",
     *                  "similarityScore"   :   0.60
     *              }                                                           */
    private static List<Map<String, Object>> fetchSemanticSimilarities(List<String> texts, String target, String token)
            throws IOException, InterruptedException {

        var payloadString = JSON_MAPPER.writeValueAsString(Map.of(
                "language", "AMERICAN",
                "probe", target,
                "sortDirection", "DESC",
                "textList", texts
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
        List<Map<String, Object>> jsonResponse = JSON_MAPPER.readValue(response.body(), List.class);
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            throw new RuntimeException("Empty or invalid JSON response from API");
        }
        return jsonResponse;
    }
}


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 4.3: the same rules, two error styles, one Java result type.
 *
 *   java -cp "$(lib)" ReviewClient.java
 */
public class ReviewClient {
    static final String URL = System.getenv().getOrDefault("SHELFIE_URL", "http://shelfie-ref:8080") + "/graphql";
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static final JsonMapper JSON = JsonMapper.builder().build();

    sealed interface CreateReviewResult permits ReviewCreated, ValidationFailed, NotAllowed {}
    record ReviewCreated(String id) implements CreateReviewResult {}
    /** Offending input fields as dotted paths, e.g. input.rating, in the server's order. */
    record ValidationFailed(List<String> fields) implements CreateReviewResult {}
    record NotAllowed(String reason) implements CreateReviewResult {}

    record Input(String bookId, int rating, String body) {}

    static JsonNode post(String user, String document, Input in) throws Exception {
        String body = JSON.writeValueAsString(Map.of("query", document, "variables", Map.of("input", in)));
        var b = HttpRequest.newBuilder(URI.create(URL)).header("Content-Type", "application/json");
        if (user != null) b.header("X-Shelfie-User", user);
        JsonNode res = JSON.readTree(HTTP.send(b.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString()).body());
        if (res.has("errors")) throw new IllegalStateException("request failed: " + res.get("errors"));
        return res.get("data");
    }

    static String dotted(JsonNode field) {
        List<String> parts = new ArrayList<>();
        field.forEach(p -> parts.add(p.asString()));
        return String.join(".", parts);
    }

    // ---- style 1: payload with userErrors ------------------------------------------------------
    static CreateReviewResult viaPayload(String user, Input in) throws Exception {
        JsonNode p = post(user, """
                mutation($input: CreateReviewInput!) {
                  createReview(input: $input) { review { id } userErrors { field message code } }
                }""", in).get("createReview");
        JsonNode errors = p.get("userErrors");
        if (errors.isEmpty()) return new ReviewCreated(p.at("/review/id").asString());
        if (errors.get(0).get("code").asString().equals("UNAUTHENTICATED")) return new NotAllowed(errors.get(0).get("message").asString());
        List<String> fields = new ArrayList<>();
        errors.forEach(e -> fields.add(dotted(e.get("field"))));
        return new ValidationFailed(fields);
    }

    // ---- style 2: result union ------------------------------------------------------------------
    static CreateReviewResult viaUnion(String user, Input in) throws Exception {
        JsonNode r = post(user, """
                mutation($input: CreateReviewInput!) {
                  createReviewTyped(input: $input) {
                    __typename
                    ... on ReviewCreated { review { id } }
                    ... on ValidationFailed { problems { field message } }
                    ... on NotAllowed { reason }
                  }
                }""", in).get("createReviewTyped");
        return switch (r.get("__typename").asString()) {
            case "ReviewCreated" -> new ReviewCreated(r.at("/review/id").asString());
            case "NotAllowed" -> new NotAllowed(r.get("reason").asString());
            case "ValidationFailed" -> {
                List<String> fields = new ArrayList<>();
                r.get("problems").forEach(p -> fields.add(dotted(p.get("field"))));
                yield new ValidationFailed(fields);
            }
            default -> throw new IllegalStateException("unknown result type " + r.get("__typename") + ": update the client");
        };
    }

    static String show(CreateReviewResult r) {
        return switch (r) {
            case ReviewCreated c -> "created " + c.id();
            case ValidationFailed v -> "invalid " + String.join(",", v.fields());
            case NotAllowed n -> "not allowed";
        };
    }

    public static void main(String[] args) throws Exception {
        record Case(String name, String payloadUser, String unionUser, Input input) {}
        List<Case> cases = List.of(
                new Case("ok", "u4", "u8", new Input("b10", 4, "Quiet, strange and kind.")),
                new Case("invalid", "u4", "u4", new Input("zz", 9, "short")),
                new Case("anonymous", null, null, new Input("b2", 5, "Long enough to count.")),
                new Case("duplicate", "u1", "u1", new Input("b1", 3, "Second thoughts, longer.")));
        for (Case c : cases) {
            System.out.println(c.name() + " payload " + show(viaPayload(c.payloadUser(), c.input())));
            System.out.println(c.name() + " union   " + show(viaUnion(c.unionUser(), c.input())));
        }
    }
}

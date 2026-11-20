import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lab 4.6: upload a book cover two ways, without ever holding the file in this JVM's heap.
 *
 *   java -cp "$(lib)" UploadCover.java multipart <bookId> <file>
 *   java -cp "$(lib)" UploadCover.java presigned <bookId> <file>
 * Prints: <mode> bytes=<n> sha256=<hex> serverBufferedBytes=<n>
 */
public class UploadCover {
    static final String URL = System.getenv().getOrDefault("SHELFIE_URL", "http://shelfie-ref:8080") + "/graphql";
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final String UPLOAD = "mutation($book: ID!, $file: Upload!) { uploadCover(bookId: $book, file: $file) { bytes sha256 serverBufferedBytes userErrors { code message } } }";

    /**
     * The GraphQL multipart request convention: operations, map, then the file.
     * TODO 1: this draft reads the whole file and builds the whole body in memory. With -Xmx48m and a
     * 50 MB cover it dies. Stream it instead (BodyPublishers can concatenate). And it forgets the
     * CSRF header the server requires (README).
     */
    static JsonNode multipart(String bookId, Path file) throws Exception {
        String boundary = "shelfie-" + UUID.randomUUID();
        String operations = JSON.writeValueAsString(Map.of("query", UPLOAD, "variables", Map.of("book", bookId, "file", JSON.nullNode())));
        var body = new java.io.ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"operations\"\r\n\r\n" + operations + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"map\"\r\n\r\n" + "{\"0\": [\"variables.file\"]}" + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"0\"; filename=\"" + file.getFileName() + "\"\r\n"
                + "Content-Type: image/jpeg\r\n\r\n").getBytes());
        body.write(Files.readAllBytes(file));
        body.write(("\r\n--" + boundary + "--\r\n").getBytes());
        HttpRequest req = HttpRequest.newBuilder(URI.create(URL))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("X-Shelfie-User", "u1")
                .POST(BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        return payload(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body(), "uploadCover");
    }

    /**
     * TODO 2: the signed-URL alternative. createCoverUpload, PUT the file to the uploadUrl with the
     * same Content-Type, then confirmCoverUpload. Stream the PUT body too.
     */
    static JsonNode presigned(String bookId, Path file) throws Exception {
        throw new UnsupportedOperationException("TODO");
    }

    static JsonNode graphql(String query, Map<String, Object> vars, String field) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(URL)).header("Content-Type", "application/json").header("X-Shelfie-User", "u1")
                .POST(BodyPublishers.ofString(JSON.writeValueAsString(Map.of("query", query, "variables", vars)))).build();
        return payload(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body(), field);
    }

    static JsonNode payload(String body, String field) {
        JsonNode res = JSON.readTree(body);
        if (res.has("errors")) throw new IllegalStateException(res.get("errors").toString());
        JsonNode p = res.at("/data/" + field);
        if (!p.get("userErrors").isEmpty()) throw new IllegalStateException(p.get("userErrors").toString());
        return p;
    }

    public static void main(String[] args) throws Exception {
        Path file = Path.of(args[2]);
        JsonNode p = switch (args[0]) {
            case "multipart" -> multipart(args[1], file);
            case "presigned" -> presigned(args[1], file);
            default -> throw new IllegalArgumentException("mode is multipart or presigned");
        };
        System.out.println(args[0] + " bytes=" + p.get("bytes").asLong() + " sha256=" + p.get("sha256").asString()
                + " serverBufferedBytes=" + p.get("serverBufferedBytes").asLong());
    }
}

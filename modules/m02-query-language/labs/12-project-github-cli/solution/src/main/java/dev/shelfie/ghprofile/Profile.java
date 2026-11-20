package dev.shelfie.ghprofile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Lab 2.12 reference: a GitHub profile card from exactly one GraphQL request. */
@Command(name = "gh-profile", mixinStandardHelpOptions = true, version = "gh-profile 1",
        description = "Print a GitHub user's profile card, fetched with one GraphQL query.",
        exitCodeListHeading = "%nExit codes:%n",
        exitCodeList = {"0:card printed", "2:GITHUB_TOKEN not set", "3:no such user", "4:HTTP or GraphQL error"})
public class Profile implements Callable<Integer> {

    @Parameters(index = "0", description = "GitHub login, e.g. torvalds")
    String login;

    @Option(names = "--pinned", defaultValue = "6", description = "Pinned repositories to show (default: ${DEFAULT-VALUE})")
    int pinned;

    @Option(names = "--endpoint", defaultValue = "https://api.github.com/graphql", description = "GraphQL endpoint (default: ${DEFAULT-VALUE})")
    URI endpoint;

    private final JsonMapper json = JsonMapper.builder().build();

    public static void main(String[] args) {
        System.exit(new CommandLine(new Profile()).execute(args));
    }

    @Override
    public Integer call() throws Exception {
        String token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.isBlank()) {
            System.err.println("GITHUB_TOKEN is not set: create a token (SETUP.md) and export it.");
            return 2;
        }
        String body = json.writeValueAsString(Map.of(
                "query", resource("/profile.graphql"),
                "operationName", "Profile",
                "variables", Map.of("login", login, "pinned", pinned)));
        var request = HttpRequest.newBuilder(endpoint)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("User-Agent", "gh-profile-course-lab")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.err.println("HTTP " + response.statusCode() + " from " + endpoint);
            return 4;
        }
        JsonNode root = json.readTree(response.body());
        JsonNode user = root.at("/data/user");
        // GitHub reports an unknown login as data.user = null plus an error of type NOT_FOUND.
        if (user.isMissingNode() || user.isNull()) {
            if (root.has("errors") && !notFoundOnly(root.get("errors"))) {
                root.get("errors").forEach(e -> System.err.println("error: " + e.path("message").asString()));
                return 4;
            }
            System.err.println("no GitHub user " + login);
            return 3;
        }
        System.out.print(card(user));
        return 0;
    }

    static boolean notFoundOnly(JsonNode errors) {
        for (JsonNode e : errors) if (!"NOT_FOUND".equals(e.path("type").asString(""))) return false;
        return true;
    }

    static String card(JsonNode u) {
        StringBuilder out = new StringBuilder();
        String name = u.path("name").asString("");
        out.append(name.isBlank() ? "@" + u.get("login").asString() : name + " (@" + u.get("login").asString() + ")").append('\n');

        List<String> where = new ArrayList<>();
        for (String f : List.of("location", "company")) {
            String v = u.path(f).asString("");
            if (!v.isBlank()) where.add(v);
        }
        if (!where.isEmpty()) out.append(String.join(" · ", where)).append('\n');

        out.append("followers ").append(u.at("/followers/totalCount").asInt())
                .append(" · following ").append(u.at("/following/totalCount").asInt())
                .append(" · public repos ").append(u.at("/repositories/totalCount").asInt())
                .append(" · contributions ").append(u.at("/contributionsCollection/contributionCalendar/totalContributions").asInt())
                .append(" this year\n");

        JsonNode pins = u.at("/pinnedItems/nodes");
        if (pins.isArray() && !pins.isEmpty()) {
            out.append("pinned\n");
            for (JsonNode r : pins) {
                String lang = r.path("primaryLanguage").isNull() ? "-" : r.at("/primaryLanguage/name").asString("-");
                out.append(String.format("  %-28s ★ %-7d %s%n", r.get("name").asString(), r.get("stargazerCount").asInt(), lang));
            }
        }
        return out.toString();
    }

    static String resource(String path) throws IOException {
        try (InputStream in = Profile.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("missing resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

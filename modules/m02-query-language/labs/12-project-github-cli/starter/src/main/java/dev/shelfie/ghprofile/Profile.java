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

/** Lab 2.12: a GitHub profile card from exactly one GraphQL request. See the README for the card format. */
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
        // TODO 1: no GITHUB_TOKEN -> message on stderr, return 2, and send nothing.
        // TODO 2: POST {query, operationName, variables: {login, pinned}} to `endpoint`
        //         with "Authorization: Bearer <token>". Read the document with resource("/profile.graphql").
        // TODO 3: data.user null -> "no GitHub user <login>" on stderr, return 3 (GitHub adds a NOT_FOUND error).
        //         Any other GraphQL error without a user, or an HTTP status other than 200 -> return 4.
        // TODO 4: print card(user) and return 0.
        return 1;
    }

    static String card(JsonNode u) {
        // TODO 5: build the card exactly as the README shows.
        return u.path("login").asString() + "\n";
    }

    static String resource(String path) throws IOException {
        try (InputStream in = Profile.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("missing resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

package dev.shelfie.gateway;

import dev.shelfie.gateway.OpenLibrary.Edition;
import dev.shelfie.gateway.OpenLibrary.Search;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/** One Open Library call per field, every time. Correct on a good day. */
@Controller
public class GatewayController {
    private final OpenLibrary openLibrary;

    GatewayController(OpenLibrary openLibrary) {
        this.openLibrary = openLibrary;
    }

    @QueryMapping public Edition book(@Argument String isbn) { return openLibrary.edition(isbn); }
    @QueryMapping public List<Edition> books(@Argument List<String> isbns) { return isbns.stream().map(openLibrary::edition).toList(); }
    @QueryMapping public Search search(@Argument String text, @Argument int first) { return openLibrary.search(text, first); }

    @SchemaMapping public List<String> subjects(Edition e, @Argument int first) { return e.subjects().stream().limit(Math.max(0, first)).toList(); }
    @SchemaMapping public String fetchedAt(Edition e) { return e.fetchedAt().toString(); }
}

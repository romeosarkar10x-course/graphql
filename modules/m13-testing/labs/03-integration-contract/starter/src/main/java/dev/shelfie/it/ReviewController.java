package dev.shelfie.it;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import tools.jackson.databind.json.JsonMapper;

@Controller
public class ReviewController {

    public record CreateReviewInput(String bookId, int rating, String body) {}

    static class Rejected extends RuntimeException {
        final ErrorType type;
        Rejected(ErrorType type, String message) { super(message); this.type = type; }
    }

    private final ReviewStore store;
    private final KafkaTemplate<String, String> kafka;
    private final String topic;
    private final JsonMapper json;

    public ReviewController(ReviewStore store, KafkaTemplate<String, String> kafka, JsonMapper json,
                            @Value("${shelfie.reviews-topic}") String topic) {
        this.store = store;
        this.kafka = kafka;
        this.json = json;
        this.topic = topic;
    }

    @QueryMapping public ReviewStore.Book book(@Argument String id) { return store.book(id).orElse(null); }
    @SchemaMapping public Double averageRating(ReviewStore.Book book) { return store.average(book.id()); }
    @SchemaMapping public List<ReviewStore.Review> reviews(ReviewStore.Book book, @Argument int last) { return store.newestFirst(book.id(), last); }

    /** Stores the review, then announces it on Kafka as {"id","bookId","author","rating"}, keyed by book. */
    @MutationMapping
    public ReviewStore.Review createReview(@Argument CreateReviewInput input, @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) throw new Rejected(ErrorType.UNAUTHORIZED, "Send a bearer token from Keycloak");
        String author = jwt.getClaimAsString("preferred_username");
        if (store.book(input.bookId()).isEmpty()) throw new Rejected(ErrorType.NOT_FOUND, "No book " + input.bookId());
        if (input.rating() < 1 || input.rating() > 5) throw new Rejected(ErrorType.BAD_REQUEST, "Rating must be 1 to 5");
        ReviewStore.Review r;
        try { r = store.add(input.bookId(), author, input.rating(), input.body().strip()); }
        catch (DuplicateKeyException e) { throw new Rejected(ErrorType.BAD_REQUEST, "You already reviewed this book"); }
        kafka.send(topic, input.bookId(),
            json.writeValueAsString(Map.of("id", r.id(), "bookId", input.bookId(), "author", author, "rating", r.rating())));
        return r;
    }

    @GraphQlExceptionHandler
    GraphQLError rejected(Rejected e, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env).errorType(e.type).message(e.getMessage()).build();
    }
}

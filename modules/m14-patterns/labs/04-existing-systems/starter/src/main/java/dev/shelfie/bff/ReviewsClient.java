package dev.shelfie.bff;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The reviews gRPC service (shelfie.v1.ShelfieService at shelfie.reviews-target). The pom already generates its
 * Java stubs from src/main/proto/shelfie.proto into dev.shelfie.bff.grpc.v1: run mvn compile and look.
 * TODO a channel, a blocking stub, a deadline on every call: ListBookReviews and GetUser.
 */
@Component
public class ReviewsClient {
    public record ReviewData(String id, int rating, String body, String authorId) {}
    public record UserData(String id, String name) {}

    ReviewsClient(@Value("${shelfie.reviews-target}") String target) {
    }

    public List<ReviewData> reviews(String bookId) {
        return List.of();
    }

    public UserData user(String id) {
        return new UserData(id, "?");
    }
}

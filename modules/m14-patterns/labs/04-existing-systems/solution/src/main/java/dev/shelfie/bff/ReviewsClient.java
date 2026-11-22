package dev.shelfie.bff;

import dev.shelfie.bff.grpc.v1.GetByIdRequest;
import dev.shelfie.bff.grpc.v1.Review;
import dev.shelfie.bff.grpc.v1.ShelfieServiceGrpc;
import dev.shelfie.bff.grpc.v1.User;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** The reviews gRPC service, through stubs generated from its .proto. Every call has a deadline. */
@Component
public class ReviewsClient {
    private final ManagedChannel channel;
    private final ShelfieServiceGrpc.ShelfieServiceBlockingStub stub;

    ReviewsClient(@Value("${shelfie.reviews-target}") String target) {
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = ShelfieServiceGrpc.newBlockingStub(channel);
    }

    public List<Review> reviews(String bookId) {
        return stub.withDeadlineAfter(2, TimeUnit.SECONDS).listBookReviews(GetByIdRequest.newBuilder().setId(bookId).build()).getReviewsList();
    }

    public User user(String id) {
        return stub.withDeadlineAfter(2, TimeUnit.SECONDS).getUser(GetByIdRequest.newBuilder().setId(id).build());
    }

    @PreDestroy
    void close() {
        channel.shutdownNow();
    }
}

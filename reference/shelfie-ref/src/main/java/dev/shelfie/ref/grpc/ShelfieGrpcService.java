package dev.shelfie.ref.grpc;

import dev.shelfie.ref.data.Model;
import dev.shelfie.ref.data.Store;
import dev.shelfie.ref.grpc.v1.Book;
import dev.shelfie.ref.grpc.v1.Club;
import dev.shelfie.ref.grpc.v1.ClubPage;
import dev.shelfie.ref.grpc.v1.GetByIdRequest;
import dev.shelfie.ref.grpc.v1.GetClubPageRequest;
import dev.shelfie.ref.grpc.v1.ListReviewsResponse;
import dev.shelfie.ref.grpc.v1.PageReview;
import dev.shelfie.ref.grpc.v1.Review;
import dev.shelfie.ref.grpc.v1.ShelfieServiceGrpc;
import dev.shelfie.ref.grpc.v1.User;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class ShelfieGrpcService extends ShelfieServiceGrpc.ShelfieServiceImplBase {

    private final Store store;

    public ShelfieGrpcService(Store store) {
        this.store = store;
    }

    @Override
    public void getClub(GetByIdRequest req, StreamObserver<Club> out) {
        reply(out, store.club(req.getId()), "club " + req.getId(), c -> Club.newBuilder()
                .setId(c.id()).setName(c.name()).setDescription(c.description())
                .setOwnerId(c.ownerId()).addAllMemberIds(c.memberIds())
                .setCurrentBookId(c.currentBookId()).setCreatedAt(c.createdAt()).build());
    }

    @Override
    public void getUser(GetByIdRequest req, StreamObserver<User> out) {
        reply(out, store.user(req.getId()), "user " + req.getId(), ShelfieGrpcService::toUser);
    }

    @Override
    public void getBook(GetByIdRequest req, StreamObserver<Book> out) {
        reply(out, store.book(req.getId()), "book " + req.getId(), b -> Book.newBuilder()
                .setId(b.id()).setTitle(b.title()).addAllAuthors(b.authors())
                .setPublishedYear(b.publishedYear()).build());
    }

    @Override
    public void listBookReviews(GetByIdRequest req, StreamObserver<ListReviewsResponse> out) {
        reply(out, store.book(req.getId()), "book " + req.getId(), b -> {
            var resp = ListReviewsResponse.newBuilder();
            for (Model.Review r : store.reviewsForBook(b.id())) {
                resp.addReviews(Review.newBuilder().setId(r.id()).setAuthorId(r.authorId())
                        .setRating(r.rating()).setBody(r.body()).setCreatedAt(r.createdAt()));
            }
            return resp.build();
        });
    }

    @Override
    public void getClubPage(GetClubPageRequest req, StreamObserver<ClubPage> out) {
        int memberLimit = req.getMemberLimit() > 0 ? req.getMemberLimit() : 5;
        int reviewLimit = req.getReviewLimit() > 0 ? req.getReviewLimit() : 3;
        reply(out, store.club(req.getClubId()), "club " + req.getClubId(), c -> {
            var page = ClubPage.newBuilder()
                    .setName(c.name()).setDescription(c.description())
                    .setOwnerName(store.user(c.ownerId()).orElseThrow().name())
                    .setMemberCount(c.memberIds().size());
            c.memberIds().stream().limit(memberLimit).forEach(id -> page.addMemberNames(store.user(id).orElseThrow().name()));
            store.book(c.currentBookId()).ifPresent(b -> {
                page.setCurrentBookTitle(b.title()).addAllCurrentBookAuthors(b.authors());
                store.reviewsForBook(b.id()).stream().limit(reviewLimit).forEach(r -> page.addLatestReviews(
                        PageReview.newBuilder().setReviewerName(store.user(r.authorId()).orElseThrow().name())
                                .setRating(r.rating()).setBody(r.body())));
            });
            store.meetupsForClub(c.id(), true).stream().findFirst().ifPresent(m ->
                    page.setNextMeetupTitle(m.title()).setNextMeetupStartsAt(m.startsAt()));
            return page.build();
        });
    }

    private static User toUser(Model.User u) {
        return User.newBuilder().setId(u.id()).setName(u.name()).setHandle(u.handle()).setBio(u.bio()).build();
    }

    private static <T, R> void reply(StreamObserver<R> out, Optional<T> found, String what, Function<T, R> map) {
        if (found.isEmpty()) {
            out.onError(Status.NOT_FOUND.withDescription(what + " not found").asRuntimeException());
            return;
        }
        out.onNext(map.apply(found.get()));
        out.onCompleted();
    }
}

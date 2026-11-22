package dev.shelfie.harness;

import dev.shelfie.mini.Resolver;
import dev.shelfie.ref.data.Model.*;
import dev.shelfie.ref.data.Store;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shelfie's read side on any engine: the reference server's own Store (its seed, compiled from
 * reference/shelfie-ref), resolved the way its controllers resolve it. Fields not listed come from record components.
 */
public final class ShelfieResolver implements Resolver {
    private final Store store = new Store();

    @Override
    public Object resolve(String type, String field, Object source, Map<String, Object> args) throws Exception {
        return switch (type + "." + field) {
            case "Query.hello" -> "Hello from Shelfie!";
            case "Query.club" -> store.club((String) args.get("id")).orElse(null);
            case "Query.clubs" -> store.clubs();
            case "Query.user" -> store.user((String) args.get("id")).orElse(null);
            case "Query.book" -> store.book((String) args.get("id")).orElse(null);
            case "Query.books" -> {
                String needle = args.get("titleContains") == null ? "" : ((String) args.get("titleContains")).toLowerCase(Locale.ROOT);
                yield store.books().stream().filter(b -> b.title().toLowerCase(Locale.ROOT).contains(needle))
                        .limit(Math.max(0, (Integer) args.get("first"))).toList();
            }
            case "Query.search" -> {
                String needle = ((String) args.get("text")).toLowerCase(Locale.ROOT);
                List<Object> hits = new ArrayList<>();
                store.books().stream().filter(b -> b.title().toLowerCase(Locale.ROOT).contains(needle)).forEach(hits::add);
                store.clubs().stream().filter(c -> c.name().toLowerCase(Locale.ROOT).contains(needle)).forEach(hits::add);
                store.users().stream().filter(u -> u.name().toLowerCase(Locale.ROOT).contains(needle)
                        || u.handle().toLowerCase(Locale.ROOT).contains(needle)).forEach(hits::add);
                yield hits;
            }
            case "Club.owner" -> store.user(((Club) source).ownerId()).orElseThrow();
            case "Club.members" -> ((Club) source).memberIds().stream().limit(Math.max(0, (Integer) args.get("first")))
                    .map(id -> store.user(id).orElseThrow()).toList();
            case "Club.memberCount" -> ((Club) source).memberIds().size();
            case "Club.currentBook" -> ((Club) source).currentBookId() == null ? null : store.book(((Club) source).currentBookId()).orElse(null);
            case "Club.meetups" -> store.meetupsForClub(((Club) source).id(), (Boolean) args.get("upcomingOnly"));
            case "User.clubs" -> store.clubsOfUser(((User) source).id());
            case "User.reviews" -> store.reviewsByUser(((User) source).id());
            case "Book.averageRating" -> store.averageRating(((Book) source).id());
            case "Book.coverUrl" -> store.coverUrl((Book) source);
            case "Book.reviews" -> store.reviewsForBook(((Book) source).id()).stream().limit(Math.max(0, (Integer) args.get("last"))).toList();
            case "Review.author" -> store.user(((Review) source).authorId()).orElseThrow();
            case "Review.book" -> store.book(((Review) source).bookId()).orElseThrow();
            case "Meetup.club" -> store.club(((Meetup) source).clubId()).orElseThrow();
            default -> Resolver.property(source, field);
        };
    }
}

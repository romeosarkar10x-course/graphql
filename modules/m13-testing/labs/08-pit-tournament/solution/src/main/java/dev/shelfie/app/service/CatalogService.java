package dev.shelfie.app.service;

import dev.shelfie.app.store.CatalogRepository;
import dev.shelfie.app.store.Records.Book;
import dev.shelfie.app.store.Records.Club;
import dev.shelfie.app.store.Records.Meetup;
import dev.shelfie.app.store.Records.User;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Books, clubs and users: lookups, search and list rules. No GraphQL types in here. */
@Service
public class CatalogService {

    private final CatalogRepository catalog;

    public CatalogService(CatalogRepository catalog) { this.catalog = catalog; }

    public Optional<Book> book(String id) { return catalog.book(id); }
    public Optional<Club> club(String id) { return catalog.club(id); }
    public Optional<User> user(String id) { return catalog.user(id); }
    public List<Club> clubs() { return catalog.clubs(); }

    /** Catalogue order. {@code titleContains} matches ignoring case (null matches all); {@code first} caps, below 0 means 0. */
    public List<Book> books(String titleContains, int first) {
        String needle = titleContains == null ? null : titleContains.toLowerCase(Locale.ROOT);
        return catalog.books().stream()
            .filter(b -> needle == null || b.title().toLowerCase(Locale.ROOT).contains(needle))
            .limit(Math.max(0, first))
            .toList();
    }

    public User owner(Club club) { return catalog.user(club.ownerId()).orElseThrow(); }

    /** Members in join order (the owner joined first), capped at {@code first}. */
    public List<User> members(Club club, int first) {
        return club.memberIds().stream().limit(Math.max(0, first)).map(id -> catalog.user(id).orElseThrow()).toList();
    }

    public int memberCount(Club club) { return club.memberIds().size(); }

    public Optional<Book> currentBook(Club club) { return catalog.book(club.currentBookId()); }

    /** In date order; only upcoming ones when {@code upcomingOnly}. */
    public List<Meetup> meetups(Club club, boolean upcomingOnly) {
        return catalog.meetups().stream()
            .filter(m -> m.clubId().equals(club.id()) && (!upcomingOnly || m.upcoming()))
            .sorted(Comparator.comparing(Meetup::startsAt))
            .toList();
    }

    public List<Club> clubsOf(User user) {
        return catalog.clubs().stream().filter(c -> c.memberIds().contains(user.id())).toList();
    }
}

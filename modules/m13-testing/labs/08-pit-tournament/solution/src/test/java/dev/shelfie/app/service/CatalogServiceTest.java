package dev.shelfie.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shelfie.app.store.CatalogRepository;
import dev.shelfie.app.store.Records.Book;
import dev.shelfie.app.store.Records.Club;
import dev.shelfie.app.store.Records.Meetup;
import dev.shelfie.app.store.Records.User;
import org.junit.jupiter.api.Test;

/** The catalogue rules against the real seed (no mocks needed: the repository is plain data). */
class CatalogServiceTest {

    final CatalogService service = new CatalogService(new CatalogRepository());
    final Club c1 = service.club("c1").orElseThrow();

    @Test
    void titleSearchIgnoresCaseAndKeepsCatalogueOrder() {
        assertThat(service.books("THE", 20)).extracting(Book::id).containsExactly("b1", "b3", "b4", "b7", "b9", "b10");
        assertThat(service.books(null, 20)).hasSize(10);
        assertThat(service.books("zzz", 20)).isEmpty();
    }

    @Test
    void firstCapsTheBookListAndNegativeMeansNone() {
        assertThat(service.books("the", 2)).extracting(Book::id).containsExactly("b1", "b3");
        assertThat(service.books(null, -1)).isEmpty();
    }

    @Test
    void membersAreInJoinOrderAndCapped() {
        assertThat(service.members(c1, 3)).extracting(User::handle).containsExactly("kemia", "asha", "benok");
        assertThat(service.members(c1, 10)).hasSize(8);
        assertThat(service.memberCount(c1)).isEqualTo(8);
    }

    @Test
    void upcomingOnlyHidesPastMeetupsAndOrderIsByDate() {
        assertThat(service.meetups(c1, true)).extracting(Meetup::id).containsExactly("m2", "m3");
        assertThat(service.meetups(c1, false)).extracting(Meetup::id).containsExactly("m1", "m2", "m3");
    }

    @Test
    void ownerAndClubsOfAUser() {
        assertThat(service.owner(c1).name()).isEqualTo("Kemi Adeyemi");
        assertThat(service.clubsOf(service.user("u1").orElseThrow())).extracting(Club::id).containsExactly("c1", "c3");
        assertThat(service.currentBook(c1)).map(Book::title).contains("The Left Hand of Darkness");
    }
}

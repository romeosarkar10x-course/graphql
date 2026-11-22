package dev.shelfie.locales;

import dev.shelfie.locales.Data.Book;
import dev.shelfie.locales.Data.Meetup;
import dev.shelfie.locales.Data.Price;
import graphql.scalars.ExtendedScalars;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.stereotype.Controller;

/**
 * Works for an English reader in London. TODO, per the schema's descriptions:
 *  - language: every Accept-Language range in order, falling back from region to language, then to English;
 *    formats in the reader's locale (Spring's Locale argument is only the first range, or the JVM's default);
 *  - time: the timeZone arguments (instants stay UTC), unknown zones as BAD_REQUEST errors;
 *  - money: exact amounts with the currency's decimals, formatted by locale, setPrice validated.
 */
@Controller
public class ShelfieController {
    public record Money(String amount, String currency, String formatted) {}
    public record MoneyInput(String amount, String currency) {}
    public record UserError(List<String> field, String message) {}
    public record SetPricePayload(Book book, List<UserError> userErrors) {}

    @Bean
    static RuntimeWiringConfigurer scalars() {
        return wiring -> wiring.scalar(ExtendedScalars.DateTime).scalar(ExtendedScalars.Date);
    }

    @QueryMapping public Book book(@Argument String id) { return Data.BOOKS.get(id); }
    @QueryMapping public Meetup meetup(@Argument String id) {
        return Data.MEETUPS.stream().filter(m -> m.id().equals(id)).findFirst().orElse(null);
    }
    @QueryMapping public List<Meetup> meetups() { return Data.MEETUPS; }

    @SchemaMapping public String blurb(Book book, Locale locale) {
        return book.blurbs().getOrDefault(locale.getLanguage(), book.blurbs().get("en"));
    }
    @SchemaMapping public String blurbLanguage(Book book, Locale locale) {
        return book.blurbs().containsKey(locale.getLanguage()) ? locale.getLanguage() : "en";
    }

    @SchemaMapping public Money price(Book book) {
        Price p = Data.PRICES.get(book.id());
        return new Money(p.amount().toString(), p.currency(), String.format("%.2f %s", p.amount(), p.currency()));
    }

    @SchemaMapping public String pagesText(Book book) { return String.valueOf(book.pages()); }

    @SchemaMapping public Book book(Meetup meetup) { return Data.BOOKS.get(meetup.bookId()); }

    @SchemaMapping public OffsetDateTime startsAt(Meetup meetup, @Argument String timeZone) {
        return meetup.startsAt().atOffset(ZoneOffset.UTC);
    }
    @SchemaMapping public OffsetDateTime endsAt(Meetup meetup, @Argument String timeZone) {
        return meetup.startsAt().atOffset(ZoneOffset.UTC).plusMinutes(meetup.durationMinutes());
    }
    @SchemaMapping public LocalDate localDate(Meetup meetup, @Argument String timeZone) {
        return meetup.startsAt().atOffset(ZoneOffset.UTC).toLocalDate();
    }
    @SchemaMapping public String startsAtText(Meetup meetup, @Argument String timeZone) {
        return meetup.startsAt().toString();
    }

    @MutationMapping public SetPricePayload setPrice(@Argument String bookId, @Argument MoneyInput price) {
        Data.PRICES.put(bookId, new Price(new BigDecimal(price.amount()), price.currency()));
        return new SetPricePayload(Data.BOOKS.get(bookId), List.of());
    }
}

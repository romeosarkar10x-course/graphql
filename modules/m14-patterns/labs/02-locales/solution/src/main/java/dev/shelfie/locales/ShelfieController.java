package dev.shelfie.locales;

import dev.shelfie.locales.Data.Book;
import dev.shelfie.locales.Data.Meetup;
import dev.shelfie.locales.Data.Price;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetchingEnvironment;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.stereotype.Controller;

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
    @QueryMapping public List<Meetup> meetups() {
        return Data.MEETUPS.stream().sorted((a, b) -> a.startsAt().compareTo(b.startsAt())).toList();
    }

    // Language: content in the best language Shelfie has; formats in the most preferred locale.

    @SchemaMapping public String blurb(Book book, @ContextValue List<Locale.LanguageRange> languages) {
        return book.blurbs().get(language(book, languages));
    }

    @SchemaMapping public String blurbLanguage(Book book, @ContextValue List<Locale.LanguageRange> languages) {
        return language(book, languages);
    }

    static String language(Book book, List<Locale.LanguageRange> languages) {
        String best = Locale.lookupTag(languages, book.blurbs().keySet());   // RFC 4647 lookup: fr-CH falls back to fr
        return best != null ? best : "en";
    }

    /** The locale numbers and dates are written in: the first range the client named. */
    static Locale formatting(List<Locale.LanguageRange> languages) {
        return languages.stream().map(Locale.LanguageRange::getRange).filter(r -> !r.equals("*"))
            .map(Locale::forLanguageTag).findFirst().orElse(Locale.ENGLISH);
    }

    @SchemaMapping public Money price(Book book, @ContextValue List<Locale.LanguageRange> languages) {
        Price p = Data.PRICES.get(book.id());
        Currency currency = Currency.getInstance(p.currency());
        NumberFormat format = NumberFormat.getCurrencyInstance(formatting(languages));
        format.setCurrency(currency);
        format.setMinimumFractionDigits(currency.getDefaultFractionDigits());
        format.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        return new Money(p.amount().setScale(currency.getDefaultFractionDigits()).toPlainString(), p.currency(), format.format(p.amount()));
    }

    @SchemaMapping public String pagesText(Book book, @ContextValue List<Locale.LanguageRange> languages) {
        return NumberFormat.getIntegerInstance(formatting(languages)).format(book.pages());
    }

    // Time: stored as Instant (UTC); a zone only changes how it is written.

    @SchemaMapping public Book book(Meetup meetup) { return Data.BOOKS.get(meetup.bookId()); }

    @SchemaMapping public OffsetDateTime startsAt(Meetup meetup, @Argument String timeZone) {
        return meetup.startsAt().atZone(zone(timeZone)).toOffsetDateTime();
    }

    @SchemaMapping public OffsetDateTime endsAt(Meetup meetup, @Argument String timeZone) {
        return meetup.startsAt().plusSeconds(60L * meetup.durationMinutes()).atZone(zone(timeZone)).toOffsetDateTime();
    }

    @SchemaMapping public LocalDate localDate(Meetup meetup, @Argument String timeZone) {
        return meetup.startsAt().atZone(zone(timeZone)).toLocalDate();
    }

    @SchemaMapping public String startsAtText(Meetup meetup, @Argument String timeZone, @ContextValue List<Locale.LanguageRange> languages) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT).withLocale(formatting(languages))
            .format(meetup.startsAt().atZone(zone(timeZone)));
    }

    static ZoneId zone(String id) {
        try {
            return ZoneId.of(id);
        } catch (DateTimeException e) {
            throw new UnknownZone(id);
        }
    }

    static final class UnknownZone extends RuntimeException {
        UnknownZone(String id) { super("Unknown time zone '" + id + "': use an IANA zone id such as Europe/Berlin"); }
    }

    @GraphQlExceptionHandler
    GraphQLError unknownZone(UnknownZone e, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env).errorType(ErrorType.BAD_REQUEST).message(e.getMessage()).build();
    }

    // Money in: exact, in the currency's decimals, positive.

    @MutationMapping public SetPricePayload setPrice(@Argument String bookId, @Argument MoneyInput price) {
        Book book = Data.BOOKS.get(bookId);
        List<UserError> errors = new ArrayList<>();
        if (book == null) errors.add(new UserError(List.of("bookId"), "No book " + bookId));
        Currency currency = null;
        try {
            currency = Currency.getInstance(price.currency());
        } catch (IllegalArgumentException e) {
            errors.add(new UserError(List.of("price", "currency"), "Not an ISO 4217 currency code: " + price.currency()));
        }
        BigDecimal amount = null;
        if (!price.amount().matches("\\d+(\\.\\d+)?")) {
            errors.add(new UserError(List.of("price", "amount"), "Write the amount as digits with an optional decimal point, e.g. 12.50"));
        } else {
            amount = new BigDecimal(price.amount());
            if (amount.signum() <= 0) errors.add(new UserError(List.of("price", "amount"), "The amount must be more than zero"));
            else if (currency != null && amount.stripTrailingZeros().scale() > currency.getDefaultFractionDigits())
                errors.add(new UserError(List.of("price", "amount"), currency.getCurrencyCode() + " has "
                    + currency.getDefaultFractionDigits() + " decimals; " + price.amount() + " has more"));
        }
        if (!errors.isEmpty()) return new SetPricePayload(book, errors);
        Data.PRICES.put(bookId, new Price(amount, currency.getCurrencyCode()));
        return new SetPricePayload(book, List.of());
    }
}

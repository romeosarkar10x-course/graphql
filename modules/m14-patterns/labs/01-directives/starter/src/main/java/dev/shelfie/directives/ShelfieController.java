package dev.shelfie.directives;

import dev.shelfie.directives.Data.Book;
import dev.shelfie.directives.Data.User;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/** Resolvers return raw values only: EUR amounts, whole emails. The schema's directives do the rest. */
@Controller
public class ShelfieController {
    public record Experiment() {}

    @QueryMapping public Book book(@Argument String id) { return Data.BOOK.get(id); }
    @QueryMapping public List<Book> books() { return Data.BOOKS; }
    @QueryMapping public User user(@Argument String id) { return Data.USER.get(id); }
    @QueryMapping public Experiment experiment() { return new Experiment(); }

    @SchemaMapping(typeName = "Book") public BigDecimal priceUsd(Book book) { return book.priceEur(); }
    @SchemaMapping(typeName = "Book") public BigDecimal priceGbp(Book book) { return book.priceEur(); }
    @SchemaMapping(typeName = "Book") public BigDecimal priceJpy(Book book) { return book.priceEur(); }

    @SchemaMapping(typeName = "Experiment") public BigDecimal currencyThenMask(Experiment e) { return Data.BOOK.get("b2").priceEur(); }
    @SchemaMapping(typeName = "Experiment") public BigDecimal maskThenCurrency(Experiment e) { return Data.BOOK.get("b2").priceEur(); }
}

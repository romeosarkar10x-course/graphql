package dev.shelfie.boot;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * TODO: port every fetcher of your Module 5 server here. Root fields: @QueryMapping / @MutationMapping;
 * fields of Club, User, Book, Review and Meetup: @SchemaMapping, with the parent as the first parameter.
 * Fields whose name matches a record component (Club.name, Book.title, ...) need nothing.
 */
@Controller
public class ShelfieController {
    @QueryMapping public String hello() { return "Hello from Shelfie"; }
}

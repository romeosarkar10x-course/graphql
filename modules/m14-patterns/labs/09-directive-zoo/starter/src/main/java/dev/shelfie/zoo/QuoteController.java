package dev.shelfie.zoo;

import java.util.List;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * Every directive-carrying field returns the plain line: the directives do the rest, once they are wired.
 * TODO a RuntimeWiringConfigurer with a SchemaDirectiveWiring per directive (lab 14.1 has the pattern).
 */
@Controller
public class QuoteController {
    public record Quote(String id, String text, String ending, String blame) {}

    static final List<Quote> QUOTES = List.of(
        new Quote("q1", "Hello my friend, the sea is calm tonight.", "Genly goes home alone.", "Estraven"),
        new Quote("q2", "Light is the left hand of darkness, and darkness the right hand of light.", "Spring comes to Gethen.", null),
        new Quote("q3", "Are you a Book person? Yes, I love books and the night stars.", null, "The Other"),
        new Quote("q4", "The Beauty of the House is immeasurable; its Kindness infinite.", "Piranesi learns his name.", "Ketterley"),
        new Quote("q5", "Winter is long; the sun is low; my friends are near.", "Klara waits in the yard.", "the Sun"));

    @QueryMapping public List<Quote> quotes() { return QUOTES; }

    @SchemaMapping public String pirate(Quote q) { return q.text(); }
    @SchemaMapping public String chat(Quote q) { return q.text(); }
    @SchemaMapping public String shanty(Quote q) { return q.text(); }
}

package dev.shelfie.filters;

import java.util.List;

/** The GraphQL input, bound by Spring for GraphQL. Every component may be null. */
public record BookFilter(List<Genre> genres, Integer publishedFrom, Integer publishedTo, Double minRating,
                         String author, String search) {
    public static final BookFilter NONE = new BookFilter(null, null, null, null, null, null);
}

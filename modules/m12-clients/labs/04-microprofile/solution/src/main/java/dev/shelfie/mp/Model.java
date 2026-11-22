package dev.shelfie.mp;

import java.util.List;

/**
 * Result types. The typesafe client also derives its selection set from them: every field of the record
 * is selected, recursively, so keep them as small as the screen.
 */
public final class Model {
  private Model() {}

  public record Owner(String name) {}
  public record Author(String name) {}
  public record Review(int rating, String body, Author author) {}
  public record Book(String title, List<String> authors, Integer publishedYear, List<Review> reviews) {}
  public record Club(String name, int memberCount, Owner owner, Book currentBook) {}
}

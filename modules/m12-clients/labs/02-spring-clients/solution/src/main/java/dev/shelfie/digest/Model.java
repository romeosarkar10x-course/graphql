package dev.shelfie.digest;

import java.util.List;

/** The digest's own types: only the fields its documents select, decoded with toEntity. */
public final class Model {
  private Model() {}

  public record Book(String title, List<String> authors) {}
  public record Owner(String name, String handle) {}
  public record ClubDigest(String id, String name, int memberCount, Book currentBook, Owner owner) {}

  public record Author(String id) {}
  public record Review(String id, int rating, String body, Author author) {}
  public record UserError(List<String> field, String message, String code) {}
  public record ReviewPayload(Review review, List<UserError> userErrors) {}
  public record NewReview(int rating, String body) {}

  public record Message(String id, String author, String body) {}
}

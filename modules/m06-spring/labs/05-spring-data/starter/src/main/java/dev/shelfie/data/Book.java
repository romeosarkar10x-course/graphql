package dev.shelfie.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** A JPA entity named like the GraphQL type: @GraphQlRepository matches on the simple name. */
@Entity
public class Book {
    @Id private String id;
    private String title;
    private String author;
    private String isbn;
    private Integer publishedYear;

    protected Book() {}

    public Book(String id, String title, String author, String isbn, Integer publishedYear) {
        this.id = id; this.title = title; this.author = author; this.isbn = isbn; this.publishedYear = publishedYear;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public Integer getPublishedYear() { return publishedYear; }
}

package dev.shelfie.connections;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Book {
    @Id private Long id;
    private String title;
    private String author;
    private int published;

    protected Book() {}

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getPublished() { return published; }
}

package dev.shelfie.filters;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.math.BigDecimal;

@Entity
public class Book {
    @Id private Long id;
    private String title;
    private String author;
    @Enumerated(EnumType.STRING) private Genre genre;
    private int published;
    private BigDecimal rating;
    private String blurb;

    protected Book() {}

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public Genre getGenre() { return genre; }
    public int getPublished() { return published; }
    public BigDecimal getRating() { return rating; }
    public String getBlurb() { return blurb; }
}

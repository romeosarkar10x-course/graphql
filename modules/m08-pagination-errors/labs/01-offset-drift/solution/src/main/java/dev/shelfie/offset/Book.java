package dev.shelfie.offset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;

/** m08_01.book from the course catalogue. New rows take their id from the column default (a sequence). */
@Entity
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String title;
    private String author = "Drift Bot";
    private String genre = "MEMOIR";
    private int published = 2026;
    private BigDecimal rating = new BigDecimal("3.0");
    private int pages = 100;
    private String blurb = "Added during the drift experiment.";
    @Column(name = "added_at") private Instant addedAt;

    protected Book() {}

    public Book(String title, Instant addedAt) {
        this.title = title;
        this.addedAt = addedAt;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public Instant getAddedAt() { return addedAt; }
}

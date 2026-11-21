package dev.shelfie.data;

import org.springframework.data.jpa.repository.JpaRepository;

/** TODO: make Spring for GraphQL generate the fetchers for Query.book and Query.books from this repository. */
public interface BookRepository extends JpaRepository<Book, String> {
}

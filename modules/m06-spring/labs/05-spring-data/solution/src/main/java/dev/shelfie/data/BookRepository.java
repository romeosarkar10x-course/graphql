package dev.shelfie.data;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.graphql.data.GraphQlRepository;

/**
 * @GraphQlRepository + QueryByExampleExecutor: Spring for GraphQL registers fetchers for every Query field
 * that returns Book or [Book] and has no other fetcher, binding the arguments into an example Book.
 */
@GraphQlRepository
public interface BookRepository extends JpaRepository<Book, String>, QueryByExampleExecutor<Book> {
    List<Book> findByTitleContainingIgnoreCaseOrderByPublishedYear(String text);
}

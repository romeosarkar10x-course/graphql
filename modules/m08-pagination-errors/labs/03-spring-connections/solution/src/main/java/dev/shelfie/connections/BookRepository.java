package dev.shelfie.connections;

import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Hand-written keyset queries. Spring Data's generated scroll (findBy(ScrollPosition, Limit, Sort))
 * spells "after (p, i)" as `published > ? or published = ? and id > ?`, which PostgreSQL runs as a
 * filter over the index from the start (lab 8.2). HQL row values become one index range.
 */
interface BookRepository extends Repository<Book, Long> {
    @Query("select b from Book b where (b.published, b.id) > (:published, :id) order by b.published, b.id")
    List<Book> after(@Param("published") int published, @Param("id") long id, Limit limit);

    @Query("select b from Book b where (b.published, b.id) < (:published, :id) order by b.published desc, b.id desc")
    List<Book> before(@Param("published") int published, @Param("id") long id, Limit limit);
}

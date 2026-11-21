package dev.shelfie.connections;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.repository.Repository;

/** Spring Data's generated keyset scroll: it writes the WHERE clause from the Sort and the position. */
interface BookRepository extends Repository<Book, Long> {
    Window<Book> findBy(ScrollPosition position, Limit limit, Sort sort);
}

package dev.shelfie.perf;

import dev.shelfie.perf.Entities.Book;
import dev.shelfie.perf.Entities.Club;
import dev.shelfie.perf.Entities.Reader;
import dev.shelfie.perf.Entities.Review;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ClubRepository extends JpaRepository<Club, Long> {
    List<Club> findAllByOrderById(Limit limit);

    @Query(value = "select count(*) from membership where club_id = ?1", nativeQuery = true)
    int countMembers(long clubId);
}

interface ReaderRepository extends JpaRepository<Reader, Long> {
    @Query(value = """
        select r.* from reader r join membership m on m.reader_id = r.id
        where m.club_id = ?1 order by m.joined_at, r.id limit ?2""", nativeQuery = true)
    List<Reader> membersOf(long clubId, int first);
}

interface BookRepository extends JpaRepository<Book, Long> {
    @Query(value = """
        select b.* from review v join book b on b.id = v.book_id
        where v.author_id = ?1 order by v.created_at desc, v.id desc limit ?2""", nativeQuery = true)
    List<Book> pickedBy(long readerId, int first);
}

interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query(value = "select * from review where author_id = ?1 order by created_at desc, id desc limit ?2", nativeQuery = true)
    List<Review> latestBy(long authorId, int first);
}

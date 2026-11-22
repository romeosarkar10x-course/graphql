package dev.shelfie.privacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GDPR erasure (Article 17): everything that identifies the person goes, in one transaction. Their reviews are
 * deleted, soft-deleted ones included (a review is personal data: it may name places and people). The audit log
 * keeps the events but loses the link: the actor becomes "erased". Then the caches forget them.
 */
@Service
public class Erasure {
    public record Erased(String userId, int reviewsDeleted) {}

    private static final Logger log = LoggerFactory.getLogger(Erasure.class);
    private final JdbcClient jdbc;
    private final Users users;

    Erasure(JdbcClient jdbc, Users users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    @Transactional
    public Erased erase(String userId) {
        int reviews = jdbc.sql("DELETE FROM m14_06.reviews WHERE author_id = ?").param(userId).update();
        jdbc.sql("UPDATE m14_06.audit_log SET actor = 'erased' WHERE actor = ?").param(userId).update();
        jdbc.sql("DELETE FROM m14_06.users WHERE id = ?").param(userId).update();
        users.evict(userId);
        log.info("erased user {} and {} reviews", userId, reviews);
        return new Erased(userId, reviews);
    }
}

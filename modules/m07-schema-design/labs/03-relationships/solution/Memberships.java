import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Edge data for lab 7.3: a member's role in a club and when they joined. Derived from Data.CLUBS so it
 * is reproducible: the owner is OWNER and joined when the club was created, the next member is a
 * MODERATOR, the rest are MEMBERs; each member joined 9 days after the one before. Do not edit.
 */
final class Memberships {
    private Memberships() {}

    enum Role { OWNER, MODERATOR, MEMBER }

    record Membership(String clubId, String userId, Role role, String joinedAt) {}

    static final List<Membership> ALL = build();

    private static List<Membership> build() {
        var out = new ArrayList<Membership>();
        for (Data.Club c : Data.CLUBS) {
            Instant start = Instant.parse(c.createdAt());
            for (int i = 0; i < c.memberIds().size(); i++) {
                String u = c.memberIds().get(i);
                Role role = u.equals(c.ownerId()) ? Role.OWNER : i == 1 ? Role.MODERATOR : Role.MEMBER;
                out.add(new Membership(c.id(), u, role, start.plus(9L * i, ChronoUnit.DAYS).toString()));
            }
        }
        return List.copyOf(out);
    }

    /** A club's memberships in join order. */
    static List<Membership> of(String clubId) {
        return ALL.stream().filter(m -> m.clubId().equals(clubId)).toList();
    }

    /** A member's memberships in join order. */
    static List<Membership> ofUser(String userId) {
        return ALL.stream().filter(m -> m.userId().equals(userId)).toList();
    }
}

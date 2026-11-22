package dev.shelfie.fed.clubs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Each club's latest pick: club id to (month, book id, user id). Seeded for c1 and c3; c2 has none. */
final class Picks {
    record Pick(String month, String bookId, String userId) {}

    static final Map<String, Pick> BY_CLUB = new ConcurrentHashMap<>();
    static { reset(); dev.shelfie.fed.Stats.resetHooks.add(Picks::reset); }

    static void reset() {
        BY_CLUB.clear();
        BY_CLUB.put("c1", new Pick("2026-11", "b6", "u11"));
        BY_CLUB.put("c3", new Pick("2026-11", "b9", "u5"));
    }

    private Picks() {}
}

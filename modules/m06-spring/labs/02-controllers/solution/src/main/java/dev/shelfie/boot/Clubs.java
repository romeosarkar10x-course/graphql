package dev.shelfie.boot;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Provided: clubs with edits applied (updateClub). Data.CLUBS is the immutable seed. */
public final class Clubs {
    private Clubs() {}
    private static final Map<String, Data.Club> EDITED = new ConcurrentHashMap<>();

    public static Optional<Data.Club> get(String id) {
        return Optional.ofNullable(EDITED.get(id)).or(() -> Data.club(id));
    }
    public static List<Data.Club> all() { return Data.CLUBS.stream().map(c -> get(c.id()).orElseThrow()).toList(); }
    public static void put(Data.Club club) { EDITED.put(club.id(), club); }
}

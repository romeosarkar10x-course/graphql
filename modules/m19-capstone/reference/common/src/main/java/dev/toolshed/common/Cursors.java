package dev.toolshed.common;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Opaque cursors: the sort key values of the last row, joined and base64url-encoded. Clients must not parse
 * them; the server checks the number of parts so a cursor from another sort order is refused.
 */
public final class Cursors {
    private static final char SEP = '\u001f';

    private Cursors() {}

    public static String encode(String... parts) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(String.join(String.valueOf(SEP), parts).getBytes(StandardCharsets.UTF_8));
    }

    /** @throws BadInput when the cursor is not one this server made with {@code parts} parts */
    public static List<String> decode(String cursor, int parts) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            List<String> values = List.of(raw.split(String.valueOf(SEP), -1));
            if (values.size() != parts) throw new BadInput("after", "this cursor is not valid for this sort order");
            return values;
        } catch (IllegalArgumentException e) {
            throw new BadInput("after", "not a cursor from this server");
        }
    }
}

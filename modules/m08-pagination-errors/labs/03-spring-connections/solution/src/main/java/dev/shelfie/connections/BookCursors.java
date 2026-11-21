package dev.shelfie.connections;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.graphql.data.pagination.CursorStrategy;

/**
 * Cursor: unpadded base64url of "v1:published:id". It does its own base64 (with CursorEncoder.noOpEncoder())
 * so that every malformed cursor ends up as one exception type. Boot's default writes the keyset as JSON with Java
 * class names (K_["java.util.Collections$UnmodifiableMap",...]): readable, and tied to the JVM types.
 */
final class BookCursors implements CursorStrategy<ScrollPosition> {
    @Override
    public boolean supports(Class<?> targetType) { return ScrollPosition.class.isAssignableFrom(targetType); }

    @Override
    public String toCursor(ScrollPosition position) {
        var keys = ((KeysetScrollPosition) position).getKeys();
        var raw = "v1:" + keys.get("published") + ":" + keys.get("id");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** Direction is the caller's business: ScrollSubrange turns this into backward() for `before`. */
    @Override
    public ScrollPosition fromCursor(String cursor) {
        String raw;
        try {
            raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new BadCursorException();
        }
        var parts = raw.split(":", -1);
        if (parts.length != 3 || !parts[0].equals("v1")) throw new BadCursorException();
        try {
            return ScrollPosition.forward(Map.of("published", Integer.parseInt(parts[1]), "id", Long.parseLong(parts[2])));
        } catch (NumberFormatException e) {
            throw new BadCursorException();
        }
    }

    static final class BadCursorException extends IllegalArgumentException {
        BadCursorException() { super("Not a books cursor"); }
    }
}

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * A global id: the type name and the type-local key, as one opaque string. Encoded as unpadded
 * base64url of "Type:key". The key may itself contain ':'; the type never does.
 */
record GlobalId(String type, String key) {
    GlobalId {
        if (type == null || !type.matches("[A-Z][A-Za-z0-9]*")) throw new IllegalArgumentException("type: " + type);
        if (key == null || key.isEmpty()) throw new IllegalArgumentException("empty key");
    }

    String encode() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString((type + ":" + key).getBytes(StandardCharsets.UTF_8));
    }

    /** Empty for anything that is not an id we made: bad base64, no ':', a bad type name, an empty key. */
    static Optional<GlobalId> decode(String opaque) {
        if (opaque == null || opaque.isEmpty()) return Optional.empty();
        String raw;
        try {
            raw = new String(Base64.getUrlDecoder().decode(opaque), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        int colon = raw.indexOf(':');
        if (colon < 0) return Optional.empty();
        try {
            return Optional.of(new GlobalId(raw.substring(0, colon), raw.substring(colon + 1)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

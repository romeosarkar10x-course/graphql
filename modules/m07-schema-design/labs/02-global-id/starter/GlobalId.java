import java.util.Optional;

/**
 * A global id: the type name and the type-local key, as one opaque string. Encode as unpadded
 * base64url of "Type:key". The key may itself contain ':'; the type never does.
 */
record GlobalId(String type, String key) {
    String encode() {
        throw new UnsupportedOperationException("TODO");
    }

    /** Empty for anything that is not an id we made: bad base64, no ':', a bad type name, an empty key. */
    static Optional<GlobalId> decode(String opaque) {
        throw new UnsupportedOperationException("TODO");
    }
}

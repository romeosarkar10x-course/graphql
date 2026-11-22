package dev.shelfie.privacy;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Users by id and by handle, cached: profiles are read on every review. A cache is a copy of personal data, so
 * erasing a user must evict it (evict), or the profile outlives the account.
 */
@Component
public class Users {
    public record User(String id, String handle, String name, String email, String phone) {}

    private final JdbcClient jdbc;
    private final Map<String, Optional<User>> byId = new ConcurrentHashMap<>();

    Users(JdbcClient jdbc, SchemaSetup schema) {
        this.jdbc = jdbc;
    }

    public User byId(String id) {
        return byId.computeIfAbsent(id, k -> jdbc.sql("SELECT id, handle, name, email, phone FROM m14_06.users WHERE id = ?")
            .param(k).query(User.class).optional()).orElse(null);
    }

    public User byHandle(String handle) {
        return handle == null ? null : jdbc.sql("SELECT id, handle, name, email, phone FROM m14_06.users WHERE handle = ?")
            .param(handle).query(User.class).optional().orElse(null);
    }

    public void evict(String id) {
        byId.remove(id);
    }
}

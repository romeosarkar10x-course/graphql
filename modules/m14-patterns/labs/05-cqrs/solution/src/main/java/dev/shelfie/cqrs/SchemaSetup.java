package dev.shelfie.cqrs;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Runs db/schema.sql while the context starts, before Kafka listeners and scheduled tasks do (they start once
 * every bean exists). As one statement, so PostgreSQL parses it.
 */
@Component
public class SchemaSetup {
    private final JdbcTemplate jdbc;

    SchemaSetup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void create() throws IOException {
        jdbc.execute(new ClassPathResource("db/schema.sql").getContentAsString(StandardCharsets.UTF_8));
    }
}

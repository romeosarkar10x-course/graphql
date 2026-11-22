package dev.shelfie.tenancy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

/** Runs db/schema.sql as the owner at start-up, as one statement so PostgreSQL handles the DO block itself. */
@Configuration
public class SchemaSetup {
    @Bean
    ApplicationRunner schema(JdbcTemplate jdbc) {
        return args -> jdbc.execute(sql());
    }

    static String sql() throws IOException {
        return new ClassPathResource("db/schema.sql").getContentAsString(StandardCharsets.UTF_8);
    }
}

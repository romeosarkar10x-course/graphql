package dev.shelfie.connections;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.graphql.data.pagination.CursorEncoder;
import org.springframework.graphql.data.pagination.CursorStrategy;
import org.springframework.graphql.data.pagination.EncodingCursorStrategy;

@SpringBootApplication
public class ConnectionsApplication {
    public static void main(String[] args) { SpringApplication.run(ConnectionsApplication.class, args); }

    /**
     * Replaces Boot's cursor strategy. The bean type matters: Boot backs off only for an
     * EncodingCursorStrategy. A plain CursorStrategy bean makes two candidates, and Spring for GraphQL
     * then registers no ScrollSubrange resolver at all (lecture 8.2).
     */
    @Bean
    EncodingCursorStrategy<ScrollPosition> cursorStrategy() {
        return CursorStrategy.withEncoder(new BookCursors(), CursorEncoder.noOpEncoder());   // BookCursors does its own base64
    }
}

package dev.shelfie.subgraphs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Two of Shelfie's subgraphs, catalog and reviews, in one jar: run it once per subgraph.
 *
 *   java -jar shelfie-java-subgraphs.jar --spring.profiles.active=catalog --server.port=4002
 *
 * The profile (catalog or reviews) picks the controllers and the schema in graphql/<profile>/.
 */
@SpringBootApplication
public class SubgraphsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SubgraphsApplication.class, args);
    }
}

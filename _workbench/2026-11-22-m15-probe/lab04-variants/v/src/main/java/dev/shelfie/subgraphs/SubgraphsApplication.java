package dev.shelfie.subgraphs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.data.federation.FederationSchemaFactory;

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

    @Bean
    GraphQlSourceBuilderCustomizer federation(FederationSchemaFactory factory) {
        return builder -> builder.schemaFactory(factory::createGraphQLSchema);
    }

    @Bean
    FederationSchemaFactory federationSchemaFactory() {
        return new FederationSchemaFactory();
    }
}

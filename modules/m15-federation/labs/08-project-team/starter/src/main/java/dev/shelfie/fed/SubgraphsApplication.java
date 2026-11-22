package dev.shelfie.fed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.data.federation.FederationSchemaFactory;

/**
 * Shelfie's four subgraphs in one jar: run it once per subgraph.
 *
 *   java -jar shelfie-team.jar --spring.profiles.active=catalog --server.port=4002
 *
 * The profile picks the controllers and the schema (classpath:graphql/<profile>/ unless
 * spring.graphql.schema.locations says otherwise). Options:
 *   --shelfie.delay-ms=N              wait N ms before answering each request (latency experiments)
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

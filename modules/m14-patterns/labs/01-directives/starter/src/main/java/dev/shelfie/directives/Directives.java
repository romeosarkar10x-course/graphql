package dev.shelfie.directives;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * The schema declares @uppercase, @mask and @currency and uses them (see shelfie.graphqls), but nothing makes them
 * true yet: graphql-java accepts any declared directive and ignores it at run time.
 *
 * TODO one SchemaDirectiveWiring per directive, registered with wiring.directive(name, wiring): wrap each field's
 * data fetcher; refuse misuse (wrong field type, unknown currency, negative keep) while the schema is built.
 */
@Configuration
public class Directives {
    @Bean
    RuntimeWiringConfigurer directiveWiring() {
        return wiring -> { };
    }
}

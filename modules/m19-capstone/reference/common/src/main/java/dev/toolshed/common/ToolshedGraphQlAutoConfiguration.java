package dev.toolshed.common;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.data.federation.FederationSchemaFactory;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/** Federation, the DateTime scalar, the error contract, the statement counter and trace filtering, for both subgraphs. */
@AutoConfiguration
public class ToolshedGraphQlAutoConfiguration {
    @Bean
    FederationSchemaFactory federationSchemaFactory() {
        return new FederationSchemaFactory();
    }

    @Bean
    GraphQlSourceBuilderCustomizer federation(FederationSchemaFactory factory) {
        return builder -> builder.schemaFactory(factory::createGraphQLSchema);
    }

    @Bean
    RuntimeWiringConfigurer dateTime() {
        return wiring -> wiring.scalar(DateTimeScalar.INSTANCE);
    }

    @Bean
    ErrorResolver errorResolver() {
        return new ErrorResolver();
    }

    /** Prometheus scrapes every second: no traces or timers for /actuator requests, only for real traffic. */
    @Bean
    ObservationPredicate noActuatorObservations() {
        return (name, context) -> !(context instanceof ServerRequestObservationContext request
                && request.getCarrier().getRequestURI().startsWith("/actuator"));
    }

    @Bean
    static SqlCounter sqlCounter() {
        return new SqlCounter();
    }
}

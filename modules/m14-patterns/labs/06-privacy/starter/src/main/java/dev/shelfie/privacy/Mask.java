package dev.shelfie.privacy;

import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/** Lab 14.1's @mask. TODO the rule the schema adds: the user the field belongs to sees it whole. */
@Configuration
public class Mask {
    @Bean
    RuntimeWiringConfigurer maskDirective() {
        return wiring -> wiring.directive("mask", new SchemaDirectiveWiring() {
            @Override
            public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> env) {
                int keep = (Integer) env.getAppliedDirective().getArgument("keep").getValue();
                var coordinates = FieldCoordinates.coordinates(env.getFieldsContainer(), env.getElement());
                DataFetcher<?> original = env.getCodeRegistry().getDataFetcher(coordinates, env.getElement());
                env.getCodeRegistry().dataFetcher(coordinates, (DataFetcher<Object>) e -> {
                    Object value = original.get(e);
                    if (value == null) return null;
                    String s = value.toString();
                    int shown = Math.min(keep, s.length());
                    return "*".repeat(s.length() - shown) + s.substring(s.length() - shown);
                });
                return env.getElement();
            }
        });
    }
}

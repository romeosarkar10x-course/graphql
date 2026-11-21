package dev.shelfie.boot;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetcherFactories;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import java.util.List;
import reactor.core.publisher.Mono;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @uppercase, wired after Spring has registered the controllers' fetchers: a type visitor over the
 * finished schema wraps each marked field's fetcher. Spring's controller fetchers return a Reactor Mono, not
 * the String, so the wrapper must map inside it: a plain "value instanceof String" never matches.
 */
@Configuration
public class UppercaseConfig {
    static Object upper(Object value) { return value instanceof String s ? s.toUpperCase() : value; }

    @Bean
    GraphQlSourceBuilderCustomizer uppercaseDirective() {
        return builder -> builder.typeVisitors(List.of(new GraphQLTypeVisitorStub() {
            @Override
            public TraversalControl visitGraphQLFieldDefinition(GraphQLFieldDefinition field, TraverserContext<GraphQLSchemaElement> ctx) {
                if (field.hasAppliedDirective("uppercase") && ctx.getParentNode() instanceof GraphQLFieldsContainer parent) {
                    var registry = ctx.getVarFromParents(GraphQLCodeRegistry.Builder.class);
                    var coordinates = FieldCoordinates.coordinates(parent, field);
                    DataFetcher<?> original = registry.getDataFetcher(coordinates, field);
                    registry.dataFetcher(coordinates, DataFetcherFactories.wrapDataFetcher(original,
                        (env, value) -> value instanceof Mono<?> mono ? mono.map(UppercaseConfig::upper) : upper(value)));
                }
                return TraversalControl.CONTINUE;
            }
        }));
    }
}

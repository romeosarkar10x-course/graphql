package dev.shelfie.ops;

import graphql.ExecutionInput;
import graphql.schema.GraphQLObjectType;
import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.observation.DataFetcherObservationContext;
import org.springframework.graphql.observation.DefaultDataFetcherObservationConvention;
import org.springframework.graphql.observation.DefaultExecutionRequestObservationConvention;
import org.springframework.graphql.observation.ExecutionRequestObservationContext;
import org.springframework.graphql.server.WebGraphQlInterceptor;

/**
 * Module 16.1: one endpoint, many operations. Spring for GraphQL already observes every request and every
 * non-trivial data fetcher; this adds what a dashboard needs and the defaults leave out:
 *
 *   graphql_request_seconds{graphql_operation_name, graphql_client_name, ...}   (+ histogram buckets for p95/p99)
 *   graphql_datafetcher_seconds{graphql_field_coordinate="Book.similar", ...}; spans "graphql field Book.similar"
 *   graphql_errors_total{code, graphql_client_name}                              one per error in a response
 *
 * Client names come from the graphql-client-name header (Hive's convention); only known clients become label
 * values, everything else is "other", so a caller cannot grow the metric's cardinality.
 */
@Configuration
public class Observability {
    static final String CLIENT = "shelfie.client";

    @Bean
    WebGraphQlInterceptor clientName(@Value("${shelfie.clients:web,ios,android,partner-bulk}") Set<String> known,
                                     MeterRegistry meters) {
        return (request, chain) -> {
            String raw = request.getHeaders().getFirst("graphql-client-name");
            String client = raw == null ? "unknown" : known.contains(raw) ? raw : "other";
            request.configureExecutionInput((input, builder) ->
                    builder.graphQLContext(Map.of(CLIENT, client)).build());
            return chain.next(request).doOnNext(response -> {
                for (var error : response.getErrors()) {
                    Object code = error.getExtensions().get("code");
                    String c = code != null ? code.toString()
                            : error.getErrorType() != null ? error.getErrorType().toString() : "UNKNOWN";
                    meters.counter("graphql.errors", "code", c, "graphql.client.name", client).increment();
                }
            });
        };
    }

    @Bean
    DefaultExecutionRequestObservationConvention requestConvention() {
        return new DefaultExecutionRequestObservationConvention() {
            @Override
            public KeyValues getLowCardinalityKeyValues(ExecutionRequestObservationContext context) {
                ExecutionInput input = context.getExecutionInput();
                String name = input.getOperationName() == null ? "anonymous" : input.getOperationName();
                String client = input.getGraphQLContext().getOrDefault(CLIENT, "unknown");
                return super.getLowCardinalityKeyValues(context)
                        .and(KeyValue.of("graphql.operation.name", name), KeyValue.of("graphql.client.name", client));
            }
        };
    }

    @Bean
    DefaultDataFetcherObservationConvention dataFetcherConvention() {
        return new DefaultDataFetcherObservationConvention() {
            @Override
            public String getContextualName(DataFetcherObservationContext context) {
                return "graphql field " + coordinate(context);
            }

            @Override
            public KeyValues getLowCardinalityKeyValues(DataFetcherObservationContext context) {
                return super.getLowCardinalityKeyValues(context).and("graphql.field.coordinate", coordinate(context));
            }
        };
    }

    static String coordinate(DataFetcherObservationContext context) {
        var env = context.getEnvironment();
        String type = env.getParentType() instanceof GraphQLObjectType o ? o.getName() : "?";
        return type + "." + env.getField().getName();
    }

}

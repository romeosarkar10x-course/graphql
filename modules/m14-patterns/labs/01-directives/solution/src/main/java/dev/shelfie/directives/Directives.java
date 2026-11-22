package dev.shelfie.directives;

import graphql.GraphQLException;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * One SchemaDirectiveWiring per directive. graphql-java calls onField once per field and directive while it builds
 * the schema, in the order the directives are written; each wiring wraps the data fetcher it finds, so the first
 * directive is the one closest to the resolver. Misuse fails at start-up, naming the field, not at request time.
 */
@Configuration
public class Directives {
    static final Map<String, BigDecimal> RATES = Map.of(
        "EUR", BigDecimal.ONE, "USD", new BigDecimal("1.08"), "GBP", new BigDecimal("0.85"), "JPY", new BigDecimal("162"));

    @Bean
    RuntimeWiringConfigurer directiveWiring() {
        return wiring -> wiring
            .directive("uppercase", wrap("uppercase", (env, coordinate) -> {
                requireString(env, coordinate, "uppercase");
                return value -> value instanceof List<?> list
                    ? list.stream().map(v -> v == null ? null : v.toString().toUpperCase(Locale.ROOT)).toList()
                    : value.toString().toUpperCase(Locale.ROOT);
            }))
            .directive("mask", wrap("mask", (env, coordinate) -> {
                Integer keep = (Integer) env.getAppliedDirective().getArgument("keep").getValue();
                if (keep == null || keep < 0) throw new GraphQLException("@mask on " + coordinate + ": keep must be 0 or more, got " + keep);
                return value -> {
                    String s = value.toString();
                    int shown = Math.min(keep, s.length());
                    return "*".repeat(s.length() - shown) + s.substring(s.length() - shown);
                };
            }))
            .directive("currency", wrap("currency", (env, coordinate) -> {
                requireString(env, coordinate, "currency");
                String code = (String) env.getAppliedDirective().getArgument("code").getValue();
                BigDecimal rate = RATES.get(code);
                if (rate == null) throw new GraphQLException("@currency on " + coordinate + ": unknown currency " + code + ", expected one of " + RATES.keySet());
                int digits = Currency.getInstance(code).getDefaultFractionDigits();
                return value -> {
                    BigDecimal eur;
                    try {
                        eur = value instanceof BigDecimal b ? b : new BigDecimal(value.toString());
                    } catch (NumberFormatException e) {
                        throw new GraphQLException("@currency on " + coordinate + ": '" + value + "' is not an amount");
                    }
                    return code + " " + eur.multiply(rate).setScale(digits, RoundingMode.HALF_EVEN).toPlainString();
                };
            }));
    }

    /** Builds the value transformation for one field (validating it), then wraps the field's data fetcher. */
    interface Transform {
        Function<Object, Object> forField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> env, String coordinate);
    }

    static SchemaDirectiveWiring wrap(String name, Transform transform) {
        return new SchemaDirectiveWiring() {
            @Override
            public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> env) {
                GraphQLFieldDefinition field = env.getElement();
                FieldCoordinates coordinates = FieldCoordinates.coordinates(env.getFieldsContainer(), field);
                Function<Object, Object> fn = transform.forField(env, coordinates.getTypeName() + "." + coordinates.getFieldName());
                DataFetcher<?> original = env.getCodeRegistry().getDataFetcher(coordinates, field);
                DataFetcher<?> wrapped = e -> apply(original.get(e), v -> {
                    try {
                        return fn.apply(v);
                    } catch (GraphQLException ex) {   // a value the directive cannot handle: a field error, not a 500
                        return DataFetcherResult.newResult().error(GraphqlErrorBuilder.newError(e).message(ex.getMessage()).build()).build();
                    }
                });
                env.getCodeRegistry().dataFetcher(coordinates, wrapped);
                return field;
            }
        };
    }

    /** Values may come back later (an async controller): transform them when they arrive. Null stays null. */
    static Object apply(Object value, Function<Object, Object> fn) {
        if (value instanceof CompletionStage<?> stage) return stage.thenApply(v -> apply(v, fn));
        if (value instanceof DataFetcherResult<?> result) {   // an earlier directive's field error, or a resolver's partial result
            if (result.getData() == null) return result;
            Object data = fn.apply(result.getData());
            if (data instanceof DataFetcherResult<?> failed) return failed;
            return DataFetcherResult.newResult().data(data).errors(result.getErrors()).localContext(result.getLocalContext()).build();
        }
        return value == null ? null : fn.apply(value);
    }

    static void requireString(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> env, String coordinate, String name) {
        var type = GraphQLTypeUtil.unwrapNonNull(env.getElement().getType());
        if (type instanceof GraphQLList list) type = GraphQLTypeUtil.unwrapNonNull(list.getWrappedType());
        boolean string = type instanceof GraphQLScalarType scalar && scalar.getName().equals("String");
        boolean listAllowed = name.equals("uppercase");
        boolean isList = GraphQLTypeUtil.unwrapNonNull(env.getElement().getType()) instanceof GraphQLList;
        if (!string || (isList && !listAllowed))
            throw new GraphQLException("@" + name + " on " + coordinate + ": only allowed on String fields, found "
                + GraphQLTypeUtil.simplePrint(env.getElement().getType()));
    }
}

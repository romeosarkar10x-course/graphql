package dev.toolshed.common;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;

/**
 * The error contract (docs/DESIGN.md, "Errors"): expected product failures are union members in the data;
 * these three exceptions are request-level mistakes and become errors with a classification. Anything else
 * is left to Spring, which answers INTERNAL_ERROR without the exception's message.
 */
public class ErrorResolver extends DataFetcherExceptionResolverAdapter {
    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        GraphqlErrorBuilder<?> b = GraphqlErrorBuilder.newError(env).message(ex.getMessage());
        return switch (ex) {
            case BadInput bad -> b.errorType(ErrorType.BAD_REQUEST).extensions(Map.of("argument", bad.argument())).build();
            case Forbidden f -> b.errorType(ErrorType.FORBIDDEN).build();
            case Unauthenticated u -> b.errorType(ErrorType.UNAUTHORIZED).build();
            default -> null;
        };
    }
}

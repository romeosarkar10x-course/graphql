package dev.shelfie.boot;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;

/**
 * Exceptions from fetchers become errors here. Domain exceptions keep their message and code.
 * Everything else is a bug or an outage: the client gets "Internal error" and an errorId, and the log
 * gets the exception with the same id.
 */
@Component
public class ErrorResolver extends DataFetcherExceptionResolverAdapter {
    private static final Logger log = LoggerFactory.getLogger(ErrorResolver.class);

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof ShelfieException e) {
            return GraphqlErrorBuilder.newError(env).errorType(e.code()).message(e.getMessage())
                .extensions(Map.of("code", e.code().name())).build();
        }
        String errorId = env.getGraphQlContext().getOrDefault(ErrorPolicyInterceptor.REQUEST_ID, "unknown");
        log.error("errorId={} path={}", errorId, env.getExecutionStepInfo().getPath(), ex);
        return GraphqlErrorBuilder.newError(env).errorType(ShelfieError.INTERNAL).message("Internal error")
            .extensions(Map.of("code", ShelfieError.INTERNAL.name(), "errorId", errorId)).build();
    }
}

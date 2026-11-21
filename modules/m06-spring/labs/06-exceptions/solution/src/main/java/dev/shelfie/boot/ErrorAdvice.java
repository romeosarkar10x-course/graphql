package dev.shelfie.boot;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

/** One place for every controller's exceptions. Anything not handled here gets Spring's masked INTERNAL_ERROR. */
@ControllerAdvice
public class ErrorAdvice {

    @GraphQlExceptionHandler
    GraphQLError notFound(Problems.NotFound e, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env).errorType(ErrorType.NOT_FOUND).message(e.getMessage()).build();
    }

    @GraphQlExceptionHandler
    GraphQLError forbidden(Problems.Forbidden e, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env).errorType(ErrorType.FORBIDDEN).message(e.getMessage()).build();
    }

    @GraphQlExceptionHandler
    GraphQLError invalid(Problems.Invalid e, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env).errorType(ErrorType.BAD_REQUEST).message(e.getMessage())
            .extensions(Map.of("field", e.field)).build();
    }

    /** Spring's ErrorType has no CONFLICT; say it with a code in extensions instead of a classification. */
    @GraphQlExceptionHandler
    GraphQLError conflict(Problems.Conflict e, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env).errorType(ErrorType.BAD_REQUEST).message(e.getMessage())
            .extensions(Map.of("code", "CONFLICT", "currentVersion", e.currentVersion)).build();
    }

    @GraphQlExceptionHandler
    GraphQLError timeout(TimeoutException e, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env).errorType(ErrorType.INTERNAL_ERROR)
            .message("A service Shelfie depends on is not answering").extensions(Map.of("retryable", true)).build();
    }
}

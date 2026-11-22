package dev.shelfie.ops;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

/** Errors clients can act on carry extensions.code. */
@ControllerAdvice
public class ErrorAdvice {
    @GraphQlExceptionHandler
    GraphQLError unavailable(ReviewsClient.ReviewsUnavailable e, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env).errorType(ErrorType.INTERNAL_ERROR).message(e.getMessage())
                .extensions(Map.of("code", "REVIEWS_UNAVAILABLE")).build();
    }

    @GraphQlExceptionHandler
    GraphQLError badInput(IllegalArgumentException e, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env).errorType(ErrorType.BAD_REQUEST).message(e.getMessage())
                .extensions(Map.of("code", "BAD_USER_INPUT")).build();
    }
}

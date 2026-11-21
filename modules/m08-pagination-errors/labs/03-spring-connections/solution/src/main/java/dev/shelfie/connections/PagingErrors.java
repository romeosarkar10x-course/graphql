package dev.shelfie.connections;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

/** The client's mistakes are BAD_REQUEST with a message it can show; anything else stays INTERNAL_ERROR. */
@ControllerAdvice
class PagingErrors {
    @GraphQlExceptionHandler({BookCursors.BadCursorException.class, BookController.PageSizeException.class})
    GraphQLError badRequest(IllegalArgumentException e, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env).errorType(ErrorType.BAD_REQUEST).message(e.getMessage()).build();
    }
}

package dev.shelfie.app.graph;

import dev.shelfie.app.service.Problems;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

/** Service failures as GraphQL errors, with Spring's classification in extensions.classification. */
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
}

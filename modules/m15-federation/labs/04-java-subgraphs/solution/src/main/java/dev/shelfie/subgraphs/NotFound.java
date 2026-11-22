package dev.shelfie.subgraphs;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.federation.RepresentationNotResolvedException;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Spring turns a null from a single or DataLoader @EntityMapping into RepresentationNotResolvedException, which
 * would reach the router (and the client) as an opaque INTERNAL_ERROR. Say what happened instead.
 */
@ControllerAdvice
class NotFound {
    @GraphQlExceptionHandler
    GraphQLError notFound(RepresentationNotResolvedException e, DataFetchingEnvironment env) {
        return GraphQLError.newError().errorType(ErrorType.NOT_FOUND).message(e.getMessage())
                .path(env.getExecutionStepInfo().getPath()).build();
    }
}

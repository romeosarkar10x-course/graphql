package dev.shelfie.secure;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

/** A refusal the client should see, with Spring's error classification (UNAUTHORIZED, FORBIDDEN, ...). */
public class Denied extends RuntimeException {
  final ErrorType type;

  public Denied(ErrorType type, String message) {
    super(message);
    this.type = type;
  }

  @ControllerAdvice
  static class Handler {
    @GraphQlExceptionHandler
    GraphQLError handle(Denied e, DataFetchingEnvironment env) {
      return GraphqlErrorBuilder.newError(env).errorType(e.type).message(e.getMessage()).build();
    }
  }
}

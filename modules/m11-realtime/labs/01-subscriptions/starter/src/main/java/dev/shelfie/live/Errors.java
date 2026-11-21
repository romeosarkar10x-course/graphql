package dev.shelfie.live;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;

@Component
class Errors extends DataFetcherExceptionResolverAdapter {
  @Override
  protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
    return ex instanceof Problem p ? GraphqlErrorBuilder.newError(env).errorType(p.type).message(p.getMessage()).build() : null;
  }

  // TODO lab 11.1: a source stream that fails ends the subscription with Spring's bare "Subscription error".
  // Tell the client what broke: code SOURCE_STREAM_FAILED for a SourceStreamFailed, the classification for
  // a Problem.
}

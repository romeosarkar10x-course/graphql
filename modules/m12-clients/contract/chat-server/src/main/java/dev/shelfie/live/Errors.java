package dev.shelfie.live;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.SubscriptionExceptionResolverAdapter;
import org.springframework.stereotype.Component;

@Component
class Errors extends DataFetcherExceptionResolverAdapter {
  @Override
  protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
    return ex instanceof Problem p ? GraphqlErrorBuilder.newError(env).errorType(p.type).message(p.getMessage()).build() : null;
  }

  /**
   * Lecture 11.1: an exception in the source stream ends the subscription. Without this resolver Spring
   * sends graphql-transport-ws "error" with the message "Subscription error" and nothing else; with it,
   * the client learns what broke and whether resubscribing makes sense.
   */
  @Component
  static class SourceErrors extends SubscriptionExceptionResolverAdapter {
    @Override
    protected GraphQLError resolveToSingleError(Throwable ex) {
      if (ex instanceof SourceStreamFailed)
        return GraphqlErrorBuilder.newError().message(ex.getMessage())
            .extensions(Map.of("code", "SOURCE_STREAM_FAILED", "retryable", true)).build();
      if (ex instanceof Problem p) return GraphqlErrorBuilder.newError().errorType(p.type).message(p.getMessage()).build();
      return null;
    }
  }
}

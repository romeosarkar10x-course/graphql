package dev.shelfie.live;

import graphql.ExperimentalApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlInterceptor;

/** "We turned on @defer." graphql-java's incremental support is experimental and off by default. */
@Configuration
class Incremental {
  @Bean
  WebGraphQlInterceptor incrementalEverywhere() {
    return (request, chain) -> {
      request.configureExecutionInput((in, b) -> b.graphQLContext(c -> c.put(ExperimentalApi.ENABLE_INCREMENTAL_SUPPORT, true)).build());
      return chain.next(request);
    };
  }

  // TODO lab 11.6: a client that sends Accept: multipart/mixed should get the page in parts, the slow
  // recommendations last; every other client should get one complete JSON response.
}

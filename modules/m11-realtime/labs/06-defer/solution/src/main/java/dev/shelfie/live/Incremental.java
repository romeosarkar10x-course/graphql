package dev.shelfie.live;

import graphql.ExecutionResult;
import graphql.ExperimentalApi;
import graphql.incremental.DelayedIncrementalPartialResult;
import graphql.incremental.IncrementalExecutionResult;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.servlet.function.*;
import reactor.core.publisher.Flux;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lecture 11.6: @defer over multipart/mixed. Spring for GraphQL 2.1 has no incremental transport: with
 * graphql-java's experimental flag on, its HTTP handler writes the initial payload and drops the rest.
 * So the flag goes on only for requests that accept multipart/mixed, and this route streams them.
 */
@Configuration
class Incremental {
  /** Named explicitly: "*\/*" is compatible with multipart/mixed, but a client sending it expects one JSON body. */
  static boolean acceptsMultipart(java.util.List<MediaType> accept) {
    return accept.stream().anyMatch(a -> a.getType().equals("multipart") && a.getSubtype().equals("mixed"));
  }

  /** Only a client that can read incremental payloads gets them; every other client gets one full result. */
  @Bean
  WebGraphQlInterceptor incrementalOnlyWhenAccepted() {
    return (request, chain) -> {
      boolean accepts = acceptsMultipart(request.getHeaders().getAccept());
      if (accepts) request.configureExecutionInput((in, b) -> b.graphQLContext(c -> c.put(ExperimentalApi.ENABLE_INCREMENTAL_SUPPORT, true)).build());
      return chain.next(request);
    };
  }

  /** Ordered before Boot's GraphQL route, and only for POST /graphql with Accept: multipart/mixed. */
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  RouterFunction<ServerResponse> multipartGraphQl(WebGraphQlHandler graphQl, JsonMapper json) {
    return RouterFunctions.route(RequestPredicates.POST("/graphql").and(request ->
            acceptsMultipart(request.headers().accept())),
        request -> {
          @SuppressWarnings("unchecked")
          Map<String, Object> body = json.readValue(request.servletRequest().getInputStream(), Map.class);
          var web = new WebGraphQlRequest(URI.create(request.uri().toString()), request.headers().asHttpHeaders(),
              new LinkedMultiValueMap<>(), null, Map.of(), body, UUID.randomUUID().toString(), null);
          ExecutionResult result = graphQl.handleRequest(web).block().getExecutionResult();
          return ServerResponse.ok().contentType(MediaType.parseMediaType("multipart/mixed; boundary=\"-\""))
              .build((req, res) -> { stream(res, result, json); return null; });
        });
  }

  /** Each payload is one part: "\r\n---\r\n", a Content-Type header, a blank line, the JSON. Then "\r\n-----\r\n". */
  static void stream(HttpServletResponse res, ExecutionResult result, JsonMapper json) throws IOException {
    OutputStream out = res.getOutputStream();
    part(out, json.writeValueAsBytes(result.toSpecification()));
    if (result instanceof IncrementalExecutionResult incremental) {
      for (DelayedIncrementalPartialResult later : Flux.from(incremental.getIncrementalItemPublisher()).toIterable())
        part(out, json.writeValueAsBytes(later.toSpecification()));
    }
    out.write("\r\n-----\r\n".getBytes(StandardCharsets.UTF_8));
    out.flush();
  }

  private static void part(OutputStream out, byte[] payload) throws IOException {
    out.write("\r\n---\r\nContent-Type: application/json; charset=utf-8\r\n\r\n".getBytes(StandardCharsets.UTF_8));
    out.write(payload);
    out.flush();   // the point of @defer: this part reaches the client before the slow one is ready
  }
}

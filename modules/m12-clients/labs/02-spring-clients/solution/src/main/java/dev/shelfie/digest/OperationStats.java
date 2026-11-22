package dev.shelfie.digest;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.graphql.client.ClientGraphQlRequest;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.GraphQlClientInterceptor;
import org.springframework.graphql.client.SyncGraphQlClientInterceptor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Interceptors see every request and response at the GraphQL level: here they count operations by name. */
@Component
public class OperationStats {
  private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();
  private final Map<String, AtomicInteger> errors = new ConcurrentHashMap<>();

  SyncGraphQlClientInterceptor sync() {
    return new SyncGraphQlClientInterceptor() {
      @Override
      public ClientGraphQlResponse intercept(ClientGraphQlRequest request, Chain chain) {
        return record(request, chain.next(request));
      }
    };
  }

  GraphQlClientInterceptor reactive() {
    return new GraphQlClientInterceptor() {
      @Override
      public Mono<ClientGraphQlResponse> intercept(ClientGraphQlRequest request, Chain chain) {
        return chain.next(request).map(response -> record(request, response));
      }
    };
  }

  private ClientGraphQlResponse record(ClientGraphQlRequest request, ClientGraphQlResponse response) {
    String name = request.getOperationName() != null ? request.getOperationName() : "(anonymous)";
    counts.computeIfAbsent(name, k -> new AtomicInteger()).incrementAndGet();
    if (!response.getErrors().isEmpty()) errors.computeIfAbsent(name, k -> new AtomicInteger()).incrementAndGet();
    return response;
  }

  public Map<String, Object> snapshot() {
    Map<String, Object> out = new TreeMap<>();
    counts.forEach((k, v) -> out.put(k, Map.of("requests", v.get(), "withErrors", errors.getOrDefault(k, new AtomicInteger()).get())));
    return out;
  }
}

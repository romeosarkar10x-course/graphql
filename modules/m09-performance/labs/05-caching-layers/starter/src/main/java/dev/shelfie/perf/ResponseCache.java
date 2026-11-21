package dev.shelfie.perf;

import graphql.ExecutionResultImpl;
import graphql.language.OperationDefinition;
import graphql.parser.Parser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.support.DefaultExecutionGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/**
 * The response cache: whole query responses in Redis, shared by every instance of the app. A hit skips
 * parsing, validation and execution. Only error-free query responses are stored, for 5 minutes.
 * The header X-Response-Cache says HIT or MISS.
 */
@Component
@Order(3)
class ResponseCache implements WebGraphQlInterceptor {
    static final String KEYS = "resp:keys";
    private final StringRedisTemplate redis;
    private final JsonMapper json = JsonMapper.builder().build();

    ResponseCache(StringRedisTemplate redis) { this.redis = redis; }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        if (!isQuery(request)) return chain.next(request);
        String key = key(request);
        String hit = redis.opsForValue().get(key);
        if (hit != null) {
            Map<String, Object> cached = json.readValue(hit, Map.class);
            var result = ExecutionResultImpl.newExecutionResult().data(cached.get("data")).build();
            var response = new WebGraphQlResponse(new DefaultExecutionGraphQlResponse(request.toExecutionInput(), result));
            response.getResponseHeaders().set("X-Response-Cache", "HIT");
            return Mono.just(response);
        }
        return chain.next(request).doOnNext(response -> {
            response.getResponseHeaders().set("X-Response-Cache", "MISS");
            if (response.isValid() && response.getErrors().isEmpty()) {
                redis.opsForValue().set(key, json.writeValueAsString(Map.of("data", response.getData())), Duration.ofMinutes(5));
                redis.opsForSet().add(KEYS, key);
            }
        });
    }

    /** Everything that can change the response. */
    static String key(WebGraphQlRequest request) {
        String parts = request.getDocument() + "\u0000" + request.getOperationName() + "\u0000" + request.getVariables();
        try {
            return "resp:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(parts.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    void invalidateAll() {
        var keys = redis.opsForSet().members(KEYS);
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
        redis.delete(KEYS);
    }

    /** Parse just to find the operation type; a document that doesn't parse isn't cached. */
    static boolean isQuery(WebGraphQlRequest request) {
        try {
            var ops = Parser.parse(request.getDocument()).getDefinitionsOfType(OperationDefinition.class);
            String name = request.getOperationName();
            return ops.stream().filter(o -> name == null ? ops.size() == 1 : name.equals(o.getName()))
                .findFirst().map(o -> o.getOperation() == OperationDefinition.Operation.QUERY).orElse(false);
        } catch (RuntimeException e) { return false; }
    }
}

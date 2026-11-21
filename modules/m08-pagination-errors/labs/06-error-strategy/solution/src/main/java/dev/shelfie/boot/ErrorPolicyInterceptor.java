package dev.shelfie.boot;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * The error policy for every response, including errors no resolver sees (parse, validation,
 * variable coercion): every error has extensions.code from the contract, and the response carries
 * the request id in extensions.requestId, the same id as the errorId of any INTERNAL error.
 */
@Component
@Order(0)
public class ErrorPolicyInterceptor implements WebGraphQlInterceptor {
    public static final String REQUEST_ID = "requestId";

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String id = request.getHeaders().getFirst("X-Request-Id");
        String requestId = id != null && id.matches("[A-Za-z0-9-]{1,64}") ? id : UUID.randomUUID().toString();
        request.configureExecutionInput((input, builder) -> builder.graphQLContext(c -> c.put(REQUEST_ID, requestId)).build());
        return chain.next(request).map(response -> response.transform(b -> {
            b.errors(response.getExecutionResult().getErrors().stream().map(ErrorPolicyInterceptor::withCode).toList());
            var extensions = new HashMap<Object, Object>(response.getExecutionResult().getExtensions() == null ? Map.of() : response.getExecutionResult().getExtensions());
            extensions.put(REQUEST_ID, requestId);
            b.extensions(extensions);
        }));
    }

    /** Errors that already have a code keep it; graphql-java's and Spring's classifications are mapped. */
    static GraphQLError withCode(GraphQLError e) {
        var ext = e.getExtensions() == null ? Map.<String, Object>of() : e.getExtensions();
        if (ext.containsKey("code")) return e;
        String code = switch (String.valueOf(e.getErrorType())) {
            case "InvalidSyntax", "ValidationError", "OperationNotSupported", "BAD_REQUEST" -> ShelfieError.INVALID_REQUEST.name();
            case "UNAUTHORIZED" -> ShelfieError.UNAUTHENTICATED.name();
            case "FORBIDDEN" -> ShelfieError.FORBIDDEN.name();
            case "NOT_FOUND" -> ShelfieError.NOT_FOUND.name();
            default -> ShelfieError.INTERNAL.name();
        };
        var extensions = new HashMap<>(ext);
        extensions.put("code", code);
        return GraphqlErrorBuilder.newError().message(e.getMessage()).locations(e.getLocations())
            .path(e.getPath()).errorType(e.getErrorType()).extensions(extensions).build();
    }
}

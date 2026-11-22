package dev.shelfie.tenancy;

import graphql.GraphQLContext;
import org.springframework.graphql.execution.ErrorType;

/** Who is asking: the token's preferred_username and tenant claim. Put in the GraphQLContext by TenantInterceptor. */
public record Tenant(String handle, String id) {
    static final String KEY = "tenant";

    static Tenant of(GraphQLContext context) {
        Tenant t = context.get(KEY);
        if (t == null) throw new NotSignedIn();
        return t;
    }

    /** Mapped to an UNAUTHORIZED error by ShelfieController. */
    static final class NotSignedIn extends RuntimeException {
        NotSignedIn() { super("Sign in with a token that names your tenant"); }
        ErrorType type() { return ErrorType.UNAUTHORIZED; }
    }
}

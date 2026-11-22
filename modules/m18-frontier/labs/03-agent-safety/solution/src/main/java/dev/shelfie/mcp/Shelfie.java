package dev.shelfie.mcp;

import java.util.Map;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Component;

/** Calls Shelfie as the caller: the X-Shelfie-User header is forwarded, nothing else from the agent's request. */
@Component
class Shelfie {
    private final HttpSyncGraphQlClient client;

    Shelfie(HttpSyncGraphQlClient client) {
        this.client = client;
    }

    Map<String, Object> operation(String name, Map<String, Object> variables) {
        return data(name, as().documentName(name).variables(variables).executeSync());
    }

    Map<String, Object> document(String document) {
        return data("run_graphql", as().document(document).executeSync());
    }

    private HttpSyncGraphQlClient as() {
        return Caller.user().map(u -> client.mutate().header(Caller.HEADER, u).build()).orElse(client);
    }

    private static Map<String, Object> data(String what, ClientGraphQlResponse response) {
        if (!response.getErrors().isEmpty()) {
            throw new IllegalStateException(what + " failed: " + response.getErrors().getFirst().getMessage());
        }
        Map<String, Object> data = response.getData();
        return data == null ? Map.of() : data;
    }
}

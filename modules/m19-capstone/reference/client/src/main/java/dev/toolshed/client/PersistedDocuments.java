package dev.toolshed.client;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.graphql.client.ClientGraphQlRequest;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.SyncGraphQlClientInterceptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Trusted documents on the client side: Spring's client loads an operation from graphql-documents/ by name,
 * and this interceptor sends its id (from persisted-documents.json, built by scripts/persist.sh) instead of
 * its text. The router refuses any document it does not know, so an edited operation fails loudly until
 * persist.sh is run again.
 */
class PersistedDocuments implements SyncGraphQlClientInterceptor {
    private static final Pattern OPERATION = Pattern.compile("(?m)^(query|mutation|subscription)\\s+([A-Za-z0-9_]+)");
    private final Map<String, String> idByName = new HashMap<>();

    PersistedDocuments() {
        try (InputStream in = getClass().getResourceAsStream("/persisted-documents.json")) {
            JsonNode manifest = JsonMapper.builder().build().readTree(in);
            for (Map.Entry<String, JsonNode> e : manifest.properties()) {
                Matcher m = OPERATION.matcher(e.getValue().asString());
                if (m.find()) idByName.put(m.group(2), e.getKey());
            }
        } catch (Exception e) {
            throw new IllegalStateException("persisted-documents.json is missing from the jar: run scripts/persist.sh and rebuild", e);
        }
    }

    static String operationName(String document) {
        Matcher m = OPERATION.matcher(document);
        if (!m.find()) throw new IllegalArgumentException("no named operation in the document");
        return m.group(2);
    }

    @Override
    public ClientGraphQlResponse intercept(ClientGraphQlRequest request, Chain chain) {
        String name = operationName(request.getDocument());
        String id = idByName.get(name);
        if (id == null) throw new IllegalStateException("operation " + name + " is not in persisted-documents.json: run scripts/persist.sh");
        return chain.next(new ById(request, id));
    }

    /** The same request, serialized as {documentId, operationName, variables} without the document text. */
    private record ById(ClientGraphQlRequest delegate, String id) implements ClientGraphQlRequest {
        public String getDocument() { return delegate.getDocument(); }
        public String getOperationName() { return delegate.getOperationName(); }
        public Map<String, Object> getVariables() { return delegate.getVariables(); }
        public Map<String, Object> getExtensions() { return delegate.getExtensions(); }
        public Map<String, Object> getAttributes() { return delegate.getAttributes(); }

        public Map<String, Object> toMap() {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("documentId", id);
            if (!getVariables().isEmpty()) body.put("variables", getVariables());
            if (!getExtensions().isEmpty()) body.put("extensions", getExtensions());
            return body;
        }
    }
}

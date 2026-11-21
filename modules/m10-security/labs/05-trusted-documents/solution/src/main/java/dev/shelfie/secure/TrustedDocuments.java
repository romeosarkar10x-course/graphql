package dev.shelfie.secure;

import graphql.ExecutionInput;
import graphql.GraphqlErrorBuilder;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.execution.preparsed.persisted.PersistedQuerySupport;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lecture 10.5: the server runs only documents its own client build produced. The manifest
 * (trusted-documents.json, written by client/BuildManifest.java) maps SHA-256 ids to bodies.
 *
 *   - extensions.persistedQuery.sha256Hash in the manifest: run the manifest's body
 *   - a hash with query text that does not match it: refused (no swapping the text under a known id)
 *   - a hash that is not in the manifest: refused, even with its text (APQ registration is not trust)
 *   - full text whose hash is in the manifest: run (old clients that send text keep working)
 *   - anything else: refused, unless shelfie.trusted-documents.enforce=false (local development)
 *
 * Parsed documents are cached by id, so trusted operations also skip parsing and validation.
 */
@Configuration
public class TrustedDocuments implements PreparsedDocumentProvider {
  private final Map<String, String> bodies = new HashMap<>();
  private final Map<String, PreparsedDocumentEntry> parsed = new ConcurrentHashMap<>();
  private final boolean enforce;

  TrustedDocuments(@Value("${shelfie.trusted-documents.enforce:true}") boolean enforce) throws Exception {
    this.enforce = enforce;
    JsonNode manifest = JsonMapper.builder().build().readTree(new ClassPathResource("trusted-documents.json").getInputStream());
    for (JsonNode op : manifest.get("operations")) bodies.put(op.get("id").asString(), op.get("body").asString());
  }

  @Bean
  GraphQlSourceBuilderCustomizer trustedDocumentsOnly() {
    return b -> b.configureGraphQl(g -> g.preparsedDocumentProvider(this));
  }

  @Override
  public CompletableFuture<PreparsedDocumentEntry> getDocumentAsync(ExecutionInput input, Function<ExecutionInput, PreparsedDocumentEntry> parseAndValidate) {
    // A request with only an id reaches graphql-java with a placeholder query text.
    String text = input.getQuery() == null || input.getQuery().equals(PersistedQuerySupport.PERSISTED_QUERY_MARKER) ? "" : input.getQuery().strip();
    String id = requestedId(input);
    if (id == null && !text.isEmpty()) id = sha256(text);
    if (id != null && bodies.containsKey(id)) {
      if (!text.isEmpty() && !sha256(text).equals(id))
        return refuse("the query text does not match document " + id, "DOCUMENT_MISMATCH");
      String body = bodies.get(id);
      return CompletableFuture.completedFuture(parsed.computeIfAbsent(id, k -> parseAndValidate.apply(input.transform(b -> b.query(body)))));
    }
    if (!enforce && !text.isEmpty()) return CompletableFuture.completedFuture(parseAndValidate.apply(input));
    return refuse("not a trusted document: this server only runs operations from Shelfie's own clients", "NOT_TRUSTED");
  }

  private static String requestedId(ExecutionInput input) {
    if (input.getExtensions().get("persistedQuery") instanceof Map<?, ?> pq && pq.get("sha256Hash") instanceof String h) return h;
    return null;
  }

  private static CompletableFuture<PreparsedDocumentEntry> refuse(String message, String code) {
    return CompletableFuture.completedFuture(new PreparsedDocumentEntry(
        GraphqlErrorBuilder.newError().message(message).extensions(Map.of("code", code)).build()));
  }

  static String sha256(String s) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}

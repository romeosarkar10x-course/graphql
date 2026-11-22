package dev.shelfie.ops;

import graphql.ExecutionResult;
import graphql.analysis.QueryTraverser;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Module 16.2: field-level usage, reported in Hive's usage report format (version 2), so a registry can answer
 * "which fields does anyone still use, and which clients?". Every operation adds:
 *
 *   map[key] = {operation, operationName, fields: ["Query.book", "Query.book.id", "Book.title", ...]}
 *   operations += {operationMapKey: key, timestamp (ms), execution: {ok, duration (ns), errorsTotal},
 *                  metadata: {client: {name, version}}}
 *
 * Reports go out every second (or every 100 operations) to shelfie.usage.endpoint with the headers Hive's usage
 * API wants (X-Usage-API-Version: 2, Authorization: Bearer <token>). The course points it at
 * contract/UsageCollector.java; Hive Cloud's endpoint is https://app.graphql-hive.com/usage with a real token.
 * Fields are field coordinates and argument coordinates (when the argument is given); Hive also lists type names.
 */
@Component
public class UsageReporter extends SimplePerformantInstrumentation implements DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(UsageReporter.class);
    private final RestClient http;
    private final String token;
    private final Map<String, Map<String, Object>> map = new LinkedHashMap<>();
    private final List<Map<String, Object>> operations = new ArrayList<>();
    private final ScheduledExecutorService flusher = Executors.newSingleThreadScheduledExecutor();

    public UsageReporter(RestClient.Builder builder, @Value("${shelfie.usage.endpoint:http://localhost:4200/usage}") String endpoint,
                         @Value("${shelfie.usage.token:course-usage-token}") String token) {
        this.http = builder.clone().baseUrl(endpoint).build();
        this.token = token;
        flusher.scheduleWithFixedDelay(this::flush, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecution(InstrumentationExecutionParameters p, InstrumentationState state) {
        long start = System.nanoTime();
        return SimpleInstrumentationContext.whenCompleted((result, failure) -> {
            if (result == null || p.getQuery() == null) return;
            List<String> fields = coordinates(p);
            if (fields.isEmpty()) return;   // did not parse or validate: nothing of the schema was used
            String key = key(p.getQuery(), p.getOperation());
            Object time = p.getGraphQLContext().get(Observability.TIME);
            Map<String, Object> client = new LinkedHashMap<>();
            client.put("name", p.getGraphQLContext().getOrDefault(Observability.CLIENT_RAW, "unknown"));
            client.put("version", p.getGraphQLContext().getOrDefault(Observability.CLIENT_VERSION, "unknown"));
            Map<String, Object> op = new LinkedHashMap<>();
            op.put("operationMapKey", key);
            op.put("timestamp", time instanceof Long t ? t : System.currentTimeMillis());
            op.put("execution", Map.of("ok", result.getErrors().isEmpty(), "duration", System.nanoTime() - start,
                    "errorsTotal", result.getErrors().size()));
            op.put("metadata", Map.of("client", client));
            boolean full;
            synchronized (this) {
                map.computeIfAbsent(key, k -> {
                    Map<String, Object> record = new LinkedHashMap<>();
                    record.put("operation", p.getQuery());
                    record.put("operationName", p.getOperation());
                    record.put("fields", fields);
                    return record;
                });
                operations.add(op);
                full = operations.size() >= 100;
            }
            if (full) flusher.execute(this::flush);
        });
    }

    /** Field and argument coordinates the operation selects, in document order, without repeats. */
    static List<String> coordinates(InstrumentationExecutionParameters p) {
        Set<String> out = new LinkedHashSet<>();
        try {
            QueryTraverser.newQueryTraverser().schema(p.getSchema()).document(graphql.parser.Parser.parse(p.getQuery()))
                    .operationName(p.getOperation()).variables(p.getVariables()).build()
                    .visitPreOrder(new QueryVisitorStub() {
                        @Override
                        public void visitField(QueryVisitorFieldEnvironment env) {
                            if (env.isTypeNameIntrospectionField()) return;
                            String field = env.getFieldsContainer().getName() + "." + env.getFieldDefinition().getName();
                            out.add(field);
                            env.getField().getArguments().forEach(a -> out.add(field + "." + a.getName()));
                        }
                    });
        } catch (RuntimeException e) {
            return List.of();
        }
        return List.copyOf(out);
    }

    static String key(String document, String operationName) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256")
                    .digest((operationName + "\n" + document).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h, 0, 8);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Send what has been collected as one report; on failure keep it for the next try (up to 10,000 operations). */
    synchronized void flush() {
        if (operations.isEmpty()) return;
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("size", operations.size());
        report.put("map", new LinkedHashMap<>(map));
        report.put("operations", new ArrayList<>(operations));
        try {
            http.post().header("X-Usage-API-Version", "2").header("Authorization", "Bearer " + token)
                    .body(report).retrieve().toBodilessEntity();
            map.clear();
            operations.clear();
        } catch (RuntimeException e) {
            log.warn("usage report not sent ({} operations kept): {}", operations.size(), e.getMessage());
            if (operations.size() > 10_000) { operations.clear(); map.clear(); }
        }
    }

    @Override
    public void destroy() {
        flusher.shutdown();
        flush();
    }

}

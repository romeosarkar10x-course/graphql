package dev.shelfie.ops;

import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import org.springframework.stereotype.Component;

/**
 * Module 16.2: field-level usage, reported in Hive's usage report format (version 2). Every operation adds:
 *
 *   map[key] = {operation, operationName, fields: ["Query.book", "Query.book.id", "Book.title", ...]}
 *   operations += {operationMapKey: key, timestamp (ms), execution: {ok, duration (ns), errorsTotal},
 *                  metadata: {client: {name, version}}}
 *
 * Send a report at least every second to shelfie.usage.endpoint (default http://localhost:4200/usage) with the
 * headers X-Usage-API-Version: 2 and Authorization: Bearer ${shelfie.usage.token:course-usage-token}.
 * The timestamp is the request's X-Simulated-Time when it has one (Observability.TIME in the GraphQL context).
 * The lab README has the details and the checks.
 */
@Component
public class UsageReporter extends SimplePerformantInstrumentation {
    // TODO 16.2
}

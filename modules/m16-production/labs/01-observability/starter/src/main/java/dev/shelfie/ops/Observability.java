package dev.shelfie.ops;

import org.springframework.context.annotation.Configuration;

/**
 * Module 16.1: one endpoint, many operations. Your job (see the lab README for the exact names):
 *
 *   - client names: read the graphql-client-name header into the GraphQL context (CLIENT below; only known
 *     names, "other" for the rest, "unknown" without the header), and the raw name and version for 16.2;
 *   - graphql_request_seconds by operation name and client name, with histogram buckets;
 *   - graphql_datafetcher_seconds and the field spans by field coordinate (Type.field);
 *   - graphql_errors_total by error code and client name.
 *
 * Other classes (CostMetric, UsageReporter) already read the context keys below; keep their names.
 */
@Configuration
public class Observability {
    static final String CLIENT = "shelfie.client";
    /** The raw client name and version (usage reports keep them; metrics only take known names). */
    static final String CLIENT_RAW = "shelfie.client.raw", CLIENT_VERSION = "shelfie.client.version";
    /** X-Simulated-Time (ms since the epoch): lets a replay of last week's traffic carry last week's timestamps. */
    static final String TIME = "shelfie.time";

    // TODO 16.1
}

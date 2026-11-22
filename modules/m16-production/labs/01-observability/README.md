# Lab 16.1: Observability: metrics by operation and client, traces per field

Lecture: [16.1 Observability](../../lectures/16.1-observability.md). Grading: auto + rubric. Needs the observability
stack (`bin/course ops`: Prometheus, Tempo and Grafana in `.cache/ops`, about 670 MB once).

The starter is Shelfie in production shape: catalogue, clubs, and reviews from a separate reviews service
(`contract/ReviewsService.java`, port 4100). It has Actuator for health and nothing else. Every request is
`POST /graphql`, so a plain HTTP dashboard shows one line. Make it observable.

**Metrics** at `/actuator/prometheus` (add `micrometer-registry-prometheus`):

| Series | Labels (besides Spring's own) |
| --- | --- |
| `graphql_request_seconds_*`, with `_bucket` histogram series | `graphql_operation_name` (the request's `operationName`, or `anonymous`), `graphql_client_name` |
| `graphql_datafetcher_seconds_*` | `graphql_field_coordinate`, e.g. `Book.similar` |
| `graphql_errors_total` | `code` (the error's `extensions.code`, else its classification, e.g. `ValidationError`), `graphql_client_name` |

`graphql_client_name` comes from the `graphql-client-name` header (Hive's convention). Only names in
`shelfie.clients` (`web,ios,android,partner-bulk`) become label values. Any other name becomes `other`, and a
request with no header is `unknown`. Store it in the GraphQL context under `Observability.CLIENT`. While you are
there, also store the raw name, `graphql-client-version` and `X-Simulated-Time` under the other keys in
`Observability.java`. Lab 16.2 uses them.

**Traces** over OTLP to Tempo (Boot 4's `spring-boot-starter-opentelemetry`):

- sample every request;
- export to the URL in the environment variable `OTLP_TRACES` (default `http://localhost:4318/v1/traces`);
- name field spans `graphql field <Type.field>`;
- make calls to the reviews service child spans in the same trace. The client must come from Boot's
  `RestClient.Builder`, which Boot 4 configures only with `spring-boot-starter-restclient`.

Push metrics nowhere: Prometheus scrapes them.

```bash
bin/course start m16/01
cd workspace/m16-production/01-observability
bin/course test m16/01
```

Look at it yourself: start the stack and the app, send traffic, open Grafana.

```bash
COURSE_PUBLISH=3000 bin/course shell
source /course/tools/testlib.sh; source /course/modules/m16-production/contract/ops.sh
ops_start; reviews_start
cd /course/workspace/m16-production/01-observability && mvn -q package -DskipTests && java -jar target/shelfie-ops.jar &
# send a few club pages (graphql-client-name: web), then http://localhost:3000 > Explore > Tempo / Prometheus
```

## Experiment: find the slowest field from traces alone

Send the club page as `web` a few dozen times:

```graphql
query ClubPage { club(id: "c1") { name readingList { title reviews { rating } similar { title } } } }
```

Then find the field that costs it the most time, using Tempo (TraceQL, or the trace view in Grafana) and, if you
like, the data fetcher metric. Do not read the code. Tempo's search shows a trace 20 to 30 s after it arrives. A
trace looked up by id shows at once.

Write `FINDINGS.md`. It starts with a line `Slowest field: Type.field`, followed by:

- the query that found it, with the numbers it showed;
- what the default instrumentation lacked.

**Bonus:** run the starter with the OpenTelemetry Java agent (`otel_agent` in `ops.sh` fetches 2.33.1) and
compare its spans with yours.

## Done when

`bin/course test m16/01` passes and `FINDINGS.md` passes the rubric (`bin/course grade m16/01`).

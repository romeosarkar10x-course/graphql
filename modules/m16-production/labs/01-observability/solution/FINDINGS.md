# Findings: the slowest field, from traces alone

## The slowest field

Slowest field: Book.similar

## How I found it (TraceQL or the trace view; no reading the code)

I sent the club page twenty times as client `web`, waited half a minute (Tempo's search lags its ingest by
20 to 30 s; a trace by id is there at once), and asked Tempo for the field spans that took longest:

    { name =~ "graphql field .*" && duration > 20ms }

Every match was `graphql field Book.similar`, 30 to 35 ms each, four of them per club page (one per book in the
reading list), and they run one after another: 4 x 31 ms of a 60 ms `graphql query` span. The next slowest field
span was `Query.club` at about 5 ms. To rank by total rather than by single span I used the metric the same
convention produces:

    topk(3, sum by (graphql_field_coordinate) (rate(graphql_datafetcher_seconds_sum[5m])))

which puts Book.similar first by an order of magnitude. Book.reviews never appears as a field span: it is a
batch mapping, so its time shows as the `graphql dataloader` span and the `http get` span to the reviews service
under it (4 ms for four books in one call).

## What the default instrumentation showed, and what I had to add

Spring for GraphQL observes the request and every non-trivial data fetcher out of the box, but its spans say
`graphql field similar` with only a path, and its request metric has no operation name: one endpoint, one
time series per outcome. I added the operation name and a bounded client name (unknown clients become `other`,
so a scraper cannot create a label value per request) to the request metric, the field coordinate to the data
fetcher spans and metric, a counter of errors by `extensions.code`, and histogram buckets so Prometheus can
compute p95 per operation. The reviews calls only became spans once the client was built from Boot's
`RestClient.Builder` (which needs `spring-boot-starter-restclient` in Boot 4).

## Bonus: the OpenTelemetry Java agent on the same app

On the starter (no Micrometer tracing at all) the agent 2.33.1 gave three spans: `POST /graphql`, `query`
(with the document, arguments replaced by `?`) and the `GET` to the reviews service. With
`otel.instrumentation.graphql.data-fetcher.enabled=true` it adds one span per data fetcher, named by the field
name only, like Spring's default. Zero code and it covers HTTP clients and JDBC too; but it adds no metrics by
operation or client, and naming spans by coordinate would need an agent extension. I would run the agent for
infrastructure spans and keep the Micrometer conventions for GraphQL-specific names and labels.

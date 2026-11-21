# Lab 11.6: `@defer` and `@stream`

Lecture: [11.6 @defer and @stream](../../lectures/11.6-defer-stream.md). Grading: auto + rubric. Needs nothing.

The starter is lab 11.1's server, where someone "turned on `@defer`": an interceptor sets graphql-java's
experimental `ExperimentalApi.ENABLE_INCREMENTAL_SUPPORT` flag for every request. Send
`contract/club-page.graphql` (its recommendations panel takes 1.5 s and is deferred) and see what happens:
a JSON client silently loses the recommendations, and a client asking for `multipart/mixed` is refused.

Make `@defer` work end to end:

1. A request with `Accept: multipart/mixed` gets a `multipart/mixed; boundary="-"` response: the first
   part as soon as the fast fields are ready, the deferred part when the recommender answers, then the
   closing delimiter (the GraphQL over HTTP incremental delivery RFC's framing).
2. Every other client (`application/json`, `application/graphql-response+json`, `*/*`) gets one complete
   response, recommendations included.

```bash
bin/course start m11/06
cd workspace/m11-realtime/06-defer
bin/course shell -c "mvn -q package && (java -jar target/shelfie-live.jar > /tmp/app.log &) && sleep 8 && \
  curl -sN -w '\nfirst byte %{time_starttransfer}s, total %{time_total}s\n' -H 'Content-Type: application/json' \
    -H 'Accept: multipart/mixed' \
    -d \"\$(jq -nc --rawfile q /course/modules/m11-realtime/contract/club-page.graphql '{query: \$q, variables: {id: \"c1\"}}')\" \
    localhost:4000/graphql"
```

In `notes.md`: time to first byte and total with and without `@defer` (and for a JSON client), the payloads
graphql-java produced compared with RFC 1110's format, what Spring did before your fix, and when `@defer`
is worth it.

## Done when

`bin/course test m11/06` passes (framing, both parts, the timing, JSON clients get everything) and
`notes.md` passes the rubric (`bin/course grade m11/06`).

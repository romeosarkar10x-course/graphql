#!/usr/bin/env bash
# Lab 16.1: metrics by operation and client, errors by code, field spans by coordinate, read back from a real
# Prometheus and Tempo. Then the experiment's answer and notes.
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m16-production/contract/ops.sh
source /course/modules/m16-production/contract/lab-app.sh
cd "$LAB_DIR" || exit 1
trap 'app_stop; ops_stop' EXIT
ops_start tempo prometheus || finish
export OTLP_TRACES=http://localhost:4318/v1/traces
lab_app_up --management.opentelemetry.tracing.export.schedule-delay=500ms || finish
CLUB='{"operationName":"ClubPage","query":"query ClubPage { club(id: \"c1\") { name readingList { title reviews { rating } similar { title } } } }"}'
TID=4bf92f3577b34da6a3ce929d0e0e4736
gql_post "$CLUB" "traceparent: 00-$TID-00f067aa0ba902b7-01" "graphql-client-name: web" >/dev/null
for _ in 1 2 3; do gql_post "$CLUB" "graphql-client-name: web" >/dev/null; done
gql_post "$CLUB" "graphql-client-name: scraper-9000" >/dev/null
gql_post '{"operationName":"Shelf","query":"query Shelf { books(first: 2) { title } }"}' >/dev/null
gql_post '{"operationName":"Rate","query":"mutation Rate { addReview(bookId: \"b1\", rating: 9, body: \"x\") { id } }"}' "graphql-client-name: ios" >/dev/null
gql_post '{"query":"{ nope }"}' "graphql-client-name: android" >/dev/null
M=$(curl -fsS localhost:4000/actuator/prometheus 2>/dev/null)
[[ -n "$M" ]] && pass "/actuator/prometheus answers" || { fail "/actuator/prometheus answers" "add the Prometheus registry and expose the endpoint"; finish; }
grep -E '^graphql_request_seconds_count\{' <<<"$M" | grep 'graphql_operation_name="ClubPage"' | grep -q 'graphql_client_name="web"' \
  && pass "request metric by operation and client (ClubPage, web)" || fail "graphql_request_seconds_count{graphql_operation_name=\"ClubPage\", graphql_client_name=\"web\"} missing"
grep -E '^graphql_request_seconds_count\{' <<<"$M" | grep 'graphql_operation_name="ClubPage"' | grep -q 'graphql_client_name="other"' \
  && grep -E '^graphql_request_seconds_count\{' <<<"$M" | grep 'graphql_operation_name="Shelf"' | grep -q 'graphql_client_name="unknown"' \
  && ! grep -q 'scraper-9000' <<<"$M" \
  && pass "client names are bounded: an unknown name is \"other\", no header is \"unknown\"" \
  || fail "client names: scraper-9000 must show as \"other\" (never as itself), no header as \"unknown\""
grep -qE '^graphql_request_seconds_bucket\{.*graphql_operation_name="ClubPage"' <<<"$M" \
  && pass "request metric has histogram buckets (p95 per operation)" || fail "no graphql_request_seconds_bucket series: histogram buckets are off"
grep -E '^graphql_datafetcher_seconds_count\{' <<<"$M" | grep -q 'graphql_field_coordinate="Book.similar"' \
  && grep -E '^graphql_datafetcher_seconds_count\{' <<<"$M" | grep -q 'graphql_field_coordinate="Query.club"' \
  && pass "data fetcher metric by field coordinate (Book.similar, Query.club)" || fail "graphql_datafetcher_seconds_count{graphql_field_coordinate=\"Book.similar\"} missing"
grep -E '^graphql_errors_total\{' <<<"$M" | grep 'code="BAD_USER_INPUT"' | grep -q 'graphql_client_name="ios"' \
  && grep -E '^graphql_errors_total\{' <<<"$M" | grep 'code="ValidationError"' | grep -q 'graphql_client_name="android"' \
  && pass "errors by code and client (BAD_USER_INPUT from ios, ValidationError from android)" \
  || fail "graphql_errors_total{code, graphql_client_name}: expected BAD_USER_INPUT/ios and ValidationError/android"
r=$(promq_wait 'histogram_quantile(0.95, sum by (le, graphql_operation_name) (rate(graphql_request_seconds_bucket[1m])))' 25 | jq -r '[.[].metric.graphql_operation_name] | join(",")')
[[ ",$r," == *",ClubPage,"* ]] && pass "Prometheus computes p95 per operation (histogram_quantile)" || fail "Prometheus has no p95 for ClubPage" "got: $r"
for _ in $(seq 1 40); do tempo_trace $TID >/tmp/t.json 2>/dev/null; [[ $(jq '.trace.resourceSpans | length' /tmp/t.json 2>/dev/null) -gt 0 ]] && break; sleep 0.5; done
names=$(jq -r '[.trace.resourceSpans[]?.scopeSpans[]?.spans[]?.name] | .[]' /tmp/t.json 2>/dev/null | sort | uniq -c)
if [[ -z "$names" ]]; then fail "the trace reached Tempo" "no spans for trace $TID: OpenTelemetry starter, sampling 1.0, OTLP endpoint from OTLP_TRACES"
else
  pass "the trace reached Tempo ($(jq '[.trace.resourceSpans[].scopeSpans[].spans[]] | length' /tmp/t.json) spans)"
  grep -qE '^ *4 graphql field Book.similar$' <<<"$names" && grep -q 'graphql field Query.club' <<<"$names" \
    && pass "field spans are named by coordinate (4 x graphql field Book.similar)" || fail "field spans by coordinate" "$(echo "$names" | head -8 | tr '\n' ';')"
  jq -e '[.trace.resourceSpans[].scopeSpans[].spans[] | select(.attributes[]? | (.key == "http.url" or .key == "url.full") and (.value.stringValue | test("4100/reviews")))] | length > 0' /tmp/t.json >/dev/null \
    && pass "calls to the reviews service are spans in the same trace" || fail "no client span for the reviews service" "build the RestClient from Boot's RestClient.Builder"
fi
require_file FINDINGS.md "the experiment's findings" && {
  a=$(grep -m1 -oE '^Slowest field: *[A-Za-z_]+\.[A-Za-z_]+' FINDINGS.md | sed 's/^Slowest field: *//')
  [[ "$(printf %s "$a" | sha256sum | cut -c1-16)" == "d1c294f07db871b2" ]] && pass "the slowest field is right" || fail "the slowest field (line 'Slowest field: Type.field')" "got: ${a:-nothing}"
  notes_check FINDINGS.md 150 m16/01; }
finish

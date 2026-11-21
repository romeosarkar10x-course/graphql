#!/usr/bin/env bash
# Module 11 probe 1: subscriptions over WebSocket and SSE, source-stream errors, @defer.
cd /course/_workbench/2026-11-21-m11-probe
java -jar app/target/probe.jar > app.log 2>&1 & PID=$!
for i in $(seq 60); do curl -s localhost:4000/graphql >/dev/null && break; sleep 0.5; done
F="java Frames.java ws://localhost:4000/graphql"
echo "== WS: ticks(n:3)"; $F 'subscription { ticks(n: 3) }'
echo "== WS: ticks(n:5, failAt:3) (exception in map)"; $F 'subscription { ticks(n: 5, failAt: 3) }'
echo "== WS: ticks(n:5, throwAt:3) (Flux.error)"; $F 'subscription { ticks(n: 5, throwAt: 3) }'
echo "== WS: two root fields"; $F 'subscription { a: ticks(n: 1) b: ticks(n: 1) }'
echo "== WS: @skip on root"; $F 'subscription { ticks(n: 1) @skip(if: false) }'
echo "== SSE: ticks(n:3)"
curl -sN -m 5 -D - -H 'Content-Type: application/json' -H 'Accept: text/event-stream' -d '{"query":"subscription { ticks(n: 3) }"}' localhost:4000/graphql
echo; echo "== SSE: ticks(n:5, failAt:3)"
curl -sN -m 5 -H 'Content-Type: application/json' -H 'Accept: text/event-stream' -d '{"query":"subscription { ticks(n: 5, failAt: 3) }"}' localhost:4000/graphql
echo; echo "== HTTP POST subscription without event-stream Accept"
curl -s -m 5 -w ' [%{http_code}]' -H 'Content-Type: application/json' -d '{"query":"subscription { ticks(n: 3) }"}' localhost:4000/graphql
echo; echo "== raw graphql-java @defer"
curl -s -H 'Content-Type: application/json' -d '{"query":"{ club(id: \"c1\") { name ... @defer(label: \"recs\") { recommendations } members } }"}' localhost:4000/raw
echo "== raw graphql-java @stream?"
curl -s -H 'Content-Type: application/json' -d '{"query":"{ club(id: \"c1\") { members @stream(initialCount: 1) } }"}' localhost:4000/raw
echo; echo "== Spring /graphql with @defer, Accept multipart/mixed"
curl -s -m 5 -w ' [%{http_code} %{content_type} %{time_starttransfer}s]' -H 'Content-Type: application/json' -H 'Accept: multipart/mixed; deferSpec=20220824, application/json' -d '{"query":"{ club(id: \"c1\") { name ... @defer { recommendations } } }"}' localhost:4000/graphql
echo; echo "== Spring /graphql with @defer, SSE accept"
curl -sN -m 5 -H 'Content-Type: application/json' -H 'Accept: text/event-stream' -d '{"query":"{ club(id: \"c1\") { name ... @defer { recommendations } } }"}' localhost:4000/graphql
echo; echo "== server log errors"; grep -E 'ERROR|WARN|Exception' app.log | head -10
kill $PID; wait $PID 2>/dev/null

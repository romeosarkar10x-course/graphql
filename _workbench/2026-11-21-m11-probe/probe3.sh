#!/usr/bin/env bash
# Module 11 probe 3: routing of Accept: multipart/mixed; slow consumer flood without keep-alive.
cd /course/_workbench/2026-11-21-m11-probe
java -Xmx256m -jar app/target/probe.jar --spring.graphql.websocket.keep-alive= --spring.graphql.http.sse.keep-alive= > app3.log 2>&1 & PID=$!
for i in $(seq 60); do curl -s localhost:4000/stats >/dev/null && break; sleep 0.5; done
for acc in 'multipart/mixed' 'multipart/mixed; deferSpec=20220824' 'multipart/mixed, application/json' 'application/json, multipart/mixed'; do
  echo "== Accept: $acc"
  curl -s -m 3 -w ' [%{http_code} %{content_type}]' -H 'Content-Type: application/json' -H "Accept: $acc" -d '{"query":"{ hello }"}' localhost:4000/graphql; echo
done
echo "== flood 10000/s for 8 s, buffer, slow reader 10/s, no keep-alive"
java SlowReader.java ws://localhost:4000/graphql 'subscription { flood(perSecond: 10000, seconds: 8) }' 10 12 &
R=$!
for s in 2 4 6 8 10 12; do sleep 2; echo "  server t=${s}s $(curl -s localhost:4000/stats)"; done
wait $R
echo "== log"; grep -E 'ERROR|WARN|Exception' app3.log | cut -c1-250 | sort | uniq -c | head
kill $PID; wait $PID 2>/dev/null

#!/usr/bin/env bash
# Module 11 probe 2: slow consumer under a 10,000 events/s flood, keep-alive, SSE GET, resolver, cleanup, routing.
cd /course/_workbench/2026-11-21-m11-probe
java -Xmx256m -jar app/target/probe.jar > app2.log 2>&1 & PID=$!
for i in $(seq 60); do curl -s localhost:4000/stats >/dev/null && break; sleep 0.5; done
stats() { curl -s localhost:4000/stats; echo; }
echo "== resolver: ticks(n:5, failAt:3) with SubscriptionExceptionResolverAdapter"
java Frames.java ws://localhost:4000/graphql 'subscription { ticks(n: 5, failAt: 3) }'
echo "== keep-alive: idle subscription, 5 s (WS keep-alive 2s)"
java Frames.java ws://localhost:4000/graphql 'subscription { idle }' 5
echo "== cleanup after client abort"; stats
echo "== SSE GET with keep-alive 2s, 5 s idle"
curl -sN -m 5 -H 'Accept: text/event-stream' 'localhost:4000/graphql?query=subscription%20%7B%20idle%20%7D' | cat -A | head -10
echo; stats
for mode in buffer latest sample; do
  echo "== flood 10000/s for 8 s, mode=$mode, slow reader 10 frames/s"
  java SlowReader.java ws://localhost:4000/graphql "subscription { flood(perSecond: 10000, seconds: 8, mode: \"$mode\") }" 10 10 &
  R=$!
  for s in 2 4 6 8 10 12; do sleep 2; echo "  server t=${s}s $(stats) rss=$(( $(awk '/VmRSS/{print $2}' /proc/$PID/status) / 1024 ))MB"; done
  wait $R
  kill -0 $PID 2>/dev/null || { echo "SERVER DIED"; tail -5 app2.log; break; }
done
echo "== fast reader, flood 10000/s for 3 s, buffer"
java Frames.java ws://localhost:4000/graphql 'subscription { flood(perSecond: 10000, seconds: 3) }' 10 | grep -c '"next"'
stats
echo "== POST /graphql Accept multipart/mixed: who answers?"
curl -s -m 3 -H 'Content-Type: application/json' -H 'Accept: multipart/mixed' -d '{"query":"{ hello }"}' localhost:4000/graphql; echo
echo "== POST /graphql Accept application/json"
curl -s -m 3 -H 'Content-Type: application/json' -H 'Accept: application/json' -d '{"query":"{ hello }"}' localhost:4000/graphql; echo
echo "== log"; grep -E 'ERROR|WARN|Exception|OutOfMemory' app2.log | cut -c1-300 | sort | uniq -c | head -10
kill $PID 2>/dev/null; wait $PID 2>/dev/null

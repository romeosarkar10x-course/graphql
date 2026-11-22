#!/usr/bin/env bash
# Lab 12.7's experiment: the app online, then in airplane mode (the Toxiproxy proxy disabled), then back.
# Run inside the course network: bin/course mobile ./airplane.sh
set -u
T=${TOXI_API:-http://toxiproxy:8474}
app() { echo "\$ shelfie-mobile $*"; build/install/shelfie-mobile/bin/shelfie-mobile "$@"; }
airplane() { curl -fsS -o /dev/null -X POST $T/proxies/mobile -d "{\"enabled\":$1}"; echo "--- airplane mode $([[ $1 == false ]] && echo ON || echo OFF)"; }
curl -s -o /dev/null -X DELETE $T/proxies/mobile
curl -fsS -o /dev/null -X POST $T/proxies -d '{"name":"mobile","listen":"0.0.0.0:8669","upstream":"shelfie-ref:8080"}'
curl -fsS -o /dev/null -X POST http://shelfie-ref:8080/_course/reset-data
rm -f shelfie.db outbox.txt
app online
airplane false
app online
app offline
app review b1 1 "Read on a plane, not for me."
app offline
airplane true
app sync
app online
curl -s -o /dev/null -X DELETE $T/proxies/mobile
curl -fsS -o /dev/null -X POST http://shelfie-ref:8080/_course/reset-data

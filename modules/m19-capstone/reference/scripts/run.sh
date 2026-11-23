#!/usr/bin/env bash
# Run Toolshed by hand inside the course image (bin/course shell, from this directory):
#   scripts/run.sh            build if needed, start inventory, lending and the router; Ctrl-C stops all three
# PostgreSQL must be up (docker compose -f docker/compose.yaml up -d postgres, or bin/course ref up).
# From the host: COURSE_PUBLISH=4000 bin/course shell, then HOST=0.0.0.0 scripts/run.sh.
set -euo pipefail
cd "$(dirname "$0")/.."
[[ -f inventory/target/inventory.jar && -f lending/target/lending.jar ]] || mvn -q -DskipTests package
pids=()
trap 'kill "${pids[@]}" 2>/dev/null; wait' EXIT INT TERM
wait_for() { for _ in $(seq 90); do curl -fs -o /dev/null "$1" && return 0; sleep 1; done; echo "$1 did not answer" >&2; exit 1; }
java -Xmx256m -jar inventory/target/inventory.jar > /tmp/toolshed-inventory.log 2>&1 & pids+=($!)
java -Xmx256m -jar lending/target/lending.jar > /tmp/toolshed-lending.log 2>&1 & pids+=($!)
wait_for http://localhost:4001/actuator/health
wait_for http://localhost:4002/actuator/health
scripts/router.sh > /tmp/toolshed-router.log 2>&1 & pids+=($!)
wait_for http://localhost:4000/health
echo "Toolshed is up: router http://localhost:4000/graphql (logs in /tmp/toolshed-*.log). Ctrl-C to stop."
wait

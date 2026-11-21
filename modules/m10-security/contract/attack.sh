#!/usr/bin/env bash
# Send one attack and measure what it cost the server (lab 10.3).
#   attack.sh <file.graphql|file.json> [pid]
# A .graphql file is sent as {"query": ...}; a .json file is sent as the request body.
# Env: URL (default http://localhost:4000/graphql), TOKEN (sent as a bearer token), PID (server process;
# default: the vulnerable fork started by vulnerable.sh, else the only java process listening).
# Prints one line: status, wall time, response size, server CPU time and resident memory before/after.
set -euo pipefail
f="${1:?usage: attack.sh <file.graphql|file.json> [pid]}"
URL="${URL:-http://localhost:4000/graphql}"
pid="${2:-${PID:-$(cat /tmp/vulnerable.pid 2>/dev/null || pgrep -n java)}}"
cpu() { awk '{print $14 + $15}' "/proc/$pid/stat"; }               # utime + stime, in clock ticks
rss() { awk '/VmRSS/ {print int($2 / 1024)}' "/proc/$pid/status"; }  # MB
hz=$(getconf CLK_TCK)
if [[ "$f" == *.json ]]; then body=$(cat "$f"); else body=$(jq -cn --rawfile q "$f" '{query: $q}'); fi
auth=(); [[ -n "${TOKEN:-}" ]] && auth=(-H "Authorization: Bearer $TOKEN")
c0=$(cpu); m0=$(rss)
out=$(curl -s -o /tmp/attack.out -w '%{http_code} %{time_total} %{size_download}' -m 120 "${auth[@]}" \
  -H 'Content-Type: application/json' --data-binary @- "$URL" <<<"$body") || out="000 120 0"
c1=$(cpu); m1=$(rss)
read -r code secs bytes <<<"$out"
printf '%-28s status %s  %6.0f ms  %9d bytes  server CPU %6d ms  RSS %4d -> %4d MB  errors: %s\n' \
  "$(basename "$f")" "$code" "$(echo "$secs * 1000" | bc)" "$bytes" $(( (c1 - c0) * 1000 / hz )) "$m0" "$m1" \
  "$(jq -r '[.errors[]?.message][0] // "none"' /tmp/attack.out 2>/dev/null | cut -c1-80)"

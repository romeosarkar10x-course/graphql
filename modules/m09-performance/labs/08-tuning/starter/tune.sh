#!/usr/bin/env bash
# Lab 9.8: the same load under every configuration in configs.txt, one after the other.
#   bin/course shell -c 'cd workspace/m09-performance/08-tuning && ./tune.sh'
# configs.txt: one "name: JVM options" per line (# comments allowed). Writes results/tuning.md.
# RATE (default 150 dashboards/s) and DURATION (default 20 s) set the load; HEAP (default 256m) is the
# same for every configuration, so only the options you list differ.
set -euo pipefail
here="${HERE:-$(cd "$(dirname "$0")" && pwd)}"; out="${RESULTS:-$here/results}"; mkdir -p "$out"
export RATE="${RATE:-150}" DURATION="${DURATION:-20}" RESULTS="$out" HERE="$here"
table="$out/tuning.md"
printf '# Tuning runs (%s dashboards/s for %s s, -Xmx%s)\n\n| Config | Options | p50 ms | p95 ms | p99 ms | Failed | GC pauses (ms, sum) | Collector |\n|---|---|---|---|---|---|---|---|\n' \
  "$RATE" "$DURATION" "${HEAP:-256m}" > "$table"
grep -vE '^\s*(#|$)' "$here/configs.txt" | while IFS= read -r line; do
  name="${line%%:*}"; opts="${line#*:}"; name="$(echo $name)"
  echo "== $name:$opts"
  # shellcheck disable=SC2086
  bash "${MEASURE:-$here/measure.sh}" "$name" -Xmx"${HEAP:-256m}" $opts > "$out/$name-measure.log" 2>&1 || true
  g="$out/$name-gatling.txt"
  pct() { grep -E "$1 percentile" "$g" | awk -F'|' '{gsub(/ /, "", $2); print $2}'; }
  ko=$(grep -E '^> KO' "$g" | awk '{print $3}')
  pauses=$(jfr print --json --events jdk.GarbageCollection "$out/$name.jfr" 2>/dev/null \
    | jq '[.recording.events[].values.sumOfPauses | sub("PT"; "") | sub("S$"; "") | tonumber] | add // 0 | . * 1000 | round' 2>/dev/null || echo "?")
  gc=$(grep -m1 -oE 'Using [A-Za-z0-9 ]+' "$out/$name-gc.log" 2>/dev/null | sed 's/Using //' || echo "?")
  printf '| %s | `%s` | %s | %s | %s | %s | %s | %s |\n' "$name" "$(echo $opts)" "$(pct 50th)" "$(pct 95th)" "$(pct 99th)" "${ko:-?}" "$pauses" "$gc" >> "$table"
  rm -f "$out/$name.jfr"   # big; keep it by commenting this line out
done
cat "$table"

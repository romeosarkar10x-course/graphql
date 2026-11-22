#!/usr/bin/env bash
# Module 16.6: find the knee. Open-model load on :4000 at increasing rates; one line per rate with p50/p95/p99.
#   bash /course/modules/m16-production/contract/ramp.sh [seconds per step] [rates...]     (default 5; 100 200 400 800 1600)
# Uses contract/club-page.json (cost 61 by lab 16.6's rule) unless BODY=<file>.
LIB=${LIB:-/course/.cache/course-lib}; C=/course/modules/m16-production/contract
step=${1:-5}; shift || true
rates=("$@"); (( ${#rates[@]} )) || rates=(100 200 400 800 1600)
for r in "${rates[@]}"; do
  out=$(java -cp "$LIB/*" "$C/Peak.java" --seconds "$step" --rate "$r" ${BODY:+--body "$BODY"} --timeout-ms 3000 | tail -1)
  printf '%5d/s  %s\n' "$r" "$(jq -c '{p50, p95, p99, failed}' <<<"$out")"
  sleep 2
done

#!/usr/bin/env bash
# Checkpoint 15, the host half (bin/course checkpoint 15 runs it before the lab checks): your team stack from
# mini-project 15.8 must come up with one docker compose command and answer the club page on localhost:4000.
set -uo pipefail
dir="$ROOT/workspace/m15-federation/08-project-team"
[[ -f "$dir/compose.yaml" ]] || { echo "  checkpoint 15: $dir/compose.yaml is missing (bin/course start m15/08)" >&2; exit 1; }
[[ -x "$ROOT/.cache/fed/bin/router" ]] || "$ROOT/bin/course" fed >/dev/null
if (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null; then echo "  checkpoint 15: port 4000 is in use on this machine; stop what runs there" >&2; exit 1; fi
export COURSE_ROOT="$ROOT" COURSE_UID="$(id -u)" COURSE_GID="$(id -g)"
cd "$dir"
echo "  docker compose up --wait (workspace/m15-federation/08-project-team)"
ok=0
if timeout 600 docker compose up --wait --quiet-pull >/tmp/checkpoint-15-compose.log 2>&1; then
  r=$(curl -s localhost:4000/ -H 'Content-Type: application/json' \
      -d '{"query":"{ club(id: \"c1\") { bookOfTheMonth { book { blurb topReview { author { displayName } } } pickedBy { displayName } } } }"}')
  if [[ "$r" == *'"blurb":"Project Hail Mary by Andy Weir (2021)"'* && "$r" == *'"displayName":"Kemi Adeyemi (@kemia)"'* ]]; then
    echo "  ok    four subgraphs, one router, one command: the club page answers on localhost:4000"; ok=1
  else echo "  FAIL  the stack came up but the club page answered: ${r:0:300}" >&2; fi
else echo "  FAIL  docker compose up did not get healthy (log: /tmp/checkpoint-15-compose.log)" >&2; tail -5 /tmp/checkpoint-15-compose.log >&2; fi
docker compose down >/dev/null 2>&1
(( ok ))

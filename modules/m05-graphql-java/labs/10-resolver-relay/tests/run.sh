#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file Relay.java || finish
cd "$LAB_DIR"
for who in u2 anon; do
  v=$([[ $who == anon ]] || echo "$who")
  timeout 60 java -cp "$(lib_cp)" Relay.java "$LAB_HOME/tests/relay.graphql" $v >/tmp/relay.json 2>/tmp/err.txt
  if json_equal /tmp/relay.json "$LAB_HOME/tests/expected/relay-$who.json"; then pass "every field matches INTENT.md (viewer: $who)"
  else
    fail "fields that do not match INTENT.md yet (viewer: $who)" "$(tail -1 /tmp/err.txt)"
    jq -r --slurpfile want "$LAB_HOME/tests/expected/relay-$who.json" '
      .data as $got | $want[0].data | to_entries[] | .key as $k | .value | to_entries[]
      | select($got[$k][.key] != .value) | "        \($k).\(.key): got \($got[$k][.key] | tojson), want \(.value | tojson)"' /tmp/relay.json 2>/dev/null | head -12
  fi
done
notes_check guesses.md 80 m05/10
finish

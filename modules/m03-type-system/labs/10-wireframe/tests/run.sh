#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
gql() { java -cp "$(lib_cp)" /course/tools/gql/Gql.java "$@"; }
if gql schema-lines schema.graphqls >/tmp/mine.lines 2>/tmp/err.txt; then pass "schema.graphqls builds ($(grep -c '^type ' /tmp/mine.lines) object types)"
else fail "schema.graphqls does not build" "$(head -2 /tmp/err.txt)"; finish; fi
for s in 01-club 02-book 03-profile 04-search; do
  f=screens/$s.graphql
  if ! grep -qE '^\s*query\s+[A-Za-z_]' "$f" 2>/dev/null; then fail "$f: write one named query"; continue; fi
  ops=$(gql operations "$f" 2>/dev/null | wc -l)
  [[ $ops == 1 ]] || { fail "$f: exactly one operation per screen (found $ops)"; continue; }
  if out=$(gql validate "$f" --schema schema.graphqls 2>&1); then pass "$f validates against your schema"
  else fail "$f does not validate" "$(head -2 <<<"$out")"; fi
done
notes_check DESIGN.md 60 m03/10
if (( T_FAILS == 0 )); then
  common=$(comm -12 <(sort /tmp/mine.lines) <(gql schema-lines "$LAB_HOME/solution/schema.graphqls" | sort) | wc -l)
  note "model answer: ${LAB_HOME#/course/}/solution/ (now that you passed). Lines in common with it: $common"
fi
finish

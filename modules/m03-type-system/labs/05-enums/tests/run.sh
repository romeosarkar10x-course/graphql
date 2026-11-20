#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file Enums.java || finish
cd "$LAB_DIR"
check() { # name args...
  local name="$1"; shift
  timeout 60 java -cp "$(lib_cp)" Enums.java "$@" >/tmp/out.json 2>/tmp/err.txt
  if json_equal /tmp/out.json "$LAB_HOME/tests/expected/$name.json"; then pass "$name"
  else fail "$name differs (< yours, > expected)"; json_diff /tmp/out.json "$LAB_HOME/tests/expected/$name.json" | head -8
       [[ -s /tmp/err.txt ]] && tail -2 /tmp/err.txt | sed 's/^/        /'; fi
}
check clubs "$LAB_HOME/tests/clubs.graphql"
check clubs-public "$LAB_HOME/tests/clubs-var.graphql" '{"v":"PUBLIC"}'
check shelf-u1 "$LAB_HOME/tests/shelf.graphql" '{"u":"u1"}'
check shelf-u3-v1 "$LAB_HOME/tests/shelf.graphql" '{"u":"u3"}'
if [[ -s schema-v2.graphqls ]]; then
  d=$(diff <(java -cp "$(lib_cp)" /course/tools/gql/Gql.java schema-lines schema.graphqls) \
           <(java -cp "$(lib_cp)" /course/tools/gql/Gql.java schema-lines schema-v2.graphqls) | grep '^[<>]')
  [[ "$d" == "> enum ReadingStatus.ABANDONED" ]] && pass "schema-v2 adds exactly ReadingStatus.ABANDONED" \
    || fail "schema-v2.graphqls should differ from schema.graphqls only by ReadingStatus.ABANDONED" "diff: $(echo $d | cut -c1-120)"
  check shelf-u3-v2 --v2 "$LAB_HOME/tests/shelf.graphql" '{"u":"u3"}'
else fail "schema-v2.graphqls is missing"; fi
if java -cp "$(lib_cp)" OldClient.java "$LAB_HOME/tests/expected/shelf-u3-v2.json" >/dev/null 2>&1; then
  fail "OldClient.java should still fail on ABANDONED (it is the v1 client; do not edit it)"
else pass "OldClient (strict, v1) fails on the v2 response, as shipped clients would"; fi
require_file TolerantClient.java || finish
out=$(java -cp "$(lib_cp)" TolerantClient.java "$LAB_HOME/tests/expected/shelf-u3-v2.json" 2>/dev/null | tr '\n' ' ')
[[ "$out" == "b9 FINISHED b4 UNKNOWN " ]] && pass "TolerantClient maps ABANDONED to UNKNOWN" || fail "TolerantClient on the v2 response should print 'b9 FINISHED' and 'b4 UNKNOWN'" "got: $out"
out=$(java -cp "$(lib_cp)" TolerantClient.java "$LAB_HOME/tests/expected/shelf-u1.json" 2>/dev/null | tr '\n' ' ')
[[ "$out" == "b1 FINISHED b6 READING b10 WANT_TO_READ " ]] && pass "TolerantClient still reads v1 responses" || fail "TolerantClient on a v1 response" "got: $out"
notes_check notes.md 60 m03/05
finish

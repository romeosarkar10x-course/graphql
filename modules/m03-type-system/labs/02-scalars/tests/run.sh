#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file Scalars.java || finish
cd "$LAB_DIR"
check() { # name query vars
  timeout 60 java -cp "$(lib_cp)" Scalars.java "$LAB_HOME/tests/$2.graphql" "$3" >/tmp/out.json 2>/tmp/err.txt
  if json_equal /tmp/out.json "$LAB_HOME/tests/expected/$1.json"; then pass "$1"
  else fail "$1 differs (< yours, > expected)"; json_diff /tmp/out.json "$LAB_HOME/tests/expected/$1.json" | head -8
       [[ -s /tmp/err.txt ]] && tail -2 /tmp/err.txt | sed 's/^/        /'; fi
}
check big-id q-big '{}'
check small-id q-small '{}'
check malformed-id q-bad '{}'
check stats q-stats '{}'
check id-as-json-number q-var '{"id": 9007199254740993}'
lines=$(java -cp "$(lib_cp)" /course/tools/gql/Gql.java schema-lines schema.graphqls 2>/dev/null)
grep -qx 'AnalyticsReview.id: ID!' <<<"$lines" && pass "AnalyticsReview.id is ID!" || fail "AnalyticsReview.id should be an ID! (opaque string), not an Int"
grep -qx 'Stats.totalPageViews: Long!' <<<"$lines" && pass "Stats.totalPageViews is Long!" || fail "Stats.totalPageViews should use the extended-scalars Long"
grep -qx 'scalar Long' <<<"$lines" && grep -q 'specifiedBy' schema.graphqls && pass "scalar Long declared with @specifiedBy" || fail "declare scalar Long with @specifiedBy(url: ...)"
notes_check notes.md 60 m03/02
finish

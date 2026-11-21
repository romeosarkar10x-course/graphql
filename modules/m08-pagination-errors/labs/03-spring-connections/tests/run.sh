#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
java -cp "$(lib_cp)" /course/modules/m08-pagination-errors/contract/Catalog.java m08_03 >/dev/null || { fail "could not load the catalogue into m08_03"; finish; }
app_build target/shelfie-connections.jar || finish
app_start java -jar target/shelfie-connections.jar || finish
app_hurl "$LAB_HOME/tests/lab.hurl" "connections (lab.hurl)"

# Opaque cursors: no Java class names, no JSON, after base64.
cursor=$(curl -s -H 'Content-Type: application/json' -d '{"query":"{ books(first: 1) { edges { cursor } } }"}' "$URL" | jq -r '.data.books.edges[0].cursor // empty')
plain=$(printf '%s' "$cursor" | tr -- '-_' '+/' | base64 -d 2>/dev/null)
if [[ -n "$cursor" ]] && ! grep -qiE 'java|\{|\[' <<<"$plain"; then pass "cursors are opaque and carry no Java types"
else fail "cursors are opaque and carry no Java types" "this one decodes to: ${plain:0:80}"; fi
app_stop

# One row-value range in the SQL, both directions.
if grep -qE '\(\s*[a-z0-9_]+\.published\s*,\s*[a-z0-9_]+\.id\s*\)\s*>' /tmp/app.log && grep -qE '\(\s*[a-z0-9_]+\.published\s*,\s*[a-z0-9_]+\.id\s*\)\s*<' /tmp/app.log; then
  pass "the SQL seeks with a row-value comparison, forward and backward"
else fail "the SQL seeks with a row-value comparison, forward and backward" "$(grep -m1 -oE 'where .{0,90}' /tmp/app.log)"; fi
if grep -qE '^\s*type\s+(BookConnection|BookEdge|PageInfo)\b' src/main/resources/graphql/*.graphqls; then
  fail "BookConnection, BookEdge and PageInfo are generated, not written in the schema"
else pass "BookConnection, BookEdge and PageInfo are generated, not written in the schema"; fi
finish

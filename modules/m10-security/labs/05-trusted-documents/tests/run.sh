#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
T="java /course/modules/m10-security/contract/Token.java"
OPS="$LAB_HOME/starter/client/operations"   # the course's copy: ids must match these bodies
sha() { tr -d '\n' <<<"$(sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' "$1")" | sha256sum | cut -d' ' -f1; }

# The client build: your BuildManifest writes the same manifest you committed, with the right ids.
if java -cp "$(lib_cp)" client/BuildManifest.java "$OPS" /tmp/manifest.json >/dev/null 2>&1; then
  want=$(for f in "$OPS"/*.graphql; do sha "$f"; done | sort | tr '\n' ' ')
  got=$(jq -r '.operations[].id' /tmp/manifest.json | sort | tr '\n' ' ')
  [[ "$got" == "$want" ]] && jq -e '[.operations[] | select(.body | length > 0)] | length == 8' /tmp/manifest.json >/dev/null \
    && pass "BuildManifest: 8 operations, ids are the SHA-256 of each body" || fail "BuildManifest: 8 operations, ids are the SHA-256 of each trimmed body"
else fail "BuildManifest runs: bin/course java client/BuildManifest.java client/operations out.json"; fi
[[ -s src/main/resources/trusted-documents.json && -s /tmp/manifest.json ]] && cmp -s <(jq -S . /tmp/manifest.json 2>/dev/null) <(jq -S . src/main/resources/trusted-documents.json 2>/dev/null) \
  && pass "the committed manifest is the build's output" || fail "src/main/resources/trusted-documents.json is up to date with client/operations"

app_build target/shelfie-secure.jar || finish
app_start java -jar target/shelfie-secure.jar || finish
home_text=$(sed 's/"/\\"/g' "$OPS/Home.graphql" | tr -d '\n')
app_hurl "$LAB_HOME/tests/trusted.hurl" "trusted documents (trusted.hurl)" \
  --variable "ada=$($T ada)" --variable "home=$(sha "$OPS/Home.graphql")" --variable "club=$(sha "$OPS/Club.graphql")" \
  --variable "addreview=$(sha "$OPS/AddReview.graphql")" --variable "home_text=$home_text" \
  --variable "evil=$(printf '%s' '{ readers(first: 40) { email } }' | sha256sum | cut -d' ' -f1)"
app_stop
# Development mode lets ad-hoc queries through, but introspection stays off.
app_start java -jar target/shelfie-secure.jar --shelfie.trusted-documents.enforce=false || finish
adhoc=$(curl -s -H 'Content-Type: application/json' -d '{"query":"{ books(first: 1) { id } }"}' "$URL" | jq -r '.data.books[0].id // empty')
intro=$(curl -s -H 'Content-Type: application/json' -d '{"query":"{ __schema { types { name } } }"}' "$URL")
[[ "$adhoc" == b1 ]] && pass "enforce=false lets ad-hoc queries run (local development)" || fail "shelfie.trusted-documents.enforce=false lets ad-hoc queries run"
jq -e '.errors and (.data.__schema.types | not)' <<<"$intro" >/dev/null && pass "introspection is off, even in development mode" \
  || fail "introspection is off (spring.graphql.schema.introspection.enabled)"
app_stop
notes_check notes.md 60 m10/05
finish

#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file hello.hurl || finish
f="$LAB_DIR/hello.hurl"

run_hurl "$f"

grep -qE 'jsonpath "\$\.data\.hello" *== *"Hello from Shelfie!"' "$f" \
  && pass "asserts the exact value of data.hello" \
  || fail "no exact assertion on data.hello" "a test that asserts nothing passes against anything"

grep -qE 'jsonpath "\$\.errors" +not exists' "$f" \
  && pass "asserts there are no errors" \
  || fail "no assertion that \$.errors does not exist" "GraphQL returns HTTP 200 even when a query fails validation; see the lecture"

# A test that passes against nothing is broken: run it against a dead port.
if hurl --test --variable ref=http://localhost:1 "$f" >/dev/null 2>&1; then
  fail "hello.hurl passes even with no server running"
else pass "hello.hurl fails when there is no server"; fi

grep -q '^## 2' "$LAB_DIR/notes.md" 2>/dev/null && [[ $(wc -w < "$LAB_DIR/notes.md") -gt 90 ]] \
  && pass "notes.md filled in (rubric-graded: bin/course grade m00/01)" \
  || fail "notes.md not filled in yet"
finish

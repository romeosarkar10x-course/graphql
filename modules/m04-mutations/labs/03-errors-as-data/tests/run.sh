#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
require_file ReviewClient.java || finish
ref_reset_data
timeout 60 java -cp "$(lib_cp)" ReviewClient.java >/tmp/rc.txt 2>/tmp/rc.err
if diff -q /tmp/rc.txt "$LAB_HOME/tests/expected/review-client.txt" >/dev/null; then pass "both styles map every outcome to the same sealed result"
else fail "output differs (< yours, > expected)"; diff /tmp/rc.txt "$LAB_HOME/tests/expected/review-client.txt" | sed 's/^/        /' | head -12; tail -2 /tmp/rc.err | sed 's/^/        /'; fi
grep -qE 'sealed\s+interface\s+CreateReviewResult' ReviewClient.java && pass "CreateReviewResult is sealed" || fail "keep CreateReviewResult a sealed interface"
ref_reset_data
notes_check notes.md 60 m04/03
finish

#!/usr/bin/env bash
source /course/tools/testlib.sh
source "$LAB_HOME/../../contract/app-test.sh"
cd "$LAB_DIR" || exit 1
app_build target/shelfie-data.jar || finish
app_start java -jar target/shelfie-data.jar || finish
app_hurl "$LAB_HOME/tests/lab.hurl" "generated and hand-written fetchers (lab.hurl)"
app_stop
grep -rqE '@QueryMapping[^}]*\b(book|books)\(' src/main/java && fail "book and books must come from @GraphQlRepository, not a controller" || pass "book and books are generated, not hand-written"
grep -q 'select' /tmp/app.log && pass "the SQL is in the log (spring.jpa.show-sql)" || fail "turn on spring.jpa.show-sql: notes.md compares the SQL"
notes_check notes.md 60 m06/05
finish

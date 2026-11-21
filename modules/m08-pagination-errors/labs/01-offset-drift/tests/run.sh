#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
catalog() { java -cp "$(lib_cp)" /course/modules/m08-pagination-errors/contract/Catalog.java m08_01 >/dev/null; }
catalog || { fail "could not load the catalogue into m08_01"; finish; }
app_build target/shelfie-offset.jar || finish
app_start java -jar target/shelfie-offset.jar || finish
app_hurl "$LAB_HOME/tests/lab.hurl" "offset paging (lab.hurl)"
# The experiment, with the harness's own copy of Drift.java: offset paging must drift by exactly 3 per page.
for mode in insert delete; do
  out=$(java -cp "$(lib_cp)" "$LAB_HOME/tests/Drift.java" $mode 5 20 "$URL" 2>&1 | tail -1)
  case "$mode:$out" in
    insert:*"duplicates 12, gaps 0") pass "drift with inserts: $out" ;;
    delete:*"duplicates 0, gaps 12") pass "drift with deletes: $out" ;;
    *) fail "drift with ${mode}s: expected 12 $([[ $mode == insert ]] && echo duplicates || echo gaps)" "$out" ;;
  esac
done
app_stop
catalog
if grep -qE 'offset \? rows fetch first \? rows only' /tmp/app.log; then pass "the database does the paging (OFFSET ... FETCH FIRST in the SQL)"
else fail "the database does the paging (OFFSET ... FETCH FIRST in the SQL)" "use findAll(Pageable): the starter loads all 10,000 books per request"; fi
notes_check notes.md 60 m08/01
finish

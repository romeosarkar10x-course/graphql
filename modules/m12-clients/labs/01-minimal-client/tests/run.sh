#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
ref_up || { fail "the reference server is not up: run bin/course ref up"; finish; }
require_file ShelfieClient.java "your client" || finish
T=$(mktemp -d); trap 'rm -rf $T' EXIT
cp ShelfieClient.java "$LAB_HOME/tests/Check.java" $T/
java -cp "$(lib_cp)" $T/Check.java "$REF/graphql" "$LAB_DIR/documents" > $T/out 2> $T/err
while IFS=$'\t' read -r line detail; do
  case "$line" in PASS\ *) pass "${line#PASS }" ;; FAIL\ *) fail "${line#FAIL }" "$detail" ;; esac
done < $T/out
grep -q . $T/out || fail "Check.java did not run" "$(grep -E 'error|Exception' $T/err | head -5)"
notes_check notes.md 60 m12/01
finish

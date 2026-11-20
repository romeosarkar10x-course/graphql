#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
gql() { java -cp "$(lib_cp)" /course/tools/gql/Gql.java "$@"; }
if mvn -q package >/tmp/r2s-build.log 2>&1 && [[ -f target/records-to-sdl.jar ]]; then pass "mvn package builds target/records-to-sdl.jar"
else fail "mvn package failed"; tail -20 /tmp/r2s-build.log | sed 's/^/        /'; finish; fi
gen() { timeout 30 java -jar target/records-to-sdl.jar "$@"; }

for root in Query; do
  if gen "dev.shelfie.fixtures.Shelfie\$$root" >/tmp/$root.graphqls 2>/tmp/err.txt && gql schema-lines /tmp/$root.graphqls >/tmp/$root.lines 2>/tmp/err2.txt; then
    if diff -q /tmp/$root.lines "$LAB_HOME/tests/expected/$root.lines" >/dev/null; then pass "root $root: generated SDL matches the expected schema"
    else fail "root $root: schema differs (< yours, > expected, as schema-lines)"; diff /tmp/$root.lines "$LAB_HOME/tests/expected/$root.lines" | head -14 | sed 's/^/        /'; fi
  else fail "root $root: generation failed or the SDL does not parse" "$(cat /tmp/err.txt /tmp/err2.txt | head -3)"; fi
done
names=$(gen 'dev.shelfie.fixtures.Shelfie$Review' 2>/dev/null | awk '/^(type|enum|union|interface) /{print $2}' | tr '\n' ' ')
[[ "$names" == "Book Club Node Review User Visibility " ]] && pass "root Review: exactly the types it reaches (User implements Node, which pulls in every Node), sorted" || fail "root Review should reach Review, User, then Node and all its implementations; sorted by name" "got: $names"
ok=1; for d in '"A reading group."' '"Members only."' '"All clubs, in seed order."' '"Anything with a global id."'; do
  grep -qF "$d" /tmp/Query.graphqls || { ok=0; fail "description $d missing from the SDL"; }
done
(( ok )) && pass "@Description becomes SDL descriptions (types, fields, enum values, interfaces)"
gen 'dev.shelfie.fixtures.Shelfie$Analytics' >/dev/null 2>/tmp/err.txt; code=$?
[[ $code == 1 ]] && grep -q 'Analytics.pageViews' /tmp/err.txt && pass "long is refused with an error naming Analytics.pageViews (exit 1)" \
  || fail "a long component should fail with exit 1 and name Analytics.pageViews" "exit $code: $(head -1 /tmp/err.txt)"
lines=$(grep -cvE '^\s*(//.*|\*.*|/\*.*)?$' src/main/java/dev/shelfie/sdl/SdlGenerator.java)
note "SdlGenerator.java: $lines lines of code (the brief says about 200)"
notes_check DESIGN.md 60 m03/13
finish

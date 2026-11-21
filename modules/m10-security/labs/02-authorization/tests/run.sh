#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
T="java /course/modules/m10-security/contract/Token.java"
app_build target/shelfie-secure.jar || finish
app_start java -jar target/shelfie-secure.jar || finish
vars=(); for u in ada ben max olu root; do vars+=(--variable "$u=$($T $u)"); done
app_hurl "$LAB_HOME/tests/forbidden.hurl" "authorization (forbidden.hurl)" "${vars[@]}"
app_stop
if [[ -f src/test/resources/my-forbidden.hurl ]] && (( $(grep -cE '^# [A-Z][0-9]+' src/test/resources/my-forbidden.hurl) >= 10 )); then
  app_start java -jar target/shelfie-secure.jar && app_hurl src/test/resources/my-forbidden.hurl "your own forbidden-path suite (my-forbidden.hurl)" "${vars[@]}"
  app_stop
else fail "write your own forbidden-path suite: src/test/resources/my-forbidden.hurl, 10+ titled entries (# F01 ...)" "the experiment: try every forbidden path for member, moderator and owner"; fi
src=$(cat src/main/java/dev/shelfie/secure/*.java)
grep -q '@PreAuthorize' <<<"$src" && pass "writes are guarded with @PreAuthorize" \
  || fail "writes are guarded with @PreAuthorize" "lecture 10.2: put the rule on the service method, not only in the controller"
grep -q 'SchemaDirectiveWiring' <<<"$src" && grep -q '@auth(role: MODERATOR)' src/main/resources/graphql/*.graphqls \
  && pass "@auth(role:) is enforced by a SchemaDirectiveWiring" || fail "@auth(role:) is enforced by a SchemaDirectiveWiring" "the directive is in the schema; wire it"
finish

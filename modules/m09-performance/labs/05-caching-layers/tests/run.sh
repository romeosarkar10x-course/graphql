#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
redis_flush() { exec 3<>/dev/tcp/redis/6379 && printf 'FLUSHALL\r\n' >&3 && read -r -t 5 line <&3 && exec 3>&-; [[ "$line" == +OK* ]]; }
fresh() {   # every scenario starts from the seed data, an empty Redis, an empty Varnish and a new JVM
  app_stop
  java -cp "$(lib_cp)" /course/modules/m09-performance/contract/ClubData.java m09_05 >/dev/null || { fail "could not load the club data into m09_05"; finish; }
  redis_flush || { fail "could not flush Redis (bin/course ref up starts it)"; finish; }
  app_start java -jar target/shelfie-perf.jar || finish
  python3 /course/tools/varnish.py 4000 >/dev/null || { fail "could not point Varnish at the app (bin/course ref up starts it)"; finish; }
}
app_build target/shelfie-perf.jar || finish
for n in 1 2 3 4 5; do
  fresh
  if hurl --test --variable "url=$URL" --variable "varnish=http://varnish:6081/graphql" "$LAB_HOME/tests/scenario-$n.hurl" >/tmp/hurl.log 2>&1
  then pass "scenario $n: no stale data"
  else fail "scenario $n: stale data" "$(grep -m1 -E 'actual:' /tmp/hurl.log | sed 's/^ *//')"; fi
done
# The caches must still cache: a repeated dashboard is a response-cache hit with no SQL at all.
fresh
D=$(jq -n --rawfile q /course/modules/m09-performance/contract/dashboard.graphql '{query: $q}')
curl -s -o /dev/null -H 'Content-Type: application/json' -d "$D" "$URL"
second=$(curl -s -D /tmp/h.txt -H 'Content-Type: application/json' -d "$D" "$URL")
if grep -qi '^X-Response-Cache: HIT' /tmp/h.txt && [[ $(jq '.extensions.sql.statements' <<<"$second") == 0 ]] \
   && [[ $(jq -cS 'del(.extensions)' <<<"$second" | md5sum) == $(jq -cS . /course/modules/m09-performance/contract/dashboard.expected.json | md5sum) ]]
then pass "the second dashboard is a response-cache hit: 0 statements, same data"
else fail "the second dashboard is a response-cache hit: 0 statements, same data" "$(grep -i '^X-Response-Cache' /tmp/h.txt) statements $(jq '.extensions.sql.statements' <<<"$second")"; fi
app_stop
java -cp "$(lib_cp)" /course/modules/m09-performance/contract/ClubData.java m09_05 >/dev/null; redis_flush
# Which layer was guilty: checked against hashes, so the key isn't printed.
right=0
for n in 1 2 3 4 5; do
  got=$(grep -E "^$n: *[a-z]+ *$" diagnosis.md 2>/dev/null | head -1 | tr -d ' ' | sha256sum | cut -c1-16)
  [[ "$got" == "$(sed -n "${n}p" "$LAB_HOME/tests/key.txt")" ]] && right=$((right + 1))
done
(( right == 5 )) && pass "diagnosis.md: the guilty layer for all 5 scenarios" || fail "diagnosis.md: the guilty layer for all 5 scenarios" "$right of 5 right"
notes_check diagnosis.md 120 m09/05
finish

#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
C=/course/modules/m09-performance/contract
java -cp "$(lib_cp)" $C/ClubData.java m09_09 >/dev/null || { fail "could not load the club data into m09_09"; finish; }
jq -n --rawfile q $C/dashboard.graphql '{query: $q}' > /tmp/dashboard.json
# The baseline: the harness's own naive app, built from the lab 9.1 reference.
base=/tmp/race-baseline; rm -rf $base; cp -r /course/modules/m09-performance/labs/01-seeing-n-plus-one/solution $base
sed -i 's/m09_01/m09_09/; s/show-sql: true .*/show-sql: false/' $base/src/main/resources/application.yaml
(cd $base && mvn -q package -DskipTests >/tmp/base-build.log 2>&1) || { fail "the baseline built" "$(grep -m2 ERROR /tmp/base-build.log)"; finish; }
app_build target/shelfie-perf.jar || finish
measure() {   # $1 jar -> "statements p95_ms", also checks the data
  app_start java -jar "$1" || return 1
  for _ in $(seq 50); do curl -s -o /dev/null -H 'Content-Type: application/json' -d @/tmp/dashboard.json "$URL"; done
  local r; r=$(curl -s -H 'Content-Type: application/json' -d @/tmp/dashboard.json "$URL")
  jq -cS 'del(.extensions)' <<<"$r" > /tmp/race-data.json
  for _ in $(seq 200); do curl -s -o /dev/null -w '%{time_total}\n' -H 'Content-Type: application/json' -d @/tmp/dashboard.json "$URL"; done | sort -n > /tmp/race-times.txt
  local mins; mins=$(for _ in 1 2 3; do curl -s -H 'Content-Type: application/json' -d @/tmp/dashboard.json "$URL" | jq '.extensions.sql.statements // -1'; done | sort -n | head -1)
  app_stop
  echo "$(jq '.extensions.sql.statements // -1' <<<"$r") $(awk 'NR==190 {printf "%.1f", $1 * 1000}' /tmp/race-times.txt) $mins"
}
read -r b_n b_p95 _ < <(measure $base/target/shelfie-perf.jar) || finish
cp /tmp/race-data.json /tmp/race-base-data.json
read -r n p95 min_n < <(measure target/shelfie-perf.jar) || finish
printf '        statements per dashboard   %-10s (naive: %s)\n        p95                        %-10s (naive: %s ms on this machine, now)\n' "$n" "$b_n" "$p95 ms" "$b_p95"
[[ $(md5sum < /tmp/race-data.json) == $(jq -cS . $C/dashboard.expected.json | md5sum) ]] && pass "the dashboard data is unchanged" \
  || fail "the dashboard data is unchanged" "compare with contract/dashboard.expected.json"
(( n >= 0 && n <= 9 )) && pass "9 or fewer statements per dashboard ($n)" || fail "9 or fewer statements per dashboard" "$n"
(( min_n >= 1 )) && pass "every request reaches the database" || fail "every request reaches the database" "a request ran $min_n statements: no response caches in this race"
awk -v p="$p95" -v b="$b_p95" 'BEGIN { exit !(p > 0 && p <= b / 2) }' && pass "p95 at most half the naive app's ($p95 ms vs $b_p95 ms)" \
  || fail "p95 at most half the naive app's" "$p95 ms vs $b_p95 ms"
rm -rf $base
finish

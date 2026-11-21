#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
C=/course/modules/m09-performance/contract; L=/course/modules/m09-performance/labs
java -cp "$(lib_cp)" $C/ClubData.java m09_10 >/dev/null || { fail "could not load the club data into m09_10"; finish; }
build() {   # $1 reference lab, $2 dir
  rm -rf "$2"; cp -r "$L/$1/solution" "$2"
  sed -i 's/m09_0[0-9]/m09_10/; s/show-sql: true .*/show-sql: false/' "$2/src/main/resources/application.yaml"
  (cd "$2" && mvn -q package -DskipTests >/tmp/guess-build.log 2>&1) || { fail "the $1 reference built" "$(grep -m2 ERROR /tmp/guess-build.log)"; finish; }
}
build 01-seeing-n-plus-one /tmp/guess-naive; build 03-batch-loading /tmp/guess-batched
declare -A actual
for app in naive batched; do
  app_start java -jar /tmp/guess-$app/target/shelfie-perf.jar || finish
  n=0
  while IFS='|' read -r which q; do
    n=$((n + 1)); [[ $which == "$app" ]] || continue
    if [[ $q == DASHBOARD ]]; then body=$(jq -n --rawfile q $C/dashboard.graphql '{query: $q}'); else body=$(jq -n --arg q "$q" '{query: $q}'); fi
    actual[$n]=$(curl -s -m 20 -H 'Content-Type: application/json' -d "$body" "$URL" | jq '.extensions.sql.statements // -1')
  done < "$LAB_HOME/tests/rounds.txt"
  app_stop
done
score=0; exact=0
for n in $(seq 10); do
  g=$(grep -E "^$n:" guesses.txt 2>/dev/null | head -1 | cut -d: -f2 | tr -dc '0-9')
  a=${actual[$n]}
  if [[ -z "$g" ]]; then r=miss
  elif (( g == a )); then r=exact; score=$((score + 2)); exact=$((exact + 1))
  elif (( (g - a) * (g - a) * 100 <= a * a )); then r=close; score=$((score + 1))
  else r=miss; fi
  printf '        round %-2d %s\n' "$n" "$r"
done
echo "        score $score of 20 ($exact exact)"
(( score >= 14 )) && pass "score $score of 20 (14 to pass)" || fail "score $score of 20 (14 to pass)" "work out each miss with lab 9.1's counter"
rm -rf /tmp/guess-naive /tmp/guess-batched
finish

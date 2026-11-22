#!/usr/bin/env bash
# Game 16.8: the answers for the seed you played (checked against the game's key), and the incident report.
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
[[ -f answers.json ]] && jq -e . answers.json >/dev/null 2>&1 || { fail "answers.json is JSON"; finish; }
seed=$(jq -r '.seed' answers.json)
[[ $seed =~ ^[0-9]+$ ]] && (( seed > 0 )) || { fail "answers.json: the seed you played (gameday.sh prints it)"; finish; }
W=$(mktemp -d); trap 'rm -rf $W' EXIT
sed 's/    static Map<String, String> answers/    public static Map<String, String> answers/' /course/modules/m16-production/contract/GameTraffic.java > $W/GameTraffic.java
printf 'public class Key { public static void main(String[] a) { var m = GameTraffic.answers(Long.parseLong(a[0]));\n  System.out.println(m.get("slowField") + " " + m.get("leakingClient") + " " + m.get("abusiveClient")); } }\n' > $W/Key.java
javac -d $W/out $W/GameTraffic.java $W/Key.java 2>/dev/null || { fail "the answer key compiles"; finish; }
read -r slow leak abuse < <(java -cp $W/out Key "$seed")
[[ $(jq -r .slowField answers.json) == "$slow" ]] && pass "incident 1: the slow field" || fail "incident 1: the slow field is not $(jq -r .slowField answers.json)"
[[ $(jq -r .leakingClient answers.json) == "$leak" ]] && pass "incident 2: the client whose subscriptions leak" || fail "incident 2: the leaking client is not $(jq -r .leakingClient answers.json)"
[[ $(jq -r .abusiveClient answers.json) == "$abuse" ]] && pass "incident 3: the abusive client" || fail "incident 3: the abusive client is not $(jq -r .abusiveClient answers.json)"
if require_file INCIDENTS.md "the incident report (rubric)"; then
  grep -q "seed $seed" INCIDENTS.md && pass "the report is for seed $seed" || fail "INCIDENTS.md: say which seed (a line with 'seed $seed')"
  n=$(grep -cE '(rate|sum|histogram_quantile|topk)\(|\{ *(name|span\.|resource\.|duration)' INCIDENTS.md)
  (( n >= 3 )) && pass "evidence: $n PromQL or TraceQL queries quoted" || fail "INCIDENTS.md: quote the PromQL or TraceQL that showed each incident (found $n)"
  notes_check INCIDENTS.md 250 m16/08
fi
finish

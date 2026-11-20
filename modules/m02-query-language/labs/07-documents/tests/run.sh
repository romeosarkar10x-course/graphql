#!/usr/bin/env bash
source /course/tools/testlib.sh
GQL="java -cp $(lib_cp) /course/tools/gql/Gql.java"
ops="$LAB_DIR/ops"
[[ -d "$ops" ]] || { fail "ops/ folder is missing"; finish; }

want=(Hello ClubCard ClubPage UserProfile BookDetails CompareClubs SearchEverything BookSearch ClubList UpcomingMeetups ClubCardVariant UserReviews)
declare -A seen=()
features=""
n=0
for f in "$ops"/*.graphql; do
  [[ -e "$f" ]] || continue
  n=$((n+1)); b=$(basename "$f")
  [[ "$b" =~ ^[0-9]{2}-[a-z0-9-]+\.graphql$ ]] || fail "$b: name files NN-kebab-case.graphql"
  info=$($GQL operations "$f" 2>&1) || { fail "$b: $info"; continue; }
  count=$(grep -c . <<<"$info")
  if [[ "$count" != 1 ]]; then fail "$b: one operation per file, found $count"; continue; fi
  read -r type name rest <<<"$info"
  [[ "$name" == "(anonymous)" ]] && { fail "$b: the operation needs a name"; continue; }
  [[ -n "${seen[$name]:-}" ]] && fail "$b: operation name $name is already used by ${seen[$name]}"
  seen[$name]=$b
  features+=" ${rest#uses=}"
  grep -qE '^# Description: .{10,}' "$f" || fail "$b: start with a '# Description: ...' comment (what screen or job it serves)"
  vars="{}"; [[ -f "${f%.graphql}.json" ]] && vars=$(cat "${f%.graphql}.json")
  out=$(jq -Rs --argjson v "$vars" --arg op "$name" '{query: ., variables: $v, operationName: $op}' "$f" \
    | curl -sS -H 'Content-Type: application/json' --data-binary @- "$REF/graphql")
  if jq -e '.errors' >/dev/null <<<"$out"; then fail "$b ($name): $(jq -r '.errors[0].message' <<<"$out")" "variables: $vars (put them in ${b%.graphql}.json)"
  elif jq -e '.data | to_entries | all(.value != null)' >/dev/null <<<"$out"; then pass "$b ($name) runs"
  else fail "$b ($name): a root field returned null; pick variables that find something"; fi
done
(( n >= 12 )) && pass "$n operation files" || fail "found $n operation files in ops/, need 12"
missing=(); for w in "${want[@]}"; do [[ -n "${seen[$w]:-}" ]] || missing+=("$w"); done
(( ${#missing[@]} == 0 )) && pass "all 12 required operations are there" || fail "missing operations: ${missing[*]}"
for feat in alias fragment-spread inline-fragment directive; do
  [[ " $features " == *"$feat"* ]] && pass "some operation uses: $feat" || fail "no operation uses: $feat"
done

m="$LAB_DIR/multi.graphql"
names=$($GQL operations "$m" 2>/dev/null | awk '$2 != "(anonymous)" {print $2}')
if [[ $(grep -c . <<<"$names") -ge 2 ]]; then
  out=$(jq -Rs '{query: .}' "$m" | curl -sS -H 'Content-Type: application/json' --data-binary @- "$REF/graphql")
  jq -e '.errors[0].message | test("operation name")' >/dev/null <<<"$out" && pass "multi.graphql without operationName is rejected" \
    || fail "multi.graphql without operationName should be rejected" "got: $(jq -c . <<<"$out" | cut -c1-120)"
  first=$(head -1 <<<"$names")
  out=$(jq -Rs --arg op "$first" '{query: ., operationName: $op}' "$m" | curl -sS -H 'Content-Type: application/json' --data-binary @- "$REF/graphql")
  jq -e '.data and (.errors | not)' >/dev/null <<<"$out" && pass "multi.graphql with operationName $first runs" || fail "multi.graphql with operationName $first failed"
else fail "multi.graphql needs two named operations"; fi

lines=$(grep -cvE '^\s*(#.*)?$' "$LAB_DIR/notes.md" 2>/dev/null || echo 0)
(( lines >= 9 )) && pass "notes.md filled in (rubric: bin/course grade m02/07)" || fail "notes.md: answer the questions"
finish

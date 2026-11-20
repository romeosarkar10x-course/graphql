#!/usr/bin/env bash
source /course/tools/testlib.sh
GQL="java -cp $(lib_cp) /course/tools/gql/Gql.java"
# Par: the shortest answer the course author found. Beat it and tell everyone.
declare -A PAR=([01]=7 [02]=13 [03]=35 [04]=44 [05]=22 [06]=45 [07]=46 [08]=22)

bytes() { local n; n=$(wc -c <"$1"); [[ "$(tail -c1 "$1" | od -An -c | tr -d ' ')" == '\n' ]] && n=$((n-1)); echo "$n"; }
strokes=0; par=0; holed=0
for h in 01 02 03 04 05 06 07 08; do
  f="$LAB_DIR/holes/$h.graphql"
  par=$((par + PAR[$h]))
  if [[ ! -s "$f" ]]; then fail "hole $h: empty"; continue; fi
  out=$(jq -Rs '{query: .}' "$f" | curl -sS -H 'Content-Type: application/json' --data-binary @- "$REF/graphql")
  if jq -e '.errors' >/dev/null <<<"$out"; then fail "hole $h: $(jq -r '.errors[0].message' <<<"$out" | cut -c1-100)"; continue; fi
  jq -c .data <<<"$out" >/tmp/got.json
  if json_equal /tmp/got.json "$LAB_HOME/tests/targets/$h.json"; then
    n=$(bytes "$f"); strokes=$((strokes + n)); holed=$((holed+1))
    pass "hole $h: $n characters (par ${PAR[$h]})"
  else fail "hole $h: data differs from tests/targets/$h.json"; json_diff /tmp/got.json "$LAB_HOME/tests/targets/$h.json"; fi
done

# GitHub holes: checked live against GitHub's REST API when GITHUB_TOKEN is set.
gh() { curl -sS -H "Authorization: Bearer $GITHUB_TOKEN" -H 'Content-Type: application/json' --data-binary @- https://api.github.com/graphql; }
for h in g1 g2; do
  f="$LAB_DIR/holes/$h.graphql"
  [[ -s "$f" ]] || { note "hole $h: empty (bonus hole, not scored)"; continue; }
  if [[ -z "${GITHUB_TOKEN:-}" ]]; then
    v=$($GQL validate "$f" --schema /course/tools/schemas/github-2026-11-21.graphql 2>&1 | head -1)
    note "hole $h: GITHUB_TOKEN not set, so only validated against GitHub's schema: $v"; continue
  fi
  case $h in
    g1) want=$(curl -sS https://api.github.com/repos/graphql/graphql-spec | jq -c '{repository: {stargazerCount: .stargazers_count}}') ;;
    g2) want=$(curl -sS https://api.github.com/users/torvalds | jq -c '{user: {name, company}}') ;;
  esac
  got=$(jq -Rs '{query: .}' "$f" | gh | jq -c .data)
  if [[ "$(jq -S . <<<"$got")" == "$(jq -S . <<<"$want")" ]]; then n=$(bytes "$f"); pass "bonus hole $h: $n characters"
  else fail "bonus hole $h: got $got, want $want"; fi
done

if (( holed == 8 )); then note "strokes: $strokes over 8 holes (par $par). Record it in games/leaderboards/graphql-golf.md"
else note "$holed/8 holes done; strokes count once all 8 are in"; fi
finish

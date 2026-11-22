#!/usr/bin/env bash
# Lab 16.3: the schema split per type with an owner each (CODEOWNERS), a review template, Book.isbn10 removed with
# usage evidence, Club.memberCount kept. The app must still serve the 7-day replay, minus the retired client.
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m16-production/contract/ops.sh
source /course/modules/m16-production/contract/lab-app.sh
cd "$LAB_DIR" || exit 1
trap 'app_stop; ops_stop' EXIT
G=src/main/resources/graphql
OLD=$OPS/ops-app/$G/shelfie.graphqls
types=$(cat $G/*.graphqls $G/**/*.graphqls 2>/dev/null | grep -oE '^(type|input|enum|interface|union|scalar) [A-Za-z_]+' | awk '{print $2}' | sort -u)
missing=(); for t in $types; do [[ -f $G/types/$t.graphqls ]] || missing+=("$t"); done
[[ -n "$types" && ${#missing[@]} == 0 && ! -f $G/shelfie.graphqls ]] && pass "one file per type under $G/types/" \
  || fail "one file per type: $G/types/<Type>.graphqls (and no shelfie.graphqls)" "missing: ${missing[*]:-all}"
CO=$( [[ -f .github/CODEOWNERS ]] && echo .github/CODEOWNERS || echo CODEOWNERS )
if [[ -f $CO ]]; then
  noowner=(); for f in $G/types/*.graphqls; do
    grep -E "^/?$f[[:space:]]+(@[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+|@[A-Za-z0-9_-]+)" "$CO" >/dev/null || noowner+=("$(basename "$f")"); done
  owners=$(grep -E "^/?$G/types/" "$CO" | awk '{print $2}' | sort -u | wc -l)
  [[ ${#noowner[@]} == 0 ]] && pass "CODEOWNERS names an owner for every type file" || fail "CODEOWNERS: no line for ${noowner[*]}"
  (( owners >= 3 )) && pass "ownership is spread: $owners owner teams" || fail "CODEOWNERS: one team owning everything is not ownership per type ($owners teams)"
else fail "CODEOWNERS (.github/CODEOWNERS)"; fi
PT=.github/pull_request_template.md
if [[ -f $PT ]]; then
  miss=(); for k in breaking deprecat usage owner changelog; do grep -qi "$k" $PT || miss+=("$k"); done
  boxes=$(grep -cE '^\s*- \[ \]' $PT)
  [[ ${#miss[@]} == 0 && $boxes -ge 5 ]] && pass "the schema-review template ($boxes checks)" \
    || fail "$PT: needs sections on breaking changes, deprecations, usage evidence, owners and the changelog, and 5+ '- [ ]' checks" "missing: ${miss[*]}; checks: $boxes"
else fail "the schema-review template ($PT)"; fi
diff=$(java -cp "$LIB/*" /course/tools/design/Design.java diff "$OLD" $G/types 2>&1)
br=$(grep -c '^BREAKING' <<<"$diff")
[[ $br == 1 ]] && grep -q 'isbn10' <<<"$diff" && pass "schema diff: exactly one breaking change, Book.isbn10 removed" \
  || fail "schema diff from the contract schema: exactly one breaking change (Book.isbn10)" "$(grep -E '^(BREAKING|DANGEROUS)' <<<"$diff" | head -4 | tr '\n' ';')"
lab_app_up --management.tracing.enabled=false || finish
B=$(gql_post '{"query":"{ __type(name: \"Book\") { fields(includeDeprecated: true) { name } } }"}')
C=$(gql_post '{"query":"{ __type(name: \"Club\") { fields(includeDeprecated: true) { name isDeprecated deprecationReason } } }"}')
! jq -e '.data.__type.fields[] | select(.name == "isbn10")' <<<"$B" >/dev/null && jq -e '.data.__type.fields[] | select(.name == "isbn13")' <<<"$B" >/dev/null \
  && pass "the running app no longer has Book.isbn10" || fail "Book.isbn10 still served (or isbn13 gone)"
jq -e '.data.__type.fields[] | select(.name == "memberCount" and .isDeprecated)' <<<"$C" >/dev/null \
  && pass "Club.memberCount stays (deprecated): iOS 2.8.0 still uses it" || fail "Club.memberCount must stay, still deprecated: the usage data shows a client using it this week"
java -cp "$LIB/*" "$OPS/Traffic.java" >/tmp/traffic.log 2>&1
f=$(grep -oE '^failed: [0-9]+' /tmp/traffic.log | awk '{print $2}')
[[ $f == 60 ]] && pass "the 7-day replay still works: only the 60 calls from retired iOS 2.7.0 fail" \
  || fail "replay: expected exactly the 60 iOS 2.7.0 operations to fail" "failed: ${f:-?} ($(tail -2 /tmp/traffic.log | tr '\n' ' '))"
if [[ -f CHANGELOG.md ]] && grep -qE '^## [0-9]{4}-[0-9]{2}-[0-9]{2}' CHANGELOG.md && grep -q 'Book.isbn10' CHANGELOG.md; then
  pass "CHANGELOG.md: a dated entry for the removal"; else fail "CHANGELOG.md: a '## YYYY-MM-DD' entry that names Book.isbn10"; fi
require_file REMOVAL.md "the removal's evidence (rubric)" && notes_check REMOVAL.md 150 m16/03
finish

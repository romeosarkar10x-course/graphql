#!/usr/bin/env bash
# Lab 16.7: the 2027-01 changelog section, a migration guide whose queries validate against the right versions,
# the announcement's dates and contents, and the peer review.
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
V=/course/modules/m16-production/contract/versions
BREAKING=(Book.isbn10 Club.memberCount Mutation.addReview)
diff=$(java -cp "$LIB/*" /course/tools/design/Design.java diff $V/2026-10.graphqls $V/2027-01.graphqls)
[[ $(grep -c '^BREAKING' <<<"$diff") == 3 ]] || { fail "the contract's two versions differ by 3 breaking changes" "$diff"; finish; }
if [[ -f CHANGELOG.md ]]; then
  sec=$(awk '/^## 2027-01/{on=1; print; next} /^## /{on=0} on' CHANGELOG.md)
  miss=(); for c in "${BREAKING[@]}" visibility Book.subtitle Club.memberTotal ReviewVisibility; do grep -q "$c" <<<"$sec" || miss+=("$c"); done
  [[ -n "$sec" && ${#miss[@]} == 0 ]] && pass "CHANGELOG.md: a 2027-01 section with every breaking change and addition" \
    || fail "CHANGELOG.md: '## 2027-01' must name ${miss[*]:-everything}"
  grep -qiE '^#+ .*breaking' <<<"$sec" && pass "the breaking changes have their own heading" || fail "CHANGELOG.md 2027-01: put breaking changes under their own heading"
else fail "CHANGELOG.md"; fi
if [[ -f MIGRATION.md ]]; then
  miss=(); for c in Book.isbn10 Club.memberCount addReview; do grep -q "$c" MIGRATION.md || miss+=("$c"); done
  [[ ${#miss[@]} == 0 ]] && pass "MIGRATION.md covers the three breaking changes" || fail "MIGRATION.md does not mention ${miss[*]}"
  W=$(mktemp -d); awk -v d="$W" '/^```graphql/{n++; f=d"/q" n ".graphql"; on=1; next} /^```/{on=0} on{print > f}' MIGRATION.md
  old=0 new=0 bad=()
  for q in "$W"/q*.graphql; do
    [[ -f $q ]] || continue
    o=$(java -cp "$LIB/*" /course/tools/gql/Gql.java validate "$q" --schema $V/2026-10.graphqls >/dev/null 2>&1 && echo 1 || echo 0)
    n=$(java -cp "$LIB/*" /course/tools/gql/Gql.java validate "$q" --schema $V/2027-01.graphqls >/dev/null 2>&1 && echo 1 || echo 0)
    [[ $o$n == 10 ]] && old=$((old+1)); [[ $n == 1 ]] && new=$((new+1)); [[ $o$n == 00 ]] && bad+=("$(basename $q)")
  done
  [[ ${#bad[@]} == 0 ]] && pass "every graphql block in MIGRATION.md validates against 2026-10 or 2027-01" || fail "graphql blocks that validate against neither version: ${bad[*]}"
  (( old >= 3 && new >= 3 )) && pass "before and after for each change: $old queries only 2026-10 accepts, $new that 2027-01 accepts" \
    || fail "MIGRATION.md: show each breaking change before (valid on 2026-10 only) and after (valid on 2027-01)" "before: $old, after: $new (need 3 each)"
  rm -rf "$W"
else fail "MIGRATION.md"; fi
if [[ -f ANNOUNCEMENT.md ]]; then
  pub=$(grep -m1 -oE '^Published: *[0-9]{4}-[0-9]{2}-[0-9]{2}' ANNOUNCEMENT.md | grep -oE '[0-9-]{10}')
  end=$(grep -m1 -oE '^2026-10 supported until: *[0-9]{4}-[0-9]{2}-[0-9]{2}' ANNOUNCEMENT.md | grep -oE '[0-9]{4}-[0-9]{2}-[0-9]{2}$')
  if [[ -n $pub && -n $end ]]; then
    rel=$(date -d 2027-01-01 +%s); p=$(date -d "$pub" +%s 2>/dev/null || echo 0); e=$(date -d "$end" +%s 2>/dev/null || echo 0)
    (( rel - p >= 30*86400 )) && pass "announced at least 30 days before the release ($pub)" || fail "Published: $pub is less than 30 days before the 2027-01-01 release"
    (( e - rel >= 270*86400 )) && pass "2026-10 overlaps 2027-01 by at least nine months (until $end)" || fail "2026-10 supported until $end: give at least nine months of overlap after 2027-01-01"
  else fail "ANNOUNCEMENT.md: lines 'Published: YYYY-MM-DD' and '2026-10 supported until: YYYY-MM-DD'"; fi
  miss=(); for c in isbn10 memberCount addReview MIGRATION.md; do grep -q "$c" ANNOUNCEMENT.md || miss+=("$c"); done
  [[ ${#miss[@]} == 0 ]] && pass "the announcement names each change and links the migration guide" || fail "ANNOUNCEMENT.md does not mention ${miss[*]}"
  notes_check ANNOUNCEMENT.md 150 m16/07
else fail "ANNOUNCEMENT.md"; fi
require_file PEER-REVIEW.md "the peer review (rubric)" && notes_check PEER-REVIEW.md 80 m16/07
finish

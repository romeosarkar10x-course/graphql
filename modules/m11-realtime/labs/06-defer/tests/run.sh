#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m11-realtime/contract/live-test.sh
cd "$LAB_DIR" || exit 1
app_build target/shelfie-live.jar || finish
app_start java -jar target/shelfie-live.jar || finish
T=$(mktemp -d)
page=$(jq -nc --rawfile q $LIVE/club-page.graphql '{query: $q, variables: {id: "c1"}}')
gql "$page" >/dev/null   # warm-up
timing=$(curl -sN -o $T/mp -D $T/mp.h -w '%{time_starttransfer} %{time_total}' -H 'Content-Type: application/json' \
  -H 'Accept: multipart/mixed' -d "$page" "$URL")
read -r ttfb total <<<"$timing"
grep -iq '^content-type: multipart/mixed' $T/mp.h && grep -q '^-----' $T/mp \
  && pass "Accept: multipart/mixed gets a multipart/mixed response that ends with the closing delimiter" \
  || fail "Accept: multipart/mixed gets a multipart/mixed response that ends with the closing delimiter" "$(head -1 $T/mp.h | tr -d '\r') $(grep -i content-type $T/mp.h | tr -d '\r')"
grep '^{' $T/mp | tr -d '\r' > $T/parts
first=$(head -1 $T/parts); last=$(tail -1 $T/parts)
jq -e '.data.club.name == "Night Owls" and (.data.club.recommendations == null) and .hasNext == true' <<<"$first" >/dev/null 2>&1 \
  && pass "first part: the club page without recommendations, hasNext true" \
  || fail "first part: the club page without recommendations, hasNext true" "got: ${first:0:160}"
jq -e '.hasNext == false and .incremental[0].path == ["club"] and .incremental[0].label == "recommendations" and (.incremental[0].data.recommendations | length) == 3' <<<"$last" >/dev/null 2>&1 \
  && pass "last part: the deferred recommendations at path [\"club\"], label kept, hasNext false" \
  || fail "last part: the deferred recommendations at path [\"club\"], label kept, hasNext false" "got: ${last:0:200}"
awk -v t="$ttfb" -v a="$total" 'BEGIN { exit !(t < 0.6 && a >= 1.4) }' \
  && pass "time to first byte ${ttfb}s, while the whole response took ${total}s" \
  || fail "the first part arrives before the slow panel (want first byte < 0.6 s, total >= 1.4 s)" "first byte ${ttfb}s, total ${total}s"
json=$(curl -s -H 'Content-Type: application/json' -H 'Accept: application/json' -d "$page" "$URL")
jq -e '(.data.club.recommendations | length) == 3 and (has("hasNext") | not)' <<<"$json" >/dev/null 2>&1 \
  && pass "a client that accepts only JSON gets the whole page in one response, recommendations included" \
  || fail "a client that accepts only JSON gets the whole page in one response, recommendations included" "got: ${json:0:200}"
plain=$(curl -s -H 'Content-Type: application/json' -d "$page" "$URL")   # curl sends Accept: */*
jq -e '(.data.club.recommendations | length) == 3' <<<"$plain" >/dev/null 2>&1 \
  && pass "Accept: */* is a JSON client too" || fail "Accept: */* is a JSON client too" "got: ${plain:0:160}"
app_stop
notes_check notes.md 60 m11/06
finish

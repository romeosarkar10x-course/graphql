#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
JAR=target/shelfie-zoo.jar
app_build $JAR || finish
app_start java -jar $JAR --shelfie.clock=2026-11-26T12:00:00Z && app_hurl $LAB_HOME/tests/zoo.hurl "the zoo on a Thursday (zoo.hurl)"
app_stop
app_start java -jar $JAR --shelfie.clock=2026-07-09T23:30:00Z && app_hurl $LAB_HOME/tests/friday.hurl "Friday in London, Thursday in UTC (friday.hurl)"
app_stop
# Your exhibit: a directive of your own, declared and used on a field.
schema=$(cat src/main/resources/graphql/*.graphqls 2>/dev/null)
mine=$(grep -oE '^directive @[A-Za-z_]+' <<<"$schema" | sed 's/directive @//' | grep -vxE 'pirate|emojify|redactOnFridays')
used=$(for d in $mine; do grep -E "^\s+[A-Za-z_]+.*:.*@$d\b" <<<"$schema" >/dev/null && echo $d; done)
[[ -n "$used" ]] && pass "your exhibit: @$(echo $used | tr ' ' ',') declared and used on a field" \
  || fail "your exhibit: a directive of your own, declared and used on a field" "found declared: ${mine:-none}"
notes_check ZOO.md 60 m14/09
finish

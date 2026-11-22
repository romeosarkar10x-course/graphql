#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
JAR=target/shelfie-locales.jar
app_build $JAR || finish
app_start java -jar $JAR || finish
app_hurl $LAB_HOME/tests/locales.hurl "languages, zones and money (locales.hurl)"
# The experiment: one meetup in three locales and three zones.
note "meetup m1 for three readers in three zones (startsAtText):"
for l in en-US de-DE ja-JP; do
  row=$(for z in America/New_York Europe/Berlin Asia/Tokyo; do
    curl -s "$URL" -H 'Content-Type: application/json' -H "Accept-Language: $l" \
      -d "{\"query\":\"{ meetup(id: \\\"m1\\\") { startsAtText(timeZone: \\\"$z\\\") } }\"}" | jq -r '.data.meetup.startsAtText // "error"'
  done | paste -sd'|' | sed 's/|/  |  /g')
  note "  $l  $row"
done
notes_check notes.md 80 m14/02
finish

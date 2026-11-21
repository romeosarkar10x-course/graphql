#!/usr/bin/env bash
source /course/tools/testlib.sh
source "$LAB_HOME/../../contract/app-test.sh"
cd "$LAB_DIR" || exit 1
app_build target/shelfie-boot.jar || finish
app_start java -jar target/shelfie-boot.jar || finish
app_hurl "$LAB_HOME/tests/lab.hurl" "roleInClub, bookSearch, updateClub (lab.hurl)"
app_hurl "$LAB_HOME/../../contract/shelfie.hurl" "Shelfie on Spring (shelfie.hurl)"
app_stop
for a in '@LocalContextValue\(' '@Arguments +[A-Z]' 'ArgumentValue<'; do
  grep -rqE "$a" src/main/java && pass "uses ${a%%[\\ ]*}" || fail "use ${a%%[\\ ]*} (the lab is about it)"
done
notes_check notes.md 60 m06/02
finish

#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
JAR=target/shelfie-tenancy.jar
tok() { java /course/modules/m10-security/contract/Token.java "$@"; }
app_build $JAR || finish
app_start java -jar $JAR || finish
app_hurl $LAB_HOME/tests/tenancy.hurl "six paths to another tenant's data, and plan visibility (tenancy.hurl)" \
  --variable "acme=$(tok ada --claim tenant=acme)" --variable "globex=$(tok max --claim tenant=globex)" \
  --variable "notenant=$(tok ben)" --variable "forged=$(tok ada --claim tenant=acme --forged)"
# The database must refuse on its own, whatever the app's SQL says.
java -cp "$(lib_cp)" $LAB_HOME/tests/RlsCheck.java > /tmp/rls.txt 2>&1
while read -r status text; do
  case $status in ok) pass "$text" ;; FAIL) fail "${text%%: *}" "${text#*: }" ;; *) fail "the database checks ran" "$status $text" ;; esac
done < /tmp/rls.txt
notes_check notes.md 80 m14/03
finish

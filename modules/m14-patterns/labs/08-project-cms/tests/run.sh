#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
JAR=target/shelfie-cms.jar
java -cp "$(lib_cp)" /course/modules/m11-realtime/contract/Sql.java "DROP SCHEMA IF EXISTS m14_08 CASCADE" >/dev/null 2>&1
app_build $JAR || finish
app_start java -jar $JAR || finish
app_hurl $LAB_HOME/tests/cms.hurl "content types become an API (cms.hurl)"
app_stop
app_start java -jar $JAR || finish
app_hurl $LAB_HOME/tests/cms-restart.hurl "after a restart, and removing types (cms-restart.hurl)"
finish

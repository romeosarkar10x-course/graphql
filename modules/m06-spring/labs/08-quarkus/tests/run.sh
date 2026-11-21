#!/usr/bin/env bash
source /course/tools/testlib.sh
source "$LAB_HOME/../../contract/app-test.sh"
cd "$LAB_DIR" || exit 1
app_build target/quarkus-app/quarkus-run.jar || finish
app_start java -jar target/quarkus-app/quarkus-run.jar || finish
grep -oE "started in [0-9.]+s" /tmp/app.log | head -1 | sed "s/^/        Quarkus /"
app_hurl "$LAB_HOME/tests/lab.hurl" "Shelfie on Quarkus (lab.hurl)"
app_stop
notes_check notes.md 60 m06/08
finish

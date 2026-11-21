#!/usr/bin/env bash
source /course/tools/testlib.sh
source "$LAB_HOME/../../contract/app-test.sh"
cd "$LAB_DIR" || exit 1
app_build target/shelfie-boot.jar || finish
app_start java -jar target/shelfie-boot.jar || finish
app_hurl "$LAB_HOME/tests/lab.hurl" "tenant, extensions, @uppercase (lab.hurl)"
app_hurl "$LAB_HOME/../../contract/shelfie.hurl" "Shelfie on Spring (shelfie.hurl)"
app_stop
grep -rqE "getHeaders|HttpServletRequest" src/main/java/dev/shelfie/boot/LabController.java && fail "LabController should get the tenant from GraphQLContext, not from the HTTP request" || pass "the tenant comes from GraphQLContext"
notes_check notes.md 60 m06/04
finish

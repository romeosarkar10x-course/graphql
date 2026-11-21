#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
app_build target/shelfie-vulnerable.jar || finish
app_start java -Xmx512m -XX:+ExitOnOutOfMemoryError -jar target/shelfie-vulnerable.jar || finish
URL="$URL" bash "$LAB_HOME/tests/exploits.sh" > /tmp/exploits.log 2>&1
listening || { fail "the server survived the exploit suite" "it died: lower what one request may cost"; finish; }
leaks=$(grep -c '^LEAK' /tmp/exploits.log); closed=$(grep -c '^closed' /tmp/exploits.log)
(( closed == 13 )) && pass "all 13 exploits closed (12 flags and the CSRF chain)" \
  || { fail "every exploit closed: $leaks of 13 still work"; grep '^LEAK' /tmp/exploits.log | sed 's/^/        /'; }
app_stop
app_start java -jar target/shelfie-vulnerable.jar || finish
app_hurl "$LAB_HOME/tests/functional.hurl" "Shelfie still works (functional.hurl)"
app_stop
finish

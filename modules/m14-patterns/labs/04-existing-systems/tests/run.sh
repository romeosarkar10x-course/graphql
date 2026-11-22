#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
JAR=target/shelfie-bff.jar; FAKE=4500
app_build $JAR || finish
(cd /course/modules/m14-patterns/contract/openlibrary && exec java -cp "$(lib_cp)" FakeOpenLibrary.java data $FAKE) > /tmp/fake-ol.log 2>&1 &
FAKE_PID=$!; trap 'kill $FAKE_PID 2>/dev/null; app_stop' EXIT
for _ in $(seq 40); do curl -fs -o /dev/null localhost:$FAKE/_fake/stats && break; sleep 0.5; done
app_start java -jar $JAR --shelfie.openlibrary-url=http://localhost:$FAKE || finish
app_hurl $LAB_HOME/tests/bff.hurl "one page over REST, gRPC and a failing Open Library (bff.hurl)" --variable "fake=http://localhost:$FAKE"
notes_check notes.md 80 m14/04
finish

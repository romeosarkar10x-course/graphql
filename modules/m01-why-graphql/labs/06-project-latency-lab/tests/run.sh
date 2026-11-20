#!/usr/bin/env bash
source /course/tools/testlib.sh
require_file LatencyModel.java || finish
out=/tmp/latency-classes; rm -rf "$out"; mkdir -p "$out"
junit=$(ls "$LIB"/junit-platform-console-standalone-*.jar)
if javac -d "$out" -cp "$junit" "$LAB_DIR/LatencyModel.java" "$LAB_HOME/tests/LatencyModelTest.java" 2>/tmp/javac.log; then pass "compiles"
else fail "does not compile"; head -20 /tmp/javac.log | sed 's/^/        /'; finish; fi
if java -jar "$junit" execute --class-path "$out" --select-class LatencyModelTest --details=summary --disable-banner >/tmp/junit.log 2>&1; then
  pass "JUnit: $(grep -oE '[0-9]+ tests successful' /tmp/junit.log)"
else
  fail "JUnit: $(grep -oE '[0-9]+ tests failed' /tmp/junit.log)"
  grep -E 'MethodSource|=>' /tmp/junit.log | head -20 | sed 's/^/        /'
fi
[[ -s "$LAB_DIR/index.html" ]] && grep -qi '<input[^>]*range' "$LAB_DIR/index.html" \
  && pass "index.html has sliders (rubric: bin/course grade m01/06)" || fail "index.html: build the slider page"
finish

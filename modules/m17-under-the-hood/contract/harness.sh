# Module 17 test helpers. Source after /course/tools/testlib.sh.
#   harness_build <engine dir>   builds the engine (mvn package) and compiles the course harness against it, with
#                                the reference server's Store (its seed) and graphql-java 26.1 for the second opinion
#   harness <Main> [args]        runs dev.shelfie.harness.<Main>
M17=/course/modules/m17-under-the-hood/contract
HARNESS_OUT=/tmp/m17-harness

harness_build() {
  local engine=$1 ref=/course/reference/shelfie-ref/src/main/java/dev/shelfie/ref/data src=/tmp/m17-src
  (cd "$engine" && mvn -q package -DskipTests) >/tmp/m17-engine.log 2>&1 || { echo "mvn package failed in $engine"; tail -20 /tmp/m17-engine.log; return 1; }
  rm -rf "$HARNESS_OUT" "$src"; mkdir -p "$HARNESS_OUT" "$src/dev/shelfie/ref/data"
  sed '/org.springframework/d; /^@Component/d' "$ref/Store.java" >"$src/dev/shelfie/ref/data/Store.java"
  cp "$ref/Model.java" "$src/dev/shelfie/ref/data/"
  HARNESS_CP="$engine/target/minigql.jar:$HARNESS_OUT:$(lib_cp)"
  javac -proc:none -nowarn -d "$HARNESS_OUT" -cp "$HARNESS_CP" "$src"/dev/shelfie/ref/data/*.java "$M17"/harness/dev/shelfie/harness/*.java \
    >/tmp/m17-harness.log 2>&1 || { echo "the harness does not compile against $engine (did you change a public signature?)"; head -20 /tmp/m17-harness.log; return 1; }
  export HARNESS_CP
}

harness() { local main=$1; shift; java -cp "$HARNESS_CP" "dev.shelfie.harness.$main" "$@"; }

# harness_build for a lab: pass/fail lines, stops the test when the engine does not build.
lab_build() {
  if harness_build "$LAB_DIR" >/tmp/m17-build.txt 2>&1; then pass "mvn package builds target/minigql.jar and the course harness compiles against it"
  else fail "the engine does not build" "$(head -15 /tmp/m17-build.txt)"; finish; fi
}

# Runs a harness program; passes when its last line matches $3 (an extended regex). Shows up to 12 problem lines.
harness_expect() { # $1 description, $2 "Main args", $3 regex for the summary line
  local out; out=$(harness $2 2>&1); local last; last=$(tail -1 <<<"$out")
  if grep -qE "$3" <<<"$last"; then pass "$1 ($last)"
  else fail "$1" "${last:0:300}"; grep -E '^(FAIL|DISAGREE|DIFF)|Exception|Error' <<<"$out" | head -12 | sed 's/^/        /'; fi
}

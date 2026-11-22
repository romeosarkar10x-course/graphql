#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
T=$LAB_HOME/tests; JAR=target/shelfie-directives.jar
app_build $JAR || finish
app_start java -jar $JAR && app_hurl $T/directives.hurl "the directives (directives.hurl)"
app_stop
# The same code under the grader's schema: behaviour follows the SDL.
W=$(mktemp -d); trap 'rm -rf $W; app_stop' EXIT
mkdir -p $W/moved && sed -e 's/  genre: String! @uppercase/  genre: String!/' -e 's/  title: String!$/  title: String! @uppercase/' \
  -e 's/  handle: String!/  handle: String! @mask(keep: 1)/' -e 's/  email: String! @mask/  email: String!/' \
  -e 's/priceUsd: String! @currency(code: "USD")/priceUsd: String! @currency(code: "EUR")/' $T/schema.graphqls > $W/moved/shelfie.graphqls
app_start java -jar $JAR --spring.graphql.schema.locations=file:$W/moved/ && app_hurl $T/moved.hurl "directives moved in the SDL (moved.hurl)"
app_stop
# Misuse must stop the server at start-up, naming the field.
n=0
bad() {   # <label> <coordinate> <sed expression>
  n=$((n + 1)); local d=$W/bad$n
  mkdir -p $d && sed -e "$3" $T/schema.graphqls > $d/shelfie.graphqls
  timeout 60 java -jar $JAR --spring.graphql.schema.locations=file:$d/ --server.port=4010 > $d.log 2>&1
  local code=$?
  if [[ $code != 0 && $code != 124 ]] && grep -qF -- "$2" $d.log; then pass "start-up fails on $1, naming $2"
  elif [[ $code == 0 || $code == 124 ]]; then fail "start-up fails on $1" "the server started anyway: misuse is found at request time, or never"
  else fail "start-up fails on $1, naming $2" "it failed, but the log never names $2"; fi
}
bad "@uppercase on a Float" "Book.priceEur" 's/  priceEur: Float!/  priceEur: Float! @uppercase/'
bad "@currency with an unknown code" "Book.priceGbp" 's/priceGbp: String! @currency(code: "GBP")/priceGbp: String! @currency(code: "XYZ")/'
bad "@mask(keep: -1)" "User.phone" 's/phone: String @mask(keep: 2)/phone: String @mask(keep: -1)/'
notes_check notes.md 80 m14/01
finish

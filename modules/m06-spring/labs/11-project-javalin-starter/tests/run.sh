#!/usr/bin/env bash
source /course/tools/testlib.sh
source "$LAB_HOME/../../contract/app-test.sh"
cd "$LAB_DIR" || exit 1
app_build target/tiny-graphql.jar || finish
grep -rqE "RuntimeWiring|GraphQLCodeRegistry|DataFetcher\b" src/main/java/dev/shelfie/app && fail "the app package should not wire graphql-java itself: that is the library's job" || pass "the app has no wiring code: only annotated controllers"
app_start java -jar target/tiny-graphql.jar 4000 || finish
app_hurl /course/modules/m05-graphql-java/labs/04-http/tests/audit.hurl "GraphQL over HTTP (audit.hurl, from lab 5.4)"
app_hurl "$LAB_HOME/../../contract/shelfie.hurl" "Shelfie on the tiny starter (shelfie.hurl)"
app_stop
# The library must fail fast on a method that resolves a field the schema does not have.
rm -rf /tmp/typo && cp -r "$LAB_DIR" /tmp/typo && cd /tmp/typo && rm -rf target
sed -i 's/public int memberCount(Data.Club c)/public int memberCountt(Data.Club c)/' src/main/java/dev/shelfie/app/TypeController.java
mvn -q package -DskipTests >/tmp/typo-build.log 2>&1 || { fail "the typo copy should still compile"; finish; }
timeout 30 java -jar target/tiny-graphql.jar 4001 >/tmp/typo.log 2>&1; code=$?
(( code != 124 )) && grep -q "memberCountt" /tmp/typo.log && pass "a method for a field not in the schema stops startup, naming it" \
  || fail "TinyGraphQL should refuse to start when an @Field method names a field the schema does not have (Club.memberCountt)"
cd "$LAB_DIR"; notes_check DESIGN.md 80 m06/11
finish

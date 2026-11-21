#!/usr/bin/env bash
source /course/tools/testlib.sh
source "$LAB_HOME/../../contract/app-test.sh"
cd "$LAB_DIR" || exit 1
app_build target/shelfie-dgs.jar || finish
[[ -f target/generated-sources/dev/shelfie/dgs/generated/types/Club.java ]] && pass "codegen generated dev.shelfie.dgs.generated.types.Club" \
  || fail "configure graphqlcodegen-maven-plugin: expected target/generated-sources/dev/shelfie/dgs/generated/types/Club.java"
grep -rq "import dev.shelfie.dgs.generated" src/main/java && pass "your fetchers use the generated types" || fail "use the generated types and DgsConstants in your fetchers"
grep -rqE "^\s*@DgsQuery" src/main/java && grep -rqE "^\s*@DgsData\(" src/main/java && pass "@DgsQuery and @DgsData" || fail "use @DgsQuery for root fields and @DgsData for nested ones"
app_start java -jar target/shelfie-dgs.jar || finish
app_hurl "$LAB_HOME/tests/lab.hurl" "Shelfie on DGS (lab.hurl)"
app_stop
# Break the schema in a copy: the build must fail, not the first request in production.
rm -rf /tmp/broken && cp -r "$LAB_DIR" /tmp/broken && cd /tmp/broken && rm -rf target
sed -i 's/^  memberCount: Int!$/  membersTotal: Int!/' src/main/resources/schema/*.graphqls
if mvn -q compile >/tmp/broken.log 2>&1; then fail "after renaming Club.memberCount in the schema the build still compiles: use the generated builders and constants"
else pass "renaming a schema field breaks the build: $(grep -oE 'cannot find symbol|method [a-zA-Z]+\(' /tmp/broken.log | head -1)"; fi
cd "$LAB_DIR"; notes_check notes.md 60 m06/07
finish

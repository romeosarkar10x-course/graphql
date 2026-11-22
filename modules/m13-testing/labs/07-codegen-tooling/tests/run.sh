#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
M13A=/course/modules/m13-testing/contract
# 1. Codegen: types and DgsConstants from the schema, and the controllers name everything through them.
grep -q 'graphqlcodegen-maven-plugin' pom.xml && pass "pom.xml runs graphqlcodegen-maven-plugin" || fail "pom.xml runs graphqlcodegen-maven-plugin"
app_build target/shelfie-app.jar || finish
gen=$(find target/generated-sources -name DgsConstants.java 2>/dev/null | head -1)
[[ -n "$gen" ]] && pass "DgsConstants is generated (${gen#target/})" || fail "DgsConstants is generated into target/generated-sources"
n=$(grep -lE 'DgsConstants' src/main/java/dev/shelfie/app/graph/*.java 2>/dev/null | wc -l)
(( n >= 4 )) && pass "$n controllers name fields through DgsConstants" || fail "every controller names its fields through DgsConstants (found $n of 4)"
grep -rqE 'import [a-z.]*generated\.types\.CreateReviewInput' src/main/java && pass "createReview binds the generated CreateReviewInput" \
  || fail "createReview binds the generated CreateReviewInput (not a hand-written record)"
# 2. Behaviour unchanged: Module 6's 14 checks.
app_start java -jar target/shelfie-app.jar && app_hurl /course/modules/m06-spring/contract/shelfie.hurl "behaviour after codegen"; app_stop
# 3. The experiment: a schema rename must break the build, not a request.
W=$(mktemp -d); trap 'rm -rf $W; app_stop' EXIT
for id in R1 R2 R3; do
  rm -rf $W/p && mkdir -p $W/p && cp -r pom.xml src $W/p/
  what=$(python3 $M13A/mutate.py $LAB_HOME/tests/renames.json $id $W/p) || { fail "$id does not apply: keep the schema as in the starter"; continue; }
  if (cd $W/p && mvn -q -o compile >$W/$id.log 2>&1); then fail "$id still compiles: $what" "the code must name this field or argument through DgsConstants or a generated type"
  else pass "$id breaks the build: $what  ($(grep -m1 -oE '[A-Za-z]+\.java:\[[0-9]+' $W/$id.log))"; fi
done
# 4. graphql.config.yml, read by graphql-config the way IDE plugins and graphql-eslint read it.
require_file graphql.config.yml "the IDE and lint config: schema and documents" || finish
if [[ ! -d node_modules ]]; then npm ci --silent >/tmp/npm.log 2>&1 || { fail "npm ci failed" "$(tail -3 /tmp/npm.log)"; finish; }; fi
out=$(node $M13A/tooling-check.mjs 2>&1) && pass "graphql.config.yml: $(head -1 <<<"$out"), $(grep -c '^ok' <<<"$out") documents valid" \
  || fail "graphql.config.yml" "$(grep FAIL <<<"$out" | head -3 | paste -sd'|')"
# 5. The docs site: every type and every root field has its page section.
require_file docs/spectaql.yml || finish
rm -rf docs/public
if npm run --silent docs >/tmp/docs.log 2>&1 && [[ -s docs/public/index.html ]]; then
  missing=$(python3 - src/main/resources/graphql/shelfie.graphqls docs/public/index.html <<'PY'
import re, sys
sdl, html = open(sys.argv[1]).read(), open(sys.argv[2]).read()
want = ["definition-" + t for t in re.findall(r'^(?:type|input|enum|interface|union|scalar)\s+(\w+)', sdl, re.M)
        if t not in ("Query", "Mutation", "Subscription")]
for root in ("Query", "Mutation", "Subscription"):
    m = re.search(r'^type ' + root + r'\s*\{(.*?)^\}', sdl, re.M | re.S)
    if m: want += [root.lower() + "-" + f for f in re.findall(r'^\s+(\w+)\s*[(:]', m.group(1), re.M)]
print(" ".join(w for w in want if f'id="{w}"' not in html))
PY
)
  [[ -z "$missing" ]] && pass "the docs site has every type and root field (docs/public/index.html)" || fail "the docs site misses sections" "$missing"
else fail "npm run docs did not build docs/public/index.html" "$(tail -3 /tmp/docs.log)"; fi
notes_check notes.md 60 m13/07
finish

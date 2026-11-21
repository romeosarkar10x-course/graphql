#!/usr/bin/env bash
# Image half of `bin/course doctor`: tools inside the course image, and the services it talks to.
source /course/tools/testlib.sh
fail=0
ok()   { printf '  \033[32mok\033[0m    %s\n' "$1"; }
bad()  { printf '  \033[31mFAIL\033[0m  %s\n        fix: %s\n' "$1" "$2"; fail=1; }
warn() { printf '  \033[33mwarn\033[0m  %s\n        %s\n' "$1" "$2"; }

echo; echo "Course image"
jv=$(java -version 2>&1 | head -1 | grep -oP '"\K[0-9]+')
(( jv >= 25 )) && ok "java $jv" || bad "java $jv (need 25)" "bin/course image"
command -v mvn >/dev/null && ok "$(mvn -v 2>/dev/null | head -1 | cut -d" " -f1-3)" || bad "maven missing" "bin/course image"
command -v hurl   >/dev/null && ok "$(hurl --version | head -1 | cut -d' ' -f1-2)" || bad "hurl missing" "bin/course image"
command -v jq     >/dev/null && ok "$(jq --version)" || bad "jq missing" "bin/course image"
command -v grpcurl >/dev/null && ok "grpcurl $(grpcurl -version 2>&1 | awk '{print $2}')" || bad "grpcurl missing" "bin/course image"
ls "$LIB"/jackson-databind-*.jar >/dev/null 2>&1 && ok "course-lib jars (Jackson 3, JUnit console)" \
  || bad "course-lib jars missing" "rm -rf .cache/course-lib && bin/course list"
[[ -w /course/.cache/m2 ]] && ok "Maven repo .cache/m2 writable" || bad ".cache/m2 not writable" "check ownership of .cache/"

echo; echo "Services"
if ref_up; then ok "reference server at $REF"
else warn "reference server not running" "bin/course ref up   (needed from Module 0.1)"; fi
if curl -fsS -o /dev/null -m 3 "$TOXI_API/proxies/shelfie" 2>/dev/null; then ok "toxiproxy route shelfie -> :8666"
else warn "toxiproxy route missing" "bin/course ref up   (needed in Module 1.2)"; fi
if (exec 3<>/dev/tcp/postgres/5432) 2>/dev/null; then ok "PostgreSQL at postgres:5432"
else warn "PostgreSQL not running" "bin/course ref up   (needed from Module 8)"; fi
if (exec 3<>/dev/tcp/redis/6379) 2>/dev/null; then ok "Redis at redis:6379"
else warn "Redis not running" "bin/course ref up   (needed from Module 9)"; fi
if (exec 3<>/dev/tcp/varnish/6082) 2>/dev/null; then ok "Varnish at varnish:6081 (admin 6082)"
else warn "Varnish not running" "bin/course ref up   (needed from Module 9)"; fi
if curl -fsS -o /dev/null -m 6 -X POST -H 'Content-Type: application/json' -d '{"query":"{__typename}"}' \
     https://countries.trevorblades.com/graphql 2>/dev/null; then ok "internet: public GraphQL APIs reachable"
else warn "public GraphQL APIs unreachable" "Module 0.3 needs internet; everything else works offline"; fi
if [[ -n "${GITHUB_TOKEN:-}" ]]; then ok "GITHUB_TOKEN set"
else warn "GITHUB_TOKEN not set" "optional; Module 0.3 and Module 2 GitHub exercises need it (SETUP.md)"; fi

echo
(( fail == 0 )) && echo "Doctor: all required checks green." || echo "Doctor: fix the FAIL lines above."
exit $fail

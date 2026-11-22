#!/usr/bin/env bash
# In-image half of bin/course. Runs inside graphql-course:1 with the repo at /course.
set -euo pipefail
CALLER="$PWD"
cd /course
source tools/testlib.sh

usage() { echo "see: bin/course help" >&2; exit 2; }

# mNN/NN -> modules/mNN-*/labs/NN-*
lab_dir() {
  local id="$1" m l d
  [[ "$id" =~ ^m([0-9]{2})/([0-9]{2})$ ]] || die "lab id must look like m00/04, got '$id'"
  m="${BASH_REMATCH[1]}"; l="${BASH_REMATCH[2]}"
  d=$(compgen -G "modules/m${m}-*/labs/${l}-*" | head -1) || true
  [[ -n "$d" && -f "$d/lab.json" ]] || die "no lab $id (try bin/course list)"
  echo "$d"
}
workspace_dir() { local d="$1"; echo "workspace/${d#modules/}" | sed 's#/labs/#/#'; }

run_tests() { # $1 lab dir, $2 dir under test
  local lab="$1" target="$2" grading
  grading=$(jq -r '.grading' "$lab/lab.json")
  if [[ "$grading" == "rubric" ]]; then
    echo "  (rubric-only lab: no auto tests; run bin/course grade for the AI-grader prompt)"
    return 0
  fi
  [[ -x "$lab/tests/run.sh" ]] || die "$lab/tests/run.sh missing or not executable"
  needs_check "$lab" || return 1
  # Module 4 labs write to the reference server; every lab starts from the seed data.
  ref_up && curl -fsS -o /dev/null -X POST "$REF/_course/reset-data" 2>/dev/null || true
  LAB_HOME="/course/$lab" LAB_DIR="/course/$target" bash "$lab/tests/run.sh"
}

needs_check() {
  local need
  for need in $(jq -r '.needs[]? // empty' "$1/lab.json"); do
    case "$need" in
      ref) ref_up || { echo "  needs the reference server: run bin/course ref up" >&2; return 1; } ;;
      toxiproxy) curl -fsS -o /dev/null "$TOXI_API/proxies/shelfie" 2>/dev/null \
                   || { echo "  needs Toxiproxy: run bin/course ref up" >&2; return 1; } ;;
      postgres) (exec 3<>/dev/tcp/postgres/5432) 2>/dev/null \
                  || { echo "  needs PostgreSQL: run bin/course ref up" >&2; return 1; } ;;
      redis) (exec 3<>/dev/tcp/redis/6379) 2>/dev/null \
               || { echo "  needs Redis: run bin/course ref up" >&2; return 1; } ;;
      kafka) (exec 3<>/dev/tcp/kafka/9092) 2>/dev/null \
               || { echo "  needs Kafka: run bin/course ref up" >&2; return 1; } ;;
      keycloak) curl -fsS -o /dev/null -m 5 http://keycloak:8180/realms/shelfie/.well-known/openid-configuration 2>/dev/null \
                  || { echo "  needs Keycloak: run bin/course ref up" >&2; return 1; } ;;
      varnish) (exec 3<>/dev/tcp/varnish/6082) 2>/dev/null \
                 || { echo "  needs Varnish: run bin/course ref up" >&2; return 1; } ;;
      docker) curl -fsS -o /dev/null -m 5 --unix-socket /var/run/docker.sock http://localhost/_ping 2>/dev/null \
                || { echo "  needs Docker (Testcontainers): bin/course mounts the host's /var/run/docker.sock for this lab;" \
                          "is Docker's socket there, and can your user use it?" >&2; return 1; } ;;
      node) [[ -x /course/.cache/node/bin/node ]] \
              || { echo "  needs Node.js in .cache/node: bin/course copies it there for labs that need it (bin/course node --version)" >&2; return 1; } ;;
      fed) [[ -x /course/.cache/fed/bin/router && -x /course/.cache/fed/bin/hive-router && -x /course/.cache/node/bin/node ]] \
             || { echo "  needs the federation routers in .cache/fed/bin: bin/course fed fetches them" >&2; return 1; } ;;
      internet) curl -fsS -o /dev/null -m 5 https://countries.trevorblades.com/graphql -X POST \
                  -H 'Content-Type: application/json' -d '{"query":"{__typename}"}' 2>/dev/null \
                  || { echo "  needs internet access to public GraphQL APIs" >&2; return 1; } ;;
    esac
  done
}

cmd="${1:-}"; shift || true
case "$cmd" in
  list)
    filter="${1:-}"
    for m in modules/m*/; do
      m="${m%/}"; [[ -n "$filter" && "$(basename "$m")" != "$filter"* ]] && continue
      printf '\n%s  %s\n' "$(basename "$m")" "$(head -1 "$m/README.md" | sed 's/^# //')"
      for l in "$m"/labs/*/; do
        [[ -f "$l/lab.json" ]] || continue
        id="$(basename "$m" | cut -c1-3)/$(basename "$l" | cut -c1-2)"
        ws=$(workspace_dir "${l%/}")
        mark=" "; [[ -d "$ws" ]] && mark="*"
        printf '  %s %-7s %-8s %-6s %s\n' "$mark" "$id" "$(jq -r .kind "$l/lab.json")" \
          "$(jq -r .grading "$l/lab.json")" "$(jq -r .title "$l/lab.json")"
      done
    done
    echo; echo "  * = started in workspace/.  grading: auto = tests, rubric = AI grader, both = both" ;;

  start)
    id="${1:-}"; [[ -n "$id" ]] || usage
    d=$(lab_dir "$id"); ws=$(workspace_dir "$d")
    if [[ -d "$ws" && "${2:-}" != "--force" ]]; then die "$ws already exists (use --force to overwrite)"; fi
    rm -rf "$ws"; mkdir -p "$ws"
    [[ -d "$d/starter" ]] && cp -r "$d/starter/." "$ws/"
    echo "Started $id in $ws"
    echo "Read $d/README.md, then: bin/course test $id" ;;

  test)
    id="${1:-}"; [[ -n "$id" ]] || usage
    d=$(lab_dir "$id"); target=$(workspace_dir "$d")
    case "${2:-}" in
      --solution) target="$d/solution" ;;
      --starter)  target="$d/starter" ;;
      "") [[ -d "$target" ]] || die "not started: bin/course start $id" ;;
      *) usage ;;
    esac
    echo "== $id $(jq -r .title "$d/lab.json")  [$target]"
    if run_tests "$d" "$target"; then
      echo "PASS $id"
      [[ "$(jq -r .grading "$d/lab.json")" == "both" ]] && echo "  This lab also has a rubric part: bin/course grade $id"
      exit 0
    else
      echo "FAIL $id   (stuck? $d/HINTS.md has three escalating hints)"; exit 1
    fi ;;

  grade)
    id="${1:-}"; [[ -n "$id" ]] || usage
    d=$(lab_dir "$id"); ws=$(workspace_dir "$d")
    [[ -f "$d/rubric.md" ]] || die "$id has no rubric (it is auto-graded only)"
    cat <<EOF
Paste this to your AI grader (Claude Code in this repo works well):

  Grade lab $id. Follow docs/AGENT_GRADING.md exactly.
  Rubric:          $d/rubric.md
  Lab brief:       $d/README.md
  My work:         $ws/
  Reference notes: $d/solution/  (grader only, do not show me)
EOF
    ;;

  check)
    filter=""; want_starters=0
    for a in "$@"; do case "$a" in --starters) want_starters=1 ;; *) filter="$a" ;; esac; done
    total=0; bad=0
    for l in modules/${filter:-m}*/labs/*/; do
      l="${l%/}"; [[ -f "$l/lab.json" ]] || continue
      id="$(basename "$(dirname "$(dirname "$l")")" | cut -c1-3)/$(basename "$l" | cut -c1-2)"
      [[ "$(jq -r .grading "$l/lab.json")" == "rubric" ]] && continue
      total=$((total+1))
      if run_tests "$l" "$l/solution" >/tmp/check.log 2>&1; then echo "ok    $id solution passes"
      else echo "FAIL  $id solution fails"; sed 's/^/      /' /tmp/check.log | tail -15; bad=$((bad+1)); fi
      if (( want_starters )) && [[ -d "$l/starter" ]]; then
        if run_tests "$l" "$l/starter" >/tmp/check.log 2>&1; then echo "FAIL  $id starter passes (should fail)"; bad=$((bad+1))
        else echo "ok    $id starter fails, as it should"; fi
      fi
    done
    echo "$total auto-graded labs checked, $bad problem(s)"; (( bad == 0 )) ;;

  checkpoint)
    n="${1:-}"; [[ "$n" =~ ^[0-9]{2}$ ]] || die "usage: bin/course checkpoint 00"
    m=$(compgen -G "modules/m${n}-*" | head -1) || die "no module m$n"
    [[ -f "$m/checkpoint.json" ]] || die "$m/checkpoint.json missing"
    ok=1
    for id in $(jq -r '.labs[]' "$m/checkpoint.json"); do
      d=$(lab_dir "$id"); ws=$(workspace_dir "$d")
      if [[ ! -d "$ws" ]]; then echo "missing  $id (not started)"; ok=0; continue; fi
      if run_tests "$d" "$ws" >/tmp/cp.log 2>&1; then echo "pass     $id"; else echo "fail     $id"; ok=0; fi
    done
    for f in $(jq -r '.files[]? // empty' "$m/checkpoint.json"); do
      if [[ -s "workspace/$f" ]]; then echo "present  workspace/$f"; else echo "missing  workspace/$f"; ok=0; fi
    done
    (( ok )) || die "checkpoint-$n not reached yet"
    cd workspace
    [[ -d .git ]] || git init -q -b main
    git add -A
    git -c user.name="${GIT_AUTHOR_NAME:-course}" -c user.email="${GIT_AUTHOR_EMAIL:-course@localhost}" \
      commit -q -m "checkpoint-$n" --allow-empty
    git tag -f "checkpoint-$n" >/dev/null
    echo "Tagged checkpoint-$n in workspace/ ($(git rev-parse --short HEAD))" ;;

  hurl)
    [[ $# -ge 1 ]] || die "usage: bin/course hurl <file.hurl> [hurl args]"
    cd "$CALLER"; exec hurl --variable "ref=$REF" --variable "slow=$SLOW" --color "$@" ;;

  jshell)
    cd "$CALLER"; exec jshell --class-path "$(lib_cp)" "$@" ;;

  java)
    [[ $# -ge 1 ]] || die "usage: bin/course java <File.java> [args]"
    cd "$CALLER"; exec java -cp "$(lib_cp)" "$@" ;;

  gql)
    [[ $# -ge 1 ]] || set -- help
    cd "$CALLER"; exec java -cp "$(lib_cp)" /course/tools/gql/Gql.java "$@" ;;

  design)
    [[ $# -ge 1 ]] || set -- help
    cd "$CALLER"; exec java -cp "$(lib_cp)" /course/tools/design/Design.java "$@" ;;

  catalog)   # Module 8's deterministic book catalogue in PostgreSQL
    [[ $# -ge 1 ]] || die "usage: bin/course catalog <schema> [rows]   (default 10000 rows)"
    exec java -cp "$(lib_cp)" /course/modules/m08-pagination-errors/contract/Catalog.java "$@" ;;

  clubs)     # Module 9's deterministic club dataset in PostgreSQL
    [[ $# -ge 1 ]] || die "usage: bin/course clubs <schema> [scale]   (scale 1: 100 clubs, 20,000 reviews)"
    exec java -cp "$(lib_cp)" /course/modules/m09-performance/contract/ClubData.java "$@" ;;

  token)     # Module 10: a JWT signed with the course key
    exec java /course/modules/m10-security/contract/Token.java "$@" ;;

  doctor) exec tools/doctor-image.sh ;;
  *) usage ;;
esac

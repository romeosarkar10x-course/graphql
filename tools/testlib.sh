#!/usr/bin/env bash
# Shared helpers for lab tests (tests/run.sh) and the runner. Source it; do not execute it.
#
# Environment every test gets:
#   LAB_HOME  the lab definition dir (README, tests, expected files)
#   LAB_DIR   the copy under test (workspace/..., solution/ or starter/)
#   REF       reference Shelfie, e.g. http://shelfie-ref:8080
#   SLOW      the same server behind Toxiproxy, http://toxiproxy:8666
#   TOXI_API  Toxiproxy admin API, http://toxiproxy:8474
#   PGURL     PostgreSQL (Module 8 on), jdbc:postgresql://postgres:5432/shelfie, user/password shelfie
#   LIB       jars for single-file Java programs (Jackson 3, graphql-java 26.1, JUnit console)

export REF="${REF:-http://shelfie-ref:8080}"
export SLOW="${SLOW:-http://toxiproxy:8666}"
export TOXI_API="${TOXI_API:-http://toxiproxy:8474}"
export PGURL="${PGURL:-jdbc:postgresql://postgres:5432/shelfie}"
export LIB="${LIB:-/course/.cache/course-lib}"
export SHELFIE_URL="${SHELFIE_URL:-$REF}"

T_FAILS=0

die()  { echo "course: $*" >&2; exit 1; }
pass() { printf '  \033[32mpass\033[0m  %s\n' "$1"; }
fail() { printf '  \033[31mfail\033[0m  %s\n' "$1"; [[ -n "${2:-}" ]] && printf '        %s\n' "$2"; T_FAILS=$((T_FAILS+1)); }
note() { printf '        %s\n' "$1"; }

# Exit status for tests/run.sh
finish() { (( T_FAILS == 0 )) || { echo "  $T_FAILS check(s) failed"; exit 1; }; }

# Class path for single-file programs: every jar in LIB except the JUnit console launcher.
lib_cp() {
  local IFS=: j jars=()
  for j in "$LIB"/*.jar; do [[ "$j" == *junit-platform-console* ]] || jars+=("$j"); done
  echo "${jars[*]}"
}

ref_up() { curl -fsS -o /dev/null -m 3 "$REF/actuator/health" 2>/dev/null; }
ref_reset_stats() { curl -fsS -o /dev/null -X POST "$REF/_course/reset"; }
ref_stats() { curl -fsS "$REF/_course/stats"; }   # {"api":N,"graphql":M}
ref_reset_data() { curl -fsS -o /dev/null -X POST "$REF/_course/reset-data"; }   # Module 4: back to the seed

# POST a GraphQL request to the reference server as user $1 ("" = anonymous). $2 query, $3 variables JSON.
gql_as() {
  local who="$1" q="$2" v="${3:-{\}}"
  jq -n --arg q "$q" --argjson v "$v" '{query: $q, variables: $v}' \
    | curl -sS -H 'Content-Type: application/json' ${who:+-H "X-Shelfie-User: $who"} --data-binary @- "$REF/graphql"
}

require_file() { # $1 path relative to LAB_DIR, $2 description
  if [[ -s "$LAB_DIR/$1" ]]; then pass "$1 exists"; return 0; fi
  fail "$1 is missing or empty" "${2:-}"; return 1
}

# Written work: passes when $1 (default notes.md) has at least $2 (default 60) more words, outside
# heading and comment lines, than the starter's copy: headings and empty tables are not answers.
notes_check() { # $1 file, $2 extra words, $3 lab id for the rubric hint
  local f="${1:-notes.md}" extra="${2:-60}" id="${3:-}" mine base
  mine=$(grep -vE '^\s*#' "$LAB_DIR/$f" 2>/dev/null | wc -w)
  base=$(grep -vE '^\s*#' "$LAB_HOME/starter/$f" 2>/dev/null | wc -w)
  if (( mine >= base + extra )); then pass "$f filled in${id:+ (rubric: bin/course grade $id)}"
  else fail "$f: answer the questions (rubric-graded${id:+: bin/course grade $id})"; fi
}

# Run a Hurl file against the reference server; quiet on success, verbose on failure.
run_hurl() { # $1 file, rest: extra hurl args
  local f="$1"; shift
  local out
  if out=$(hurl --test --variable "ref=$REF" --variable "slow=$SLOW" "$@" "$f" 2>&1); then
    pass "hurl $(basename "$f")"
  else
    fail "hurl $(basename "$f")"; echo "$out" | grep -vE '^\s*$' | head -25 | sed 's/^/        /'
  fi
}

# Compare two JSON files ignoring key order and whitespace.
json_equal() { diff <(jq -S . "$1") <(jq -S . "$2") >/dev/null; }
json_diff()  { diff <(jq -S . "$1") <(jq -S . "$2") | head -30 | sed 's/^/        /'; }

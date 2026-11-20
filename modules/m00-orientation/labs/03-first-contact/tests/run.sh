#!/usr/bin/env bash
source /course/tools/testlib.sh
ops="$LAB_DIR/operations"

# POST a .graphql file to an endpoint; prints the JSON response.
post_op() { # $1 url, $2 file, rest: extra curl args
  local url="$1" file="$2"; shift 2
  jq -Rs '{query: .}' "$file" | curl -sS -m 20 -H 'Content-Type: application/json' "$@" --data-binary @- "$url"
}

check_op() { # $1 file, $2 url, $3 jq expression that must be true, $4 label
  local f="$ops/$1" out
  grep -qvE '^\s*(#.*)?$' "$f" 2>/dev/null || { fail "$1 has no operation in it yet"; return; }
  out=$(post_op "$2" "$f") || { fail "$1: request failed"; return; }
  if jq -e '.errors' >/dev/null 2>&1 <<<"$out"; then
    fail "$1: the API returned errors" "$(jq -c '.errors[0].message' <<<"$out")"
  elif jq -e "$3" >/dev/null 2>&1 <<<"$out"; then pass "$1: $4"
  else fail "$1: response does not contain $4" "$(head -c 300 <<<"$out")"; fi
}

check_op swapi-film.graphql https://swapi-graphql.netlify.app/graphql \
  '.data.film.title == "A New Hope" and .data.film.director == "George Lucas" and .data.film.releaseDate == "1977-05-25"' \
  "title, director and release date of A New Hope"

check_op countries-india.graphql https://countries.trevorblades.com/graphql \
  '.data.country.name == "India" and .data.country.capital == "New Delhi" and .data.country.currency == "INR" and ([.data.country.languages[].name] | index("Hindi") != null)' \
  "India with capital, currency and language names"

if [[ -n "${GITHUB_TOKEN:-}" ]]; then
  f="$ops/github-viewer.graphql"
  out=$(post_op https://api.github.com/graphql "$f" -H "Authorization: bearer $GITHUB_TOKEN" -H 'User-Agent: graphql-course')
  if jq -e '.data.viewer.login and (.data.viewer.repositories.nodes | length <= 3) and (.data.viewer.repositories.nodes | all(has("name")))' >/dev/null 2>&1 <<<"$out"; then
    pass "github-viewer.graphql: your login and up to 3 repository names"
  else fail "github-viewer.graphql: unexpected response" "$(head -c 300 <<<"$out")"; fi
else
  grep -q 'viewer' "$ops/github-viewer.graphql" 2>/dev/null \
    && note "skip  github-viewer.graphql not run: GITHUB_TOKEN not set (optional, see SETUP.md)" \
    || fail "github-viewer.graphql has no viewer query yet"
fi

[[ $(sed -n '/^| 1 /p' "$LAB_DIR/notes.md" | grep -oE '[0-9]{2,}' | head -1) ]] \
  && pass "notes.md has measured sizes (rubric: bin/course grade m00/03)" \
  || fail "notes.md: fill in the byte counts in section 1"
finish

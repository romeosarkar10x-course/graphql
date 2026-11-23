#!/usr/bin/env bash
# Trusted documents: build the router's persisted-documents manifest from operations/.
#   scripts/persist.sh            writes router/persisted-documents.json ({"<sha256>": "<document>"}) and
#                                 tests/ids.env (OperationName=<sha256>, for Hurl's --variables-file)
# A document is one operation file plus the fragment files it spreads (transitively), in name order. The id is
# the SHA-256 of the document's text. Run it after changing an operation; CI fails if the files are stale.
set -euo pipefail
cd "$(dirname "$0")/.."
declare -A frag_file
for f in operations/*.graphql; do
  grep -qE '^(query|mutation|subscription)' "$f" && continue
  for name in $(grep -oE '^fragment [A-Za-z0-9_]+' "$f" | cut -d' ' -f2); do frag_file[$name]=$f; done
done
spreads() { { grep -hoE '\.\.\.[A-Za-z0-9_]+' "$@" || true; } | { grep -vE '\.\.\.on$' || true; } | sed 's/.*\.\.\.//' | sort -u; }
manifest='{}'; : > tests/ids.env
for f in operations/*.graphql; do
  grep -qE '^(query|mutation|subscription)' "$f" || continue
  files=("$f"); seen=" "
  todo=$(spreads "$f")
  while [[ -n $todo ]]; do
    next=""
    for n in $todo; do
      [[ $seen == *" $n "* ]] && continue
      seen+="$n "
      ff=${frag_file[$n]:-}; [[ -n $ff ]] || { echo "$f: no file defines fragment $n" >&2; exit 1; }
      files+=("$ff"); next+=" $(spreads "$ff")"
    done
    todo=$(echo $next)
  done
  doc=$(for x in "${files[@]}"; do grep -v '^#' "$x"; done)
  id=$(printf '%s' "$doc" | sha256sum | cut -d' ' -f1)
  name=$(grep -oE '^(query|mutation|subscription) [A-Za-z0-9_]+' "$f" | head -1 | cut -d' ' -f2)
  manifest=$(jq -c --arg id "$id" --arg doc "$doc" '. + {($id): $doc}' <<<"$manifest")
  echo "$name=$id" >> tests/ids.env
done
jq -S . <<<"$manifest" > router/persisted-documents.json
echo "$(jq length router/persisted-documents.json) documents -> router/persisted-documents.json, tests/ids.env"

# Hashed answer keys for labs 17.1 and 17.10. Source it; answers_score <answers.json> <key.tsv> prints "points wrong-ids".
# An answer is normalised before hashing: case, surrounding space, a leading "§", "section" or "sec.", a trailing
# ".", "@" and quotes do not matter; for "list" answers, neither does order or the separator (commas, spaces, "and").
answer_norm() { # $1 kind (text|list), $2 answer
  local a
  a=$(printf '%s' "$2" | tr '[:upper:]' '[:lower:]' | sed -E 's/[@"`]//g; s/^[[:space:]]*(§|section|sec\.?)[[:space:]]*//; s/[[:space:]]+/ /g; s/^ //; s/ $//; s/\.$//')
  [[ $1 == list ]] && a=$(printf '%s' "$a" | sed -E 's/ and /,/g; s/[ ,]+/\n/g' | grep -v '^$' | sort -u | paste -sd, -)
  printf '%s' "$a"
}
answer_hash() { answer_norm "$1" "$2" | sha256sum | cut -c1-16; }
answers_score() { # $1 answers.json, $2 key.tsv (id kind hash)
  local n=0 wrong=() id kind hash mine
  while IFS=$'\t' read -r id kind hash; do
    mine=$(jq -r --arg id "$id" '.[$id] // "" | if type == "array" then join(",") else tostring end' "$1" 2>/dev/null)
    [[ -n "$mine" && "$(answer_hash "$kind" "$mine")" == "$hash" ]] && n=$((n+1)) || wrong+=("$id")
  done <"$2"
  echo "$n ${wrong[*]}"
}

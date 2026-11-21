#!/usr/bin/env bash
# Capture the flag: flags are checked against hashes; this script never prints one.
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
[[ -f flags.txt ]] || { fail "flags.txt is missing"; finish; }
score=0; found=()
while read -r line; do
  [[ "$line" =~ ^FLAG\{([a-z-]+)-[0-9a-f]{8}\}$ ]] || continue
  h=$(printf '%s' "$line" | sha256sum | cut -d' ' -f1)
  if grep -qx "$h" "$LAB_HOME/tests/key.txt" && [[ ! " ${found[*]} " == *" ${BASH_REMATCH[1]} "* ]]; then
    found+=("${BASH_REMATCH[1]}"); score=$((score + 1))
  fi
done < <(grep -v '^#' flags.txt | tr -d '\r' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')
echo "  score: $score of 12${found[*]:+ (${found[*]})}"
(( score >= 8 )) && pass "$score of 12 flags captured" || fail "8 of 12 flags to pass, you have $score" "HINTS.md, and the vulnerability classes in lecture 10.3"
finish

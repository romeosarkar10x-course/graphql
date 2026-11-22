#!/usr/bin/env bash
# Writes the hashed keys of labs 17.1 and 17.10 from their solutions' answers.json. Run inside the course image:
#   bin/course shell -c 'bash _workbench/2026-11-23-m17-build/gen-keys.sh'
source /course/modules/m17-under-the-hood/contract/answers/answers.sh
L=/course/modules/m17-under-the-hood/labs
for lab in 01-spec-reading 10-spec-hunt; do
  jq -r 'to_entries[] | "\(.key)\t\(.value)"' $L/$lab/solution/answers.json | while IFS=$'\t' read -r id a; do
    kind=text; [[ $id == q09 ]] && kind=list
    printf '%s\t%s\t%s\n' "$id" "$kind" "$(answer_hash $kind "$a")"
  done >$L/$lab/tests/key.tsv
  echo "$lab: $(wc -l <$L/$lab/tests/key.tsv) keys"
done

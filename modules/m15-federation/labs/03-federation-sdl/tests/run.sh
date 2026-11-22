#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m15-federation/contract/fed.sh
cd "$LAB_DIR" || exit 1
W=$(mktemp -d); trap 'rm -rf $W; fed_stop' EXIT
for s in accounts catalog reviews clubs; do require_file subgraphs/$s.graphqls >/dev/null || fail "subgraphs/$s.graphqls is missing"; done
composed=0
if compose supergraph.yaml $W/supergraph.graphql $W/api.graphql 2>$W/errors.txt; then
  pass "the four subgraphs compose"; composed=1
  checks=0
  while IFS='|' read -r verdict name detail; do
    checks=$((checks+1))
    if [[ $verdict == ok ]]; then pass "$name"; else fail "$name" "$detail"; fi
  done < <(java -cp "$(lib_cp)" $LAB_HOME/tests/Fed3Check.java $W/supergraph.graphql $W/api.graphql)
  (( checks > 0 )) || fail "the checker ran" "it printed nothing: see the error above"
else
  fail "the four subgraphs compose" "$(head -5 $W/errors.txt | cut -c1-300 | paste -sd'|' | sed 's/|/\n        /g')"
fi
# The experiment, by the grader: catalog's Book without its @key directives must not compose.
if (( composed )); then
mkdir -p $W/nokey/subgraphs && cp subgraphs/*.graphqls $W/nokey/subgraphs/ && cp supergraph.yaml $W/nokey/
sed -i -E 's/^(type Book[^{]*)@key\(fields: *"[^"]*"\) */\1/; s/^(type Book[^{]*)@key\(fields: *"[^"]*"\) */\1/' $W/nokey/subgraphs/catalog.graphqls
if grep -qE '^type Book[^{]*@key' $W/nokey/subgraphs/catalog.graphqls; then fail "experiment: Book without @key" "could not remove @key from catalog's Book (keep 'type Book ... @key(fields: \"...\")' on one line)"
elif (cd $W/nokey && compose supergraph.yaml $W/nokey.graphql 2>$W/nokey.txt); then fail "experiment: catalog's Book without @key no longer composes" "it composed: is Book an entity anywhere?"
else pass "experiment: catalog's Book without @key no longer composes ($(head -1 $W/nokey.txt | cut -d: -f1))"; fi
fi
notes_check notes.md 80 m15/03
finish

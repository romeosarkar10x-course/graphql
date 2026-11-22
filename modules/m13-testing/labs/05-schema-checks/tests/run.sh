#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR" || exit 1
T=$LAB_HOME/tests; CP=$(lib_cp); W=$(mktemp -d); trap 'rm -rf $W' EXIT
require_file SchemaCheck.java || finish
# The unchanged schema and the known clients: a clean pass.
out=$(java -cp "$CP" SchemaCheck.java $T/old.graphqls $T/old.graphqls $T/clients 2>/dev/null); code=$?
[[ $code == 0 ]] && ! grep -qE '^(BREAKING|DANGEROUS|INVALID)' <<<"$out" && grep -q '^verdict: PASS' <<<"$out" \
  && pass "no change: verdict PASS, nothing reported" || fail "no change: verdict PASS, nothing reported" "exit $code: $(head -3 <<<"$out" | paste -sd'|')"
for s in $T/scenarios/*/; do
  name=$(basename $s); rm -rf $W/clients; cp -r $T/clients $W/clients; [[ -d $s/extra-clients ]] && cp -r $s/extra-clients $W/clients/extra
  out=$(cd $W && java -cp "$CP" "$LAB_DIR/SchemaCheck.java" $T/old.graphqls $s/new.graphqls clients 2>/dev/null); code=$?
  why=()
  while read -r kind rest; do
    case "$kind" in
      exit) [[ $code == "$rest" ]] || why+=("exit $code, want $rest") ;;
      has) grep -qF -- "$rest" <<<"$out" || why+=("no \"$rest\"") ;;
      lacks) grep -qF -- "$rest" <<<"$out" && why+=("unexpected \"$rest\"") ;;
    esac
  done < $s/expect
  grep -qE '^verdict: (PASS|FAIL)' <<<"$out" || why+=("no verdict line")
  (( ${#why[@]} == 0 )) && pass "$name" || fail "$name" "$(IFS=';'; echo "${why[*]}")"
done
# The pipeline: a GitHub Actions workflow that runs graphql-inspector and this check on every pull request.
wf=.github/workflows/schema.yml
if require_file $wf; then
  grep -qE 'pull_request' $wf && pass "$wf runs on pull requests" || fail "$wf runs on pull requests"
  grep -qE 'uses: *graphql-hive/graphql-inspector@' $wf && pass "$wf runs graphql-inspector's action" || fail "$wf runs graphql-inspector's action (graphql-hive/graphql-inspector@...)"
  grep -qE 'SchemaCheck' $wf && pass "$wf runs SchemaCheck" || fail "$wf runs SchemaCheck"
fi
notes_check notes.md 60 m13/05
finish

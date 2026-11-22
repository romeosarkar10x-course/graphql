#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m17-under-the-hood/contract/harness.sh
cd "$LAB_DIR" || exit 1
lab_build
out=$(harness RoundTrip /course/reference/shelfie-ref/src/main/resources/graphql/schema.graphqls $M17/conformance/fixture.graphqls $M17/printing/kitchen-sink.graphqls 2>&1)
for check in print stable introspect rebuild; do
  bad=$(grep -E "^FAIL $check " <<<"$out")
  if [[ -z "$bad" ]] && grep -qE "^pass $check " <<<"$out"; then pass "$check: 3 schemas (reference, fixture, kitchen sink)"
  else fail "$check"; head -4 <<<"${bad:-$out}" | cut -c1-300 | sed 's/^/        /'; fi
done
finish

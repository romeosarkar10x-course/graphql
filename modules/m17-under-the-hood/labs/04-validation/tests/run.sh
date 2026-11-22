#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m17-under-the-hood/contract/harness.sh
cd "$LAB_DIR" || exit 1
lab_build
harness_expect "40 invalid documents: your errors per rule match graphql-java's" "ValidateDiff $M17/validation/invalid" '^documents 40 of 40 agree'
harness_expect "20 valid documents (8 tricky ones and Module 2's operations): no errors" "ValidateDiff $M17/validation/valid /course/modules/m02-query-language/labs/07-documents/solution/ops" '^documents 20 of 20 agree; errors yours 0 '
finish

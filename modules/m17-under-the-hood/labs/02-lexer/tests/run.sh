#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m17-under-the-hood/contract/harness.sh
cd "$LAB_DIR" || exit 1
lab_build
harness_expect "20 fixed cases, spec behaviour where graphql-java differs" "Cases lexer" '^passed 20 of 20$'
harness_expect "differential test against graphql-java's lexer on 1,000 documents" "TokenDiff 1000 17" '^agree [0-9]+ of 1000; disagree 0; '
finish

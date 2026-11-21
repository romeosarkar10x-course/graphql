#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
design() { java -cp "$(lib_cp)" /course/tools/design/Design.java "$@"; }
for f in relations.graphqls Relations.java clients/review-card.graphql; do require_file "$f" || finish; done
if out=$(design lint relations.graphqls --rules references 2>&1); then pass "no foreign keys in output types (author, not authorId)"
else fail "output fields that are keys, not references"; grep -E '^fail|rror' <<<"$out" | head -8 | sed 's/^/        /'; fi
rm -rf /tmp/rel && mkdir -p /tmp/rel
if javac -cp "$(lib_cp)" -d /tmp/rel Data.java Memberships.java Relations.java "$LAB_HOME/tests/RelCheck.java" 2>/tmp/rel.log; then
  (cd "$LAB_DIR" && java -cp "/tmp/rel:$(lib_cp)" RelCheck) >/tmp/rel.txt 2>&1
  if diff -q /tmp/rel.txt "$LAB_HOME/tests/expected/relations.txt" >/dev/null; then
    pass "members connection: 3 pages with role and joinedAt on the edge; owner, book, author as objects"
  else fail "query results differ (< yours, > expected)"; diff /tmp/rel.txt "$LAB_HOME/tests/expected/relations.txt" | cut -c1-220 | head -12 | sed 's/^/        /'; fi
else fail "Relations.java does not compile with the harness" "$(grep error /tmp/rel.log | head -3)"; fi
if out=$(design clients relations.graphqls clients/review-card.graphql 2>&1); then
  ops=$(grep -cE '^\s*(query|mutation)\b' clients/review-card.graphql)
  if (( ops == 1 )) && ! grep -qE '\b(authorId|bookId)\b' clients/review-card.graphql; then pass "review card: one operation, no keys, valid against your schema"
  else fail "review card should be ONE operation that selects author and book, not authorId/bookId ($ops operations)"; fi
else fail "clients/review-card.graphql is not valid against relations.graphqls"; head -4 <<<"$out" | sed 's/^/        /'; fi
notes_check notes.md 100 m07/03
finish

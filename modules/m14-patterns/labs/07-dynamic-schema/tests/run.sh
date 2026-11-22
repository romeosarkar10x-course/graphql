#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
cd "$LAB_DIR" || exit 1
JAR=target/shelfie-dynamic.jar; ADMIN="http://localhost:$PORT/admin/graphql"
sql() { java -cp "$(lib_cp)" /course/modules/m11-realtime/contract/Sql.java "$1" 2>&1; }
gql() { curl -s "$URL" -H 'Content-Type: application/json' -d "{\"query\":\"$1\"}"; }
adm() { curl -s "$ADMIN" -H 'X-Admin-Key: course-admin-key' -H 'Content-Type: application/json' -d "{\"query\":\"$1\"}"; }
fields() { gql '{ __type(name: \"Book\") { fields { name } } }' | jq -r '[.data.__type.fields[]?.name] | join(",")'; }
app_build $JAR || finish
app_start java -jar $JAR || finish
pid=$APP_PID
app_hurl $LAB_HOME/tests/dynamic.hurl "two endpoints, one changing the other (dynamic.hurl)" --variable "admin=$ADMIN"

# Data in a new column needs no schema change at all.
sql "UPDATE m14_07.books SET page_count = 304 WHERE id = 'b1'" >/dev/null
[[ $(gql '{ book(id: \"b1\") { pageCount } }' | jq -r .data.book.pageCount) == 304 ]] && pass "the new field serves the new column's data" \
  || fail "the new field serves the new column's data" "book(b1).pageCount is not 304"

# The experiment: someone adds a column and a metadata row by hand. The field appears, nobody restarts anything.
sql "ALTER TABLE m14_07.books ADD COLUMN language text" >/dev/null
sql "UPDATE m14_07.books SET language = 'en'" >/dev/null
sql "INSERT INTO m14_07.fields (type_name, field_name, graphql_type, column_name, description) VALUES ('Book', 'language', 'STRING', 'language', 'Original language.')" >/dev/null
got=; for _ in $(seq 20); do got=$(gql '{ book(id: \"b2\") { language } }' | jq -r '.data.book.language // empty'); [[ $got == en ]] && break; sleep 0.5; done
if [[ $got == en ]] && kill -0 $pid 2>/dev/null; then pass "a metadata row added in the database appears as Book.language within seconds, same process"
else fail "a metadata row added in the database appears as Book.language within 10 s" "book(b2).language: ${got:-absent}"; fi

# Bad metadata must not take the API down.
sql "INSERT INTO m14_07.fields (type_name, field_name, graphql_type, column_name) VALUES ('Book', 'ghost', 'STRING', 'no_such_column'), ('Book', '2fast', 'INT', 'page_count'), ('Book', 'mood', 'EMOJI', 'title')" >/dev/null
sleep 3
f=$(fields)
if [[ $f == id,title,author,publishedYear,pageCount,language ]]; then pass "rows that make no sense (no such column, a name GraphQL refuses, an unknown type) are skipped; the rest serves"
else fail "rows that make no sense are skipped; the rest serves" "Book's fields: ${f:-none (the schema is gone)}"; fi
sql "DELETE FROM m14_07.fields WHERE field_name IN ('ghost', '2fast', 'mood')" >/dev/null

# Reloads while traffic flows: no request may fail.
( for _ in $(seq 150); do gql '{ books { id title } }'; echo; done ) > /tmp/traffic.txt &
traffic=$!
swaps=0
for n in a b c; do adm "mutation { addField(name: \\\"extra_$n\\\", type: STRING) { field { name } } }" | grep -q "extra_$n" && swaps=$((swaps + 1)); sleep 0.2
  adm "mutation { removeField(name: \\\"extra_$n\\\") { userErrors { message } } }" >/dev/null; done
wait $traffic
bad=$(grep -vc '"books":\[{"id":"b1"' /tmp/traffic.txt)
if [[ $swaps != 3 ]]; then fail "150 queries during six schema swaps: none failed" "the admin endpoint made $swaps of 3 swaps"
elif [[ $bad == 0 ]]; then pass "150 queries during six schema swaps: none failed"
else fail "150 queries during six schema swaps: none failed" "$bad of 150 were not a normal answer"; fi

# Removing a field removes it from the schema, not the data.
out=$(adm 'mutation { removeField(name: \"language\") { field { name } userErrors { message } } }')
f=$(fields); kept=$(sql "SELECT count(*) FROM m14_07.books WHERE language = 'en'")
[[ $(jq -r '.data.removeField.field.name // empty' <<<"$out" 2>/dev/null) == language && $f != *language* && $kept == 3 ]] && pass "removeField: Book.language is gone, the column and its data stay" \
  || fail "removeField: Book.language is gone, the column and its data stay" "removeField said: ${out:0:120}; fields: $f; rows with language: $kept"
notes_check notes.md 80 m14/07
finish

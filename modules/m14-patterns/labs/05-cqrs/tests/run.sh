#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m14-patterns/contract/kafka.sh
cd "$LAB_DIR" || exit 1
JAR=target/shelfie-cqrs.jar; TOPIC="m14-reviews-$(date +%s)-$RANDOM"
sql() { java -cp "$(lib_cp)" /course/modules/m11-realtime/contract/Sql.java "$1" 2>&1; }
gql() { curl -s "$URL" -H 'Content-Type: application/json' -d "{\"query\":\"$1\"}"; }
app_build $JAR || finish
app_start java -jar $JAR --shelfie.topic=$TOPIC || finish
app_hurl $LAB_HOME/tests/cqrs.hurl "a command, then the read model (cqrs.hurl)"
id=$(sql "SELECT id FROM m14_05.reviews WHERE author = 'kim' AND book_id = 'b1'" | head -1)
[[ -n "$id" && "$id" != *Exception* ]] || { fail "the review from C01 is in m14_05.reviews" "$id"; finish; }

# The outbox: the event was stored with the review, and the relay marked it published.
row=$(sql "SELECT type, payload->>'reviewId', published_at IS NOT NULL FROM m14_05.outbox WHERE payload->>'reviewId' = '$id'")
[[ "$row" == $'ReviewCreated\t'"$id"$'\tt' ]] && pass "the outbox has the ReviewCreated event for $id, published" \
  || fail "the outbox has the ReviewCreated event for $id, published" "m14_05.outbox: ${row:-no row}"
n=$(sql "SELECT count(*) FROM m14_05.outbox WHERE payload->>'bookId' = 'b2'")
[[ "$n" == 0 ]] && pass "a refused command wrote no event" || fail "a refused command wrote no event" "$n events for b2"

# Kafka: the event really went through the topic, keyed by book.
events() { kafka_tool dump "$TOPIC" 4 | awk -F'\t' -v id="$id" 'index($2, id)'; }
ev=$(events)
if [[ $(echo "$ev" | grep -c .) == 1 ]] && [[ "$(echo "$ev" | cut -f1)" == b1 ]] \
   && echo "$ev" | cut -f2 | jq -e '.eventId and .type == "ReviewCreated" and .bookId == "b1" and .rating == 3' >/dev/null 2>&1; then
  pass "Kafka topic: one ReviewCreated record for the review, key b1, with eventId, bookId and rating"
else fail "Kafka topic: one ReviewCreated record for the review, key b1, with eventId, bookId and rating" "found: ${ev:-nothing}"; fi

# At least once: the relay publishes the same event again; the projection must not count it twice.
sql "UPDATE m14_05.outbox SET published_at = NULL WHERE payload->>'reviewId' = '$id'" >/dev/null
for _ in $(seq 20); do [[ $(sql "SELECT count(*) FROM m14_05.outbox WHERE published_at IS NULL") == 0 ]] && break; sleep 0.5; done
sleep 2
again=$(events | grep -c .); count=$(gql '{ book(id: \"b1\") { reviewCount } }' | jq -r .data.book.reviewCount)
if [[ $again -ge 2 && $count == 3 ]]; then pass "a redelivered event (same eventId) changes nothing: still 3 reviews"
elif [[ $again -lt 2 ]]; then fail "a redelivered event changes nothing" "the relay did not publish the event again after published_at was cleared"
else fail "a redelivered event (same eventId) changes nothing" "reviewCount is now $count"; fi

# The read model is built from events only: an event another service published shows up too.
kafka_tool send "$TOPIC" b3 '{"eventId":"7f1c2a4e-0000-4000-8000-000000000001","type":"ReviewCreated","reviewId":"r-ext-1","bookId":"b3","author":"partner","rating":4,"body":"From the shop."}'
got=
for _ in $(seq 20); do got=$(gql '{ review(id: \"r-ext-1\") { status } book(id: \"b3\") { reviewCount averageRating } }'); [[ "$got" == *PUBLISHED* ]] && break; sleep 0.5; done
echo "$got" | jq -e '.data.review.status == "PUBLISHED" and .data.book.reviewCount == 1 and .data.book.averageRating == 4' >/dev/null 2>&1 \
  && pass "an event published by someone else reaches the read model" || fail "an event published by someone else reaches the read model" "${got:0:200}"
notes_check notes.md 80 m14/05
finish

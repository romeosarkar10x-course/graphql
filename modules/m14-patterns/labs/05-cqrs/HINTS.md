# Hints: m14/05

1. Ask what happens if the process dies between "insert the review" and "send to Kafka", or if Kafka is down: an
   event only in memory is lost. The outbox row is inserted by the same `@Transactional` method as the review, so
   it exists exactly when the review does.
2. The relay is a `@Scheduled(fixedDelay = 500)` method: select unpublished rows `FOR UPDATE SKIP LOCKED`, send each
   with `KafkaTemplate.send(topic, key, payload).get(...)`, then set `published_at`. Mark after the acknowledgement,
   never before: the price is duplicates, which the projection must absorb.
3. Give `processed_events` a primary key on `eventId` and insert into it `ON CONFLICT DO NOTHING` in the same
   transaction as the read-model update: zero rows inserted means "seen before, do nothing". `PENDING` is a query
   over both models: in the read model is `PUBLISHED`, only in the write model is `PENDING`.

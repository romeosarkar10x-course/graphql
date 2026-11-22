# Lab 14.5: Events and CQRS

Lecture: [14.5 Events and CQRS](../../lectures/14.5-cqrs.md). Grading: auto + rubric. Needs PostgreSQL and Kafka
(`bin/course ref up`).

`createReview` is a command. Today the starter writes the review and updates the figures in one go. Split it:

1. **The command** stores the review and a `ReviewCreated` event in `m14_05.outbox`, in **one transaction**, and
   returns the review as `PENDING`.
2. **A relay** publishes unpublished outbox rows to the Kafka topic `${shelfie.topic}`, keyed by book id, and marks
   them published once the broker acknowledged them. The event is JSON with at least `eventId`, `type`
   (`ReviewCreated`), `reviewId`, `bookId`, `author`, `rating`, `body`.
3. **A projection** (a `@KafkaListener`) is the only writer of the read model (`review_view`, `book_stats`) and
   applies each `eventId` **once**: Kafka and the relay deliver at least once.
4. **Queries** read the read model; a review only the write side has is `PENDING`, and the schema says so.

The experiment: create a review and query it within 10 ms (the mutation's own payload) and after 1 s. The grader
also clears an event's `published_at` (a relay crash after sending: the event goes out twice), and publishes an
event to the topic itself, as another service would.

```bash
bin/course start m14/05
cd workspace/m14-patterns/05-cqrs
bin/course shell -c 'mvn -q package && java -jar target/shelfie-cqrs.jar'
bin/course test m14/05
```

The test uses a fresh topic per run (`--shelfie.topic=m14-reviews-...`), so old events never reach the freshly
reset database.

In `notes.md`: within 10 ms and after 1 s, why an outbox, and the costs.

## Done when

`bin/course test m14/05` passes and `notes.md` passes the rubric (`bin/course grade m14/05`).

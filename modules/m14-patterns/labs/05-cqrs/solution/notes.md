# Lab 14.5 notes

## Within 10 ms and after 1 s

The mutation's own payload is the "within 10 ms" read: the review is PENDING and the book still shows 2 reviews at
4.5, because the projection cannot have run before the command's transaction committed. About half a second
later (the relay's 500 ms poll, then Kafka, then the projection) `review(id:)` says PUBLISHED and the book shows 3
at 4.0. The schema states it: `ReviewStatus`, figures that count published reviews only, and a payload that
describes the book "before this review". A client can show "posting..." honestly instead of a count that jumps.

## Why an outbox

Writing the review and then sending to Kafka are two systems: a crash between them loses the event, and sending
first can announce a review whose transaction rolls back. The outbox row commits with the review, and the relay
publishes it afterwards: at least once, because it marks the row only after the broker's acknowledgement. The
grader cleared `published_at` to simulate a crash after sending; the event went out twice and the count stayed 3,
because the projection records each `eventId` in the same transaction as the change it makes.

## Costs

Two models to keep in sync, a lag every screen must handle, and rebuilds: a new read model means replaying the
topic from the start (the grader's foreign event shows the read model only knows events). Worth it when reads and
writes scale differently or several read models serve different screens; not for one table and one screen.

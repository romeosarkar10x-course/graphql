# Lab 4.4 — Safe to retry, safe to race

**Lecture:** [4.4 Idempotency, concurrency, bulk](../../lectures/4.4-idempotency.md) · **Grading:** auto + rubric · **Needs:** nothing (Maven project, in-memory H2)

A phone on a train taps "Post review"; the response is lost in a tunnel; the app retries. Two club
owners rename the club at the same moment. An import of 10,000 ISBNs outlives any HTTP timeout. Three
mutations, three patterns.

## The project

`shelfie-writes`: graphql-java 26.1 over an in-memory H2 database (`Db.java`, given). The schema is in
`src/main/resources/writes.graphqls`. `java -jar target/shelfie-writes.jar` runs the experiment: 50
virtual threads send the same `createReview` with one idempotency key, and it prints how many reviews
exist afterwards.

```bash
../../../bin/course shell -c 'cd workspace/m04-mutations/04-idempotency && mvn -q package && java -jar target/shelfie-writes.jar'
```

## Do

1. Run the experiment on the starter **three times** and write the counts in `notes.md`.
2. **TODO 1, `createReview`:** exactly one review per idempotency key under 50 concurrent requests;
   every other request replays it (`replayed: true`); the same key with a different input is a
   `KEY_REUSED` user error. Let the database decide who wins: the `idem_key` primary key is there.
3. **TODO 2, `renameClub`:** optimistic locking. Update only if `version = expectedVersion`; otherwise a
   `CONFLICT` user error that says the current version. (JPA's `@Version` generates exactly this
   `UPDATE ... WHERE version = ?`; Module 6 uses it.)
4. **TODO 3, `importBooks`:** insert the job, run the import on the `JOBS` executor
   (`CompletableFuture.runAsync`), return the job at once (`QUEUED` or `RUNNING`). Clients poll `job(id)`.
5. Run the experiment again; `notes.md`.

## Done when

`bin/course test m04/04` is green and `bin/course grade m04/04` gives 7/10 or more.

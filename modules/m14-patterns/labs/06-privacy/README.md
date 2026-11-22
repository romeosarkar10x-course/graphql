# Lab 14.6: Auditing, soft delete and privacy

Lecture: [14.6 Auditing, soft delete, privacy](../../lectures/14.6-privacy.md). Grading: auto + rubric. Needs
PostgreSQL (`bin/course ref up`).

Signed in with Module 10's tokens (`bin/course token oluwaseun`, `ada`, `root` for the admin role):

1. **`@mask` with an owner rule**: a user sees their own email and phone whole, everyone else masked.
2. **An audit `Instrumentation`**: every top-level mutation field, whatever its outcome, as one row: when, the
   actor's user id, operation name, field, the **names** of the arguments (never their values), `ok` or `error`.
   `auditLog` is for admins.
3. **Soft delete**: `deleteReview` hides a review from every read at once; its author can `restoreReview` for 30
   days; `purgeDeletedReviews` (admin, and a nightly job) removes older ones for good.
4. **GDPR erase**: `deleteMyAccount` removes the user, all their reviews (soft-deleted ones too) and every trace
   that names them: the audit keeps its rows with the actor `erased`, caches forget them, and **the logs never
   held their personal data** in the first place.

The experiment: Oluwaseun asks to be erased; the grader then searches the API (after warming every cache),
**every table of `m14_06` as text**, the audit log, and the app's log output for their name, email, phone and
review text.

```bash
bin/course start m14/06
cd workspace/m14-patterns/06-privacy
bin/course shell -c 'mvn -q package && java -jar target/shelfie-privacy.jar'
bin/course test m14/06
```

In `notes.md`: the erase verified, what the audit keeps, soft delete and retention.

## Done when

`bin/course test m14/06` passes and `notes.md` passes the rubric (`bin/course grade m14/06`).

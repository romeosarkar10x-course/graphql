# Hints, lab 4.4

1. "Check, then act" is the bug: between the SELECT and the INSERT, 49 other threads also checked. Reverse it: try to INSERT the key first, in the same transaction as the review. A duplicate key throws `SQLIntegrityConstraintViolationException` (H2's exception is a subclass); that thread rolls back and reads the winner's row.
2. Turn off auto-commit (`conn.setAutoCommit(false)`) so the key, the review and the link commit together; a loser's INSERT waits for the winner's commit (that is what `LOCK_TIMEOUT` in `Db` is for) and only then fails. Store something that identifies the request (book, rating, body) with the key, so a reused key with a different body can be told apart.
3. For renames, `executeUpdate()` returns the number of rows changed: 1 means you held the right version, 0 means someone else got there first (or the club does not exist: check which).

# Hints, game 10.10

1. Work from the flag list in your lab 10.9 notes: for each, name the one line of reasoning the server got
   wrong ("persisted means trusted", "requests not attempts", "the token proves the role") before
   touching code. Most fixes are small.
2. Four fixes are in `CtfSecurity.java` (the token decoder, the cookie, the `text/plain` filter, error
   messages), one is a demand limit, and the rest are authorization: the same row and field rules as lab
   10.2, applied on every path (`node`, `book.reviews`, `review(id:)`, the subscription, the club log).
3. For the brute force, count `joinClub` calls per reader in the resolver, not requests in an interceptor.
   For mass assignment, the safest input type has no field the caller may not set.

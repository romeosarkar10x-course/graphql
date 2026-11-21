# Error detective: reference explanations (grader and tutor only)

1. **B, `User.handle`.** The path is `["club", "members", 4, "handle"]`: user u4 (Dev Patel) came from the
   import with no handle. `handle: String!` nulls u4, `[User!]!` can't hold a null item, `members` is
   non-null, so `club` is null: the whole page. Fix the data, and decide in the schema whether `handle`
   can really be absent (lab 7.4). Other clubs are fine because u4 isn't in them.
2. **F, `Meetup.startsAt`.** The response's `errorId` is `42707928`. The log line with that id is a
   `DateTimeParseException` for `2026-11-31` ("Invalid date 'NOVEMBER 31'") at `/club/meetups[2]/startsAt`. The S3 error
   (`5be13d2a`, cause J) is an unrelated request: the distractor. `meetups: [Meetup!]!` and
   `startsAt: String!` take the club with it.
3. **D, `Club.memberCount`.** No `data` key, a `ValidationError`: `FieldUndefined` for `Club.memberCount`.
   iOS 4.2 still sends it, Android 5.0 doesn't. A removed field is a breaking change for every client
   still in the field (lab 7.6). Deprecate, measure usage, then remove.
4. **E, `Mutation.rateBook(rating:)`.** Variable coercion: `"rating": "5"` is a string for `Int!`. A request
   error (no `data`). The new form sends the input value as text.
5. **G, `Book.reviews`.** `ExecutionAborted` with no path. The budget is 500 ms. The reviews fetcher
   logs its query at :00.178 and finishes at :01.079, 900 ms later, and the abort is reported right
   after. graphql-java checks cancellation between fetchers, so the slow fetcher ran to the end
   anyway. Only this book has the slow reviews query.
6. **C, `Club.owner`.** Both `owner.name` and `owner.handle` are null, which is what happens when the
   owner fetcher returns the id string `"u11"` instead of the user: the default property fetcher finds
   no `name` or `handle` on a `String`. That is the "faster owner lookup" change. B is the trap.

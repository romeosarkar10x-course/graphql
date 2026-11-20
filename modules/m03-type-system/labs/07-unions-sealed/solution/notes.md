# Lab 3.7 notes (reference)

## The starter's bug

The resolver called the `BookFinished` record a `MeetupScheduled`, so the `... on MeetupScheduled`
fragment applied: `title` and `startsAt` came from the default fetcher on a record that has neither,
and `title` is `String!`: a NullValueInNonNullableField error at `/feed[3]/title`; `[FeedItem!]!`
under a non-null root field passes it all the way up, so `data` is null. With nullable fields it
would have been plausible-looking nulls and no error at all. Either way
the bug is in the resolver and is found at runtime.

## The compiler error

`error: the switch expression does not cover all possible input values` at the type resolver's
`switch`. The build fails where the new case must be handled.

## Question 1

Interface: clients can select `at` without fragments and sort or group by it; a shared field is the
textbook case for an interface. Union: the items are not "kinds of one thing" beyond the timestamp,
and an interface invites adding more shared fields that do not fit all members; a union also lets
members come from other teams' types. Both are defensible; a common answer is a union plus `at` on
every member, or an interface `FeedItem { at }`.

## Question 2

Clients that assume they know every member: an exhaustive `switch` on `__typename` with no fallback,
or a UI that throws on unknown types. Clients should always select `__typename`, handle an
unknown/default case (skip or "update the app"), and never rely on the union being closed.

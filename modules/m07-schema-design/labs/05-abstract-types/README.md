# Lab 7.5 — One feed, three ways

**Lecture:** [7.5 Choosing abstract types](../../lectures/7.5-abstract-types.md) · **Grading:** auto + rubric · **Needs:** nothing

A club's activity feed has three kinds of item: a review was posted, a member joined, a meetup was
scheduled. Each has an id, a time (`happenedAt`) and an actor (`User`), plus its own data: a `Review`,
an optional `welcomeMessage`, a `Meetup`. Model it three ways, then ship a fourth kind and count what
changes.

## Do

1. `feed-interface.graphqls` and its client `feed-interface.graphql` are done: read them as the example.
2. `feed-union.graphqls`: `union FeedItem = ReviewPosted | MemberJoined | MeetupScheduled`.
3. `feed-enum.graphqls`: one `type FeedItem` with `kind: FeedItemKind!` (an enum of the three kinds)
   and every kind's fields on it, nullable.
4. A client per design (`feed-union.graphql`, `feed-enum.graphql`) that shows every kind's data.
   Keep `Query.feed(clubId: ID!): [FeedItem!]!` in all three.
5. **v2**: in `v2/`, each design plus `BookFinished` (an actor finished a `book: Book!`), and each client
   updated to show it. The v1 clients must stay valid against v2.
6. `notes.md`.

Check a pair yourself: `../../../bin/course design diff feed-union.graphqls v2/feed-union.graphqls` and
`../../../bin/course design clients v2/feed-union.graphqls feed-union.graphql`. The test also prints, per
design, how many mock feed items a **v1** client receives blank from the v2 schema.

## Experiment

Add the fourth variant to each, count client changes. Then compare with graphql-inspector:
`../../../bin/course inspector diff feed-union.graphqls v2/feed-union.graphqls` (needs Docker on the host;
the first run builds a small Node.js image).

## Done when

`bin/course test m07/05` is green and `bin/course grade m07/05` gives 7/10 or more.

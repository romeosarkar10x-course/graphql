# Lab 7.3 — References, not keys

**Lecture:** [7.3 Modelling relationships](../../lectures/7.3-relationships.md) · **Grading:** auto + rubric · **Needs:** nothing

The starter's `relations.graphqls` is the database with a GraphQL accent: `ownerId`, `currentBookId`,
`authorId`, and `memberIds` next to `memberRoles`, two lists the client must zip by position. Every
relationship costs the client another request.

## Do

1. `relations.graphqls`: replace every key with the object it points at (`owner: User!`,
   `currentBook: Book`, `author: User!`, `book: Book!`, `clubs: [Club!]!`). Keep the four `Query` fields.
2. Make `Club.members(first: Int = 10, after: String)` a **connection**:
   - `ClubMemberConnection { edges: [ClubMemberEdge!]! pageInfo: PageInfo! totalCount: Int! }`
   - `ClubMemberEdge { cursor: String! node: User! role: ClubRole! joinedAt: String! }`
   - `PageInfo { hasNextPage: Boolean! endCursor: String }`, `enum ClubRole { OWNER MODERATOR MEMBER }`
   The cursor format is yours (opaque to clients; the tests never look inside it). Module 8 does
   pagination properly; here the point is the **edge carrying data about the pair**.
3. `Relations.java`: the fetchers. `Memberships.java` gives each membership's role and join date; don't
   edit it or `Data.java`.
4. `clients/review-card.graphql`: rewrite the three-operation review card as **one** operation.
5. `notes.md`.

Check: `../../../bin/course design lint relations.graphqls --rules references` and
`../../../bin/course design clients relations.graphqls clients/`.

## Experiment

Run `design clients` with your new schema on the **starter's** `review-card.graphql`. That error is what
every deployed client would see if you removed `authorId` in one release. Write it down (notes §4).

## Done when

`bin/course test m07/03` is green and `bin/course grade m07/03` gives 7/10 or more.

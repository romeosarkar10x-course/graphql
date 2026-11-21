# ADR-007: How Shelfie's schema evolves

Status: accepted · Date: 2026-11-21 · Deciders: (reference solution)

## Context

Six change requests arrived over one release cycle: Markdown descriptions, member roles, several
authors per book, half-star ratings, mixed search, and finally the retirement of release 0. Release-0
apps are installed on phones and never update, so every step had to keep their five operations valid
until request 6 retired them; release-1 apps then had to keep working through the clean-up.

## Decision

1. **Add, never change.** A field's type, nullability or arguments never change in place. A new
   capability is a new field next to the old one (`about` beside `description`, `authors` beside
   `author`, `score` beside `rating`), because SchemaDiff calls any in-place change of a used field
   BREAKING and the deployed apps would agree.
2. **Name the replacement for what it is, not "v2".** `about`, `authors`, `score`, `memberships`: the
   new name has to be good for years, since it outlives the old one. No `description2`, no `ratingV2`.
3. **Deprecate in the same step that adds the replacement**, with a reason that names the replacement
   ("Use score: it has half stars."). The spec's default reason, "No longer supported", is not allowed:
   it tells a client developer nothing.
4. **Old fields keep working with real values.** `rating` is computed from `score` (rounded half up), so
   a release-0 strip shows correct whole stars, not zeros.
5. **Remove only with evidence.** A deprecated field goes only when usage for every deployed client is
   zero for a full release cycle. Request 6 met that for `description` and `author`, not for `rating`.
6. **Every step passes the gate**: deployed clients valid, SchemaDiff with nothing breaking (or only the
   agreed removals), and the schema still valid with every deprecated element removed (RFC #1231).

## The six steps

| Step | Request | What you added | What you deprecated | Riskiest choice |
|---|---|---|---|---|
| 1 | Markdown descriptions | `Club.about: String` | `Club.description` | Naming: `about` is final, `descriptionMarkdown` would have leaked a format into the name |
| 2 | Roles | `Club.memberships`, `Membership`, `ClubRole` | nothing (`members` is still right for a plain list) | `ClubRole` is an enum: adding `GUEST` later is dangerous for clients with exhaustive switches |
| 3 | Several authors | `Book.authors: [Author!]!`, `Author` | `Book.author` | `author` must keep returning something for multi-author books: the first author's name |
| 4 | Half stars | `Review.score: Float!` | `Review.rating` | `Float` allows 3.14; a `Rating` scalar or enum of half steps would encode the rule in the type |
| 5 | Search | `Query.search`, `union SearchResult = Book \| Club` | nothing | A union: adding `User` results later is dangerous (an old client gets an empty object) |
| 6 | Release 0 retired | nothing | (removed `Club.description`, `Book.author`) | The only deliberate breaking release; approved on usage data, not on a diff |

## Consequences

The schema carries both shapes of three fields for a whole release cycle, and `Review.rating` is still
there after step 6: release 1 reads it in the review strip. It can go only after that strip moves to
`score` in a release that the usage logs then show as the whole fleet. Costs: a bigger schema, resolver
code for derived legacy fields, and clean-up that takes months instead of a day. Gains: no app in anyone's
pocket broke, no `/v2` to run beside `/v1`, and each change was reviewed on its own.

## Alternatives considered

- **A `/v2` endpoint with the new shapes.** Two schemas to run, secure and document; clients migrate all at
  once instead of field by field. Lost: GraphQL's per-field selection makes versions unnecessary.
- **Change `rating` to `Float` in place.** Every release-0 client typed `rating` as an integer; a 3.5
  breaks their parsing even though the documents stay valid (Jenga card 06's lesson). Lost.
- **A `Rating` object (`rating { stars halfStars }`).** Turning a scalar field into an object field is
  breaking, so it would still need a new name. Kept as the better design for a new schema, rejected here
  for the extra nesting on every review.

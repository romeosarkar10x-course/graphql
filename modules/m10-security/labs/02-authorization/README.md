# Lab 10.2: Authorization

Lecture: [10.2 Authorization](../../lectures/10.2-authorization.md). Grading: auto. Needs nothing: from this
lab on, tokens are verified offline with the course public key, and tests mint them with `bin/course token`.

The starter is lab 10.1's solution. It knows who is calling, but lets everyone do everything. Add the rules
from the schema's descriptions (`contract/secure.graphqls`), in the business layer, so that no path to the
data skips them.

| Rule | Kind |
|---|---|
| A private club (c1, c3) and its reviews are visible to its members and admins only, on **every** path: `club`, `clubs`, `node`, `Reader.clubs`, `Book.reviews`, `Review.club` | row |
| `Reader.email`: the reader themself and admins; null for everyone else | field |
| `Club.inviteCode`: the club's owner and admins; null for everyone else | field |
| `readers`: moderators (and admins), enforced by `@auth(role: MODERATOR)` in the schema | role |
| `addReview`: members of the club | write |
| `removeReview`: the author, a moderator, or the club's owner | write |
| `renameClub`: the club's owner | write |

Refusals are GraphQL errors classified `UNAUTHORIZED` (no token) or `FORBIDDEN` (signed in, not allowed).
Spring for GraphQL classifies Spring Security's exceptions that way for you.

## What to build

1. One `Access` component that holds every rule; resolvers and `node(id:)` ask it.
2. `@PreAuthorize` on a service that does the writes (`@EnableMethodSecurity`).
3. A `SchemaDirectiveWiring` for `@auth(role:)`, registered with a `RuntimeWiringConfigurer`.
4. The experiment: write `src/test/resources/my-forbidden.hurl` yourself first, at least 10 titled entries
   (`# F01 ...`), each trying a forbidden path as anonymous, ada, ben, max, olu or root. Then run the
   course's 29.

```bash
bin/course start m10/02
bin/course token ben --header        # paste into GraphiQL's headers, or use it with curl
bin/course test m10/02
```

Users: ada (member of c1, owns c2), ben (member of c2, owns c3), max (moderator, member of c1),
olu (owns c1), root (admin). Reviews v1 to v30 are in c1, v61 on in c3.

## Done when

`bin/course test m10/02` passes: the course's 29 checks (`tests/forbidden.hurl`) and your own suite
against your server, `@PreAuthorize` on the writes, and the directive wired.

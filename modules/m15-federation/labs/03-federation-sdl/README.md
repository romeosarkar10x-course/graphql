# Lab 15.3: Federation concepts in SDL

Lecture: [15.3 Federation concepts](../../lectures/15.3-federation-concepts.md). Grading: auto + rubric. Needs the
federation tools (`bin/course fed`, fetched by the test itself).

`subgraphs/` holds what each team owns, written as plain GraphQL: `accounts`, `catalog`, `reviews`, `clubs`.
They do not compose yet: four teams define `User`, `Book` and `Media` without agreeing on anything. Annotate them
with Federation 2 (`extend schema @link(url: "https://specs.apollo.dev/federation/v2.9", import: [...])`) so that
`supergraph.yaml` composes and the supergraph says:

| What | Directive |
|---|---|
| `User` is an entity (key `id`) owned by accounts; `Book` by catalog, with two keys, `id` and `isbn`; `Review` (reviews) and `Club` (clubs) by `id` | `@key` |
| reviews adds `User.reviews`; clubs adds `User.clubs` and `Book.clubsReading` | `@key` on their own copies |
| `User.handle` is answered by accounts, and by reviews when the router reaches a user through `Review.author` | `@shareable`, `@external`, `@provides` |
| clubs computes `Book.weeksToRead` from catalog's `pages` | `@external`, `@requires` |
| `User.clubCount` is moving from accounts to clubs, 25% of traffic for now | `@override(from:, label: "percent(25)")` |
| `User.email` stays in the supergraph, never in the API | `@inaccessible` |
| catalog's `interface Media` (`Book`, `Audiobook`): reviews adds `reviews`, `reviewCount`, `averageRating` to every `Media` without knowing the implementations | `@key` on the interface, `@interfaceObject` |
| `Query.books`, `Query.book`, `Book.title`, `Book.authors` belong to a public contract | `@tag(name: "public")` |

The test composes your files with The Guild's composition library (MIT; [contract/compose](../../contract/compose/)),
the same rules Rover and Hive apply, and reads the supergraph's `join__` directives. Then the experiment: it
removes catalog's `@key`s from `Book` and composes again, which must fail. Run it yourself first and read the error.

```bash
bin/course start m15/03
cd workspace/m15-federation/03-federation-sdl
bin/course shell -c 'source /course/tools/testlib.sh; source /course/modules/m15-federation/contract/fed.sh; compose supergraph.yaml supergraph.graphql api.graphql'
bin/course test m15/03
```

`api.graphql` is the schema clients will see. In `notes.md`: the error, which directive solves which team problem,
and what composition checked that a review would miss.

## Done when

`bin/course test m15/03` passes and `notes.md` passes the rubric (`bin/course grade m15/03`).

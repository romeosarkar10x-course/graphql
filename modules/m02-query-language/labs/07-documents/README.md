# Lab 2.7 — Documents and hygiene

**Lecture:** [2.7 Documents and hygiene](../../lectures/2.7-documents.md) · **Grading:** auto + rubric · **Needs:** `bin/course ref up`

Every screen and job in Shelfie gets its own named operation in its own file. These 12 files are
checkpoint 02's operation set, and later modules reuse them.

## Do

1. `bin/course start m02/07`, then work in `workspace/m02-query-language/07-documents/`.
2. `ops/` has two operations already. Add the other ten, one named operation per file
   (`NN-kebab-case.graphql`), each starting with a `# Description:` line. Put variables in a
   `.json` file with the same name (`02-club-card.json`).

   | Operation | Serves |
   |---|---|
   | `Hello` | Smoke test (given) |
   | `ClubCard` | Compact card (given) |
   | `ClubPage` | Module 1's club page contract |
   | `UserProfile` | A member's profile, clubs and reviews |
   | `BookDetails` | Book page; `$reviewsLast: Int = 5` |
   | `CompareClubs` | Two clubs side by side |
   | `SearchEverything` | The search box over the `SearchResult` union |
   | `BookSearch` | Title search with a limit |
   | `ClubList` | All clubs, one row each |
   | `UpcomingMeetups` | A club's upcoming meetups |
   | `ClubCardVariant` | Compact or detailed card with a Boolean variable |
   | `UserReviews` | Everything one member reviewed |

   Across the set, use at least one alias, one named fragment, one inline fragment and one directive.
   `../../../bin/course gql operations ops/04-user-profile.graphql` shows what a file contains.
3. Write `multi.graphql` (two named operations) and run the experiments in `notes.md`.

## Done when

`bin/course test m02/07` is green and `bin/course grade m02/07` gives 7/10 or more.

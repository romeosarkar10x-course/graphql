# Lab 15.1 notes

## The four teams and their boundaries

- accounts: who people are (`User`'s own fields, `me`, `updateProfile`, the want-to-read list, which is a
  property of the reader, not of the book).
- catalog: what books are (`Book`'s bibliographic fields, `books`, `book`, `bookByIsbn`).
- reviews: what people think of books (`Review`, and the fields it adds to others: `Book.reviews`,
  `reviewCount`, `averageRating`, `User.reviews`).
- clubs: people reading together (`Club`, `Meetup`, and `User.clubs`, `User.attending`, `Book.clubsReading`).

The checker counted accounts 11 fields, catalog 10, reviews 13, clubs 22. Clubs is the biggest because meetups
came with it; if it grows, `Meetup` is the next split. Only `Book` and `User` need keys for the cross-team
references the monolith has today; `Review` and `Club` get keys anyway because the next feature will point at
them (a meetup about a review thread, a club's review digest).

The rule I followed: a field belongs to the team that holds the data it is computed from, not the team whose
type it is on. `Book.averageRating` is a book's field and reviews' data, so reviews owns it.

## Club book of the month: coordination cost under three models

The feature: each club picks a book of the month; the club page shows it with its title, the top review and the
reviewer's name. Touches clubs (the pick), catalog (title), reviews (top review), accounts (name).

| | monolith (one team, one schema) | BFF over four services | federated graph (four subgraphs) |
|---|---|---|---|
| schema change | 1 PR, 1 reviewer, 1 deploy | BFF schema PR + clubs API PR + reviews API PR ("top review" endpoint) | clubs adds `Club.bookOfTheMonth`, reviews adds `Book.topReview`: 2 subgraph PRs, composition checks both |
| teams that must agree on a contract | 0 (same codebase) | 3: BFF team with clubs and with reviews | 2: clubs and reviews, each with the shared graph; catalog and accounts do nothing |
| meetings (my estimate) | 0-1 | 3 (one per API, plus a BFF review) | 1 (who owns "top": highest rating? newest?) |
| deploys in order | 1 | 3, services before BFF | 2, independent: either can ship first, the field appears when both have |
| who waits | nobody, but everyone shares one release train | the BFF team waits for both services | nobody waits for the platform team |

In a monolith the feature is cheapest today but the team is the bottleneck for every other feature too. The
BFF puts a team between every product team and the client, so every feature costs one more handover. Federation
spends the coordination on the schema itself: two teams meet once to agree on `topReview`, composition checks
the rest, and the router writes the join that the BFF team would have written by hand.

## Where the map will hurt

- `Book.clubsReading` and `User.clubs` make clubs' subgraph a dependency of the book and profile pages: a clubs
  outage now costs fields on pages clubs does not own (they must be nullable or the page fails, lab 15.6).
- The want-to-read list sits with accounts, but "readers who want this book" would sit with catalog's pages:
  the same data asked from both sides is a sign of a missing boundary.
- Conway's law says the graph will drift towards the org chart; if clubs and meetups get separate teams, the
  ownership map changes, and with it the subgraphs.

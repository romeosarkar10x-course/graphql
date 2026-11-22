# Retrospective: club book of the month

Solo run: I played all four teams, one commit per team, in the order a real deadline would force.

## What each team shipped, and in which order

1. **catalog**: `Book.blurb` ("Good Omens by Terry Pratchett and Neil Gaiman (1990)"). Self-contained: title,
   authors and year are catalog's own data. Shipped first because nothing depended on anything else.
2. **accounts**: `User.displayName` ("Asha Rao (@asha)"). Also self-contained, but every page that shows a
   person starts using it, so it went out before the pages did.
3. **reviews**: `Book.topReview`. Reviews already adds fields to `Book` (an entity it does not own), so this was
   one more field on the same `@key(fields: "id")` stub; no other team involved.
4. **clubs**: `Club.bookOfTheMonth` and `pickBookOfTheMonth`. Last, because the page is only useful when the book
   it points at has a blurb and a top review. Clubs returns `Book` and `User` as key-only stubs; the router fetches
   the rest from catalog, reviews and accounts.
5. **platform**: the router forwarded no headers, so `pickBookOfTheMonth` saw an anonymous caller and refused even
   the owner. One line in `router.yaml` (`propagate: x-shelfie-user`); the kind of change nobody owns until a
   feature needs it.

Any order would have composed: each field is new, and federation lets a field appear when its subgraph ships.
The order only decided when the page was complete.

## Where we had to agree

- What "top" means: highest rating, newest on a tie (the higher review number). Written in the schema's
  description, because the other teams read it there, not in reviews' code.
- Whether clubs validates the book: it cannot see the catalogue. It checks the id's shape and trusts the router
  to find the book; a wrong id shows up as `book: null` plus an error on the page, which is the catalog team's
  honest answer. A stricter rule would need clubs to call catalog, a dependency between subgraphs that the
  graph was meant to avoid.
- The month format (`YYYY-MM`): a `String` with a description, because no team owns a `Month` scalar.

## What composition caught, and what it did not

It caught nothing this time, and that was the point of four additive changes. It would have caught a second
`blurb` defined by reviews without `@shareable`, or a `topReview` returning a type reviews does not declare.
It cannot catch the things that failed during the run: the missing header propagation (a runtime config), the
non-owner pick (behaviour), and a `weeksToRead` that clubs answers wrong when catalog's `pages` is wrong (data).
Those needed the page test, run through the router, which is what each team's CI should run against the
composed graph, not only against its own subgraph.

`docker compose up --wait` brought the whole stack up in one command (build, four subgraphs, supergraph from
`_service`, router); the club page answered on localhost:4000.

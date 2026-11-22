# Schema review board: decisions

Date: 2026-11-23
Board (solo run, one person in every seat): chair; catalog, reviews, community and platform owners; client
representatives for web and iOS; API product (partners); SRE (cost and load).

## Proposal 1: Book.price

- Decision: **accept with changes**.
- Reasons: additive, so no client breaks, and the partner has a dated need. But `Float` euros fails three ways
  the thread already named: floats round (0.1 + 0.2), the currency is implicit, and price is per shop, not per
  book. The style guide's money rule applies: an amount in minor units plus a currency code.
- Change required: `Book.offers: [Offer!]!` with `type Offer { shop: Shop! price: Money! url: String }` and
  `type Money { amount: Int! "minor units, e.g. cents" currency: String! "ISO 4217" }`.
- Conditions: catalog owns `Offer` and `Money` (CODEOWNERS lines in the same pull request); `offers` is capped
  (`first`, default 5) so cost stays bounded.
- Dissent: catalog wanted the simple field now and the shape later. The board's view: a public field cannot be
  reshaped later without a version cycle (lab 16.7), so get it right once.

## Proposal 2: Review.rating as Float

- Decision: **reject as written; accept an additive alternative**.
- Reasons: changing an output type from `Int` to `Float` breaks every client that selects `rating` with
  generated types (the iOS note), and the usage data shows web and Android select it. Changing the argument
  type breaks Android's `Rate` mutation, the only writer. Old iOS versions stay installed for months, so this
  would need a dated version and nine months of overlap, all for half a star.
- Alternative accepted: add `Review.score: Float!` (0.5 steps) and an optional `addReview(score: Float)` argument
  that wins over `rating` when given; keep `rating` as the score rounded down; deprecate `rating` with reason
  "Use score" once web and Android read `score`.
- Conditions: reviews team adds usage alerts on `Review.rating` so removal is decided from data (lab 16.3).
- Dissent: none.

## Proposal 3: searchBooks(input:)

- Decision: **accept the addition, reject the removal for now**.
- Reasons: `searchBooks` with filters and pagination is the right shape. Removing `search` this sprint breaks
  web's `Search` operation, about 10% of web traffic, with no notice. Web deploys daily, so the window can be short,
  but it has to exist, and it has to be measured.
- Plan: add `searchBooks` now; deprecate `search` ("Use searchBooks") in the same release; web migrates; remove
  `search` once the usage reports show zero operations for 14 days (web is the only client and deploys daily;
  a partner-facing field would get the full version cycle).
- Conditions: platform keeps the old resolver as a thin call into the new one, so there is one implementation.
- Dissent: platform, on maintaining two entry points; accepted because the old one becomes three lines.

## Proposal 4: Club.membersConnection

- Decision: **accept**.
- Reasons: unbounded lists are the one thing the style guide forbids on growing collections, and the cost rule
  already treats `members` as 6 items, which open clubs would make false (SRE: the abusive-client incident in
  lab 16.8 was exactly an unbounded fan-out). Additive plus a deprecation, so nobody breaks today.
- Conditions: `first` has a maximum (100) enforced by the server; the cost rule uses `first`; `totalCount` is
  cheap (a count query) or it is not offered. Deprecate `members` only when the open-clubs launch date is set,
  with the usage-driven removal of lab 16.3. iOS 2.8.0's `memberCount` removal is unrelated and stays on its own
  schedule.
- Dissent: web asked to keep `members` for small private clubs; the board kept it (deprecated, not removed) and
  will revisit with usage data in 90 days.

## Follow-ups

| What | Owner | When |
| --- | --- | --- |
| `Offer` and `Money` schema pull request | catalog | 2026-12-04 |
| `Review.score` and usage alert on `rating` | reviews | 2026-12-11 |
| `searchBooks`; deprecate `search` | platform | 2026-12-04 |
| `membersConnection` with a 100 cap | community | 2026-12-18 |

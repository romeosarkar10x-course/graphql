# Schema Jenga: bets

For each card in `../../../modules/m07-schema-design/labs/08-schema-jenga/tower/deck/` (the first line
says what it changes): bet **BREAKS** (at least one frozen client becomes invalid) or **HOLDS**, before
running anything. Then settle it and fill the other three columns.

| # | Card | Bet | Tower | Clients broken | SchemaDiff |
|---|---|---|---|---|---|
| 01 | remove User.bio | BREAKS | BREAKS | 1 | BREAKING |
| 02 | remove Membership.joinedAt | HOLDS | HOLDS | 0 | BREAKING |
| 03 | rename Club.description to about | BREAKS | BREAKS | 1 | BREAKING |
| 04 | Club.members(first:) becomes Int! with no default | HOLDS | HOLDS | 0 | BREAKING |
| 05 | Book.averageRating becomes Float! | HOLDS | HOLDS | 0 | SAFE |
| 06 | Book.publishedYear changes from Int to String | HOLDS | HOLDS | 0 | BREAKING |
| 07 | add ClubRole.GUEST | HOLDS | HOLDS | 0 | DANGEROUS |
| 08 | Book.reviews gets a required minRating: Int! argument | BREAKS | BREAKS | 2 | BREAKING |
| 09 | User.avatarUrl becomes avatar: Image (an object with url and alt) | BREAKS | BREAKS | 4 | BREAKING |
| 10 | Query.user(handle:) becomes user(id: ID!) | BREAKS | BREAKS | 1 | BREAKING |

Score: (reference copy: the bets are the answers) 10 of 10.

## After the game

Cards 02, 04 and 06 are BREAKING for SchemaDiff, yet no frozen client broke: nobody selects
`Membership.joinedAt`, and every client that calls `members` passes `first`, so `Int!` without a default
changes nothing for them. A diff gate would have blocked two harmless changes. Card 06 is the opposite
warning: the tower holds (`publishedYear` is still a leaf every client may select) but an app that parses
it as a number now gets `"1969"` and fails at run time, so "valid" is not "unbroken". Card 05 is SAFE and
holds. The gate I'd build: SchemaDiff for the classification, then validate the operations real clients
send (the frozen set here; in production the trusted-documents registry of Module 10 or field usage from
Module 16) and fail only when a used coordinate breaks. Type changes on used fields always need a human,
even when every document stays valid.

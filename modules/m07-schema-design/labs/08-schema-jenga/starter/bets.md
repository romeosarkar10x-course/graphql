# Schema Jenga: bets

For each card in `../../../modules/m07-schema-design/labs/08-schema-jenga/tower/deck/` (the first line
says what it changes): bet **BREAKS** (at least one frozen client becomes invalid) or **HOLDS**, before
running anything. Then settle it and fill the other three columns.

| # | Card | Bet | Tower | Clients broken | SchemaDiff |
|---|---|---|---|---|---|
| 01 | remove User.bio | | | | |
| 02 | remove Membership.joinedAt | | | | |
| 03 | rename Club.description to about | | | | |
| 04 | Club.members(first:) becomes Int! with no default | | | | |
| 05 | Book.averageRating becomes Float! | | | | |
| 06 | Book.publishedYear changes from Int to String | | | | |
| 07 | add ClubRole.GUEST | | | | |
| 08 | Book.reviews gets a required minRating: Int! argument | | | | |
| 09 | User.avatarUrl becomes avatar: Image (an object with url and alt) | | | | |
| 10 | Query.user(handle:) becomes user(id: ID!) | | | | |

Score: one point per correct bet. My score: 

## After the game

Which cards did SchemaDiff call BREAKING while the tower held? What does that say about using a
schema diff alone as the CI gate, and what would you add?

# Game 18.8: one supergraph, many languages. Read-only, through the router. Variable: router.

# Y01 accounts alone: every reader
POST {{router}}
```graphql
{ users { id handle } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.users" count == 12
jsonpath "$.data.users[0].handle" == "asha"

# Y02 one book from all four subgraphs: catalog's title and pages, reviews' figures, accounts' names, clubs' readers
POST {{router}}
```graphql
{ book(id: "b1") { title pages weeksToRead reviewCount averageRating reviews { body author { name city } } clubsReading { name } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.book.title" == "The Left Hand of Darkness"
jsonpath "$.data.book.weeksToRead" == 4
jsonpath "$.data.book.reviewCount" == 5
jsonpath "$.data.book.averageRating" == 4.2
jsonpath "$.data.book.reviews[3].author.name" == "Jonas Berg"
jsonpath "$.data.book.reviews[3].author.city" == "Oslo"
jsonpath "$.data.book.clubsReading[0].name" == "The Midnight Readers"

# Y03 the signed-in reader, forwarded by the router to every subgraph that needs it
POST {{router}}
X-Shelfie-User: u1
```graphql
{ me { name clubs { name } reviews { book { title } } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.me.name" == "Asha Rao"
jsonpath "$.data.me.clubs" count == 2
jsonpath "$.data.me.reviews" count == 3
jsonpath "$.data.me.reviews[1].book.title" == "Piranesi"

# Y04 reviews first, then the rest: the plan crosses languages in the other direction
POST {{router}}
```graphql
{ latestReviews(first: 2) { rating author { handle name } book { title year clubsReading { name owner { handle } } } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.latestReviews[0].book.title" == "Station Eleven"
jsonpath "$.data.latestReviews[0].author.name" == "Asha Rao"
jsonpath "$.data.latestReviews[1].book.year" == 1989

# Y05 clubs with everything
POST {{router}}
```graphql
{ clubs { name memberCount owner { name } currentBook { title averageRating } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.clubs" count == 3
jsonpath "$.data.clubs[1].owner.name" == "Grace Kim"
jsonpath "$.data.clubs[2].currentBook.averageRating" == 4.5

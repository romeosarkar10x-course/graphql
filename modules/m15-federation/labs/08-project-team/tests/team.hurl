# T01 the club page: four teams' fields in one answer
POST {{url}}
```graphql
{
  club(id: "c1") {
    name
    bookOfTheMonth {
      month
      book { blurb weeksToRead topReview { rating body author { displayName } } }
      pickedBy { displayName }
    }
  }
}
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.club.bookOfTheMonth.month" == "2026-11"
jsonpath "$.data.club.bookOfTheMonth.book.blurb" == "Project Hail Mary by Andy Weir (2021)"
jsonpath "$.data.club.bookOfTheMonth.book.weeksToRead" == 5
jsonpath "$.data.club.bookOfTheMonth.book.topReview.body" == "Finally, physics."
jsonpath "$.data.club.bookOfTheMonth.book.topReview.author.displayName" == "Jonas Berg (@jonasb)"
jsonpath "$.data.club.bookOfTheMonth.pickedBy.displayName" == "Kemi Adeyemi (@kemia)"

# T02 no pick yet
POST {{url}}
```graphql
{ club(id: "c2") { name bookOfTheMonth { month } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.club.name" == "Slow Burn Mysteries"
jsonpath "$.data.club.bookOfTheMonth" == null

# T03 only the owner picks: a member is refused, and nothing changes
POST {{url}}
X-Shelfie-User: u5
```graphql
mutation { pickBookOfTheMonth(clubId: "c2", bookId: "b3", month: "2026-12") { id } }
```
HTTP 200
[Asserts]
jsonpath "$.data.pickBookOfTheMonth" == null
jsonpath "$.errors[0].message" exists
POST {{url}}
```graphql
{ club(id: "c2") { bookOfTheMonth { month } } }
```
HTTP 200
[Asserts]
jsonpath "$.data.club.bookOfTheMonth" == null

# T04 the owner picks, and the page shows it at once
POST {{url}}
X-Shelfie-User: u7
```graphql
mutation { pickBookOfTheMonth(clubId: "c2", bookId: "b3", month: "2026-12") { bookOfTheMonth { month book { blurb topReview { body } } pickedBy { displayName } } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.pickBookOfTheMonth.bookOfTheMonth.month" == "2026-12"
jsonpath "$.data.pickBookOfTheMonth.bookOfTheMonth.book.blurb" == "The Remains of the Day by Kazuo Ishiguro (1989)"
jsonpath "$.data.pickBookOfTheMonth.bookOfTheMonth.book.topReview.body" == "Quietly crushing."
jsonpath "$.data.pickBookOfTheMonth.bookOfTheMonth.pickedBy.displayName" == "Grace Kim (@gracek)"

# T05 the owner with a month that is not YYYY-MM
POST {{url}}
X-Shelfie-User: u7
```graphql
mutation { pickBookOfTheMonth(clubId: "c2", bookId: "b4", month: "December") { id } }
```
HTTP 200
[Asserts]
jsonpath "$.data.pickBookOfTheMonth" == null
jsonpath "$.errors[0].message" exists

# T06 the top review: highest rating, newest on a tie; none without reviews
POST {{url}}
```graphql
{ a: book(id: "b1") { topReview { id rating } } b: book(id: "b9") { topReview { id } } c: book(id: "b7") { topReview { id } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.a.topReview.id" == "r3"
jsonpath "$.data.b.topReview.id" == "r12"
jsonpath "$.data.c.topReview" == null

# T07 blurbs and display names everywhere, not only on the club page
POST {{url}}
```graphql
{ book(id: "b5") { blurb } latestReviews(first: 1) { author { displayName } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.book.blurb" == "Good Omens by Terry Pratchett and Neil Gaiman (1990)"
jsonpath "$.data.latestReviews[0].author.displayName" == "Asha Rao (@asha)"

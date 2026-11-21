# Module 6: 14 Shelfie behaviour checks (mini-project 5's S01-S14), for every Spring, DGS and Quarkus Shelfie.
# Expected values come from the seed in Data.java, the same as the reference server's.

# S01 The club page from Module 1, nested three levels
POST {{url}}
Content-Type: application/json
{"query": "{ club(id: \"c1\") { name owner { name } memberCount currentBook { title authors averageRating } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.name" == "The Midnight Readers"
jsonpath "$.data.club.owner.name" == "Kemi Adeyemi"
jsonpath "$.data.club.memberCount" == 8
jsonpath "$.data.club.currentBook.averageRating" == 4.2

# S02 members(first:) caps the list, in join order
POST {{url}}
Content-Type: application/json
{"query": "{ club(id: \"c1\") { members(first: 3) { handle } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.members" count == 3
jsonpath "$.data.club.members[1].handle" == "asha"

# S03 members default: 10, so all 8
POST {{url}}
Content-Type: application/json
{"query": "{ club(id: \"c1\") { members { id } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.members" count == 8

# S04 An unknown id is null, not an error
POST {{url}}
Content-Type: application/json
{"query": "{ club(id: \"zz\") { name } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club" == null
jsonpath "$.errors" not exists

# S05 books(titleContains:) ignores case
POST {{url}}
Content-Type: application/json
{"query": "{ books(titleContains: \"THE\") { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books" count == 6

# S06 books(first:) and catalogue order
POST {{url}}
Content-Type: application/json
{"query": "{ books(first: 2) { title } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books" count == 2
jsonpath "$.data.books[1].title" == "Piranesi"

# S07 reviews(last:) newest first
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b1\") { reviews(last: 2) { id rating author { handle } } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.reviews" count == 2
jsonpath "$.data.book.reviews[0].id" == "r5"
jsonpath "$.data.book.reviews[1].author.handle" == "jonasb"

# S08 A book nobody reviewed: averageRating is null
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b7\") { title averageRating reviews { id } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.averageRating" == null
jsonpath "$.data.book.reviews" count == 0

# S09 meetups: upcoming by default, all with upcomingOnly: false, in date order
POST {{url}}
Content-Type: application/json
{"query": "{ club(id: \"c1\") { next: meetups { id } all: meetups(upcomingOnly: false) { id club { id } } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.next" count == 2
jsonpath "$.data.club.all" count == 3
jsonpath "$.data.club.all[0].id" == "m1"
jsonpath "$.data.club.all[2].club.id" == "c1"

# S10 A user's clubs and reviews
POST {{url}}
Content-Type: application/json
{"query": "{ user(id: \"u1\") { name clubs { id } reviews { book { title } } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.user.clubs" count == 2
jsonpath "$.data.user.reviews" count == 3

# S11 viewer comes from the X-Shelfie-User header
POST {{url}}
Content-Type: application/json
X-Shelfie-User: u7
{"query": "{ viewer { name } }"}
HTTP 200
[Asserts]
jsonpath "$.data.viewer.name" == "Grace Kim"

# S12 No header, no viewer
POST {{url}}
Content-Type: application/json
{"query": "{ viewer { name } }"}
HTTP 200
[Asserts]
jsonpath "$.data.viewer" == null

# S13 createReview without a viewer: a userError, not an exception
POST {{url}}
Content-Type: application/json
{"query": "mutation { createReview(input: {bookId: \"b7\", rating: 4, body: \"Anarres forever\"}) { review { id } userErrors { message } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.createReview.review" == null
jsonpath "$.data.createReview.userErrors" count == 1

# S14 createReview as u4, then read it back through the book
POST {{url}}
Content-Type: application/json
X-Shelfie-User: u4
{"query": "mutation { createReview(input: {bookId: \"b7\", rating: 4, body: \"Anarres forever\"}) { review { id rating author { handle } book { averageRating } } userErrors { field } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.createReview.review.id" == "r16"
jsonpath "$.data.createReview.review.author.handle" == "devp"
jsonpath "$.data.createReview.review.book.averageRating" == 4.0
jsonpath "$.data.createReview.userErrors" count == 0

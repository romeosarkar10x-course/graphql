# Lab 13.3: a black-box contract for Shelfie's reads and createReview. No Java, no knowledge of the server:
# the same file runs against Module 5's graphql-java server and Module 13's Spring server.
# Run it against a freshly started server: C07 creates a review, and a second run would be a duplicate.
#   hurl --test --variable url=http://localhost:4000/graphql contract.hurl

# C01 books filters by title, ignoring case, in catalogue order, capped by first
POST {{url}}
Content-Type: application/json
{"query": "{ books(titleContains: \"the\", first: 3) { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books[*].id" count == 3
jsonpath "$.data.books[0].id" == "b1"
jsonpath "$.data.books[1].id" == "b3"
jsonpath "$.data.books[2].id" == "b4"

# C02 the filter is a substring match, not a prefix one
POST {{url}}
Content-Type: application/json
{"query": "{ books(titleContains: \"SUN\") { id title } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books" count == 1
jsonpath "$.data.books[0].title" == "Klara and the Sun"

# C03 without X-Shelfie-User nothing is stored, and the reason is data, not an error
POST {{url}}
Content-Type: application/json
{"query": "mutation { createReview(input: {bookId: \"b5\", rating: 5, body: \"Anon\"}) { review { id } userErrors { field message } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.createReview.review" == null
jsonpath "$.data.createReview.userErrors[0].message" contains "Sign in"

# C04 a rating of 6 is refused on the rating field
POST {{url}}
Content-Type: application/json
X-Shelfie-User: u7
{"query": "mutation { createReview(input: {bookId: \"b5\", rating: 6, body: \"Six!\"}) { review { id } userErrors { field } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.createReview.review" == null
jsonpath "$.data.createReview.userErrors[0].field[0]" == "input"
jsonpath "$.data.createReview.userErrors[0].field[1]" == "rating"

# C05 so is a rating of 0
POST {{url}}
Content-Type: application/json
X-Shelfie-User: u7
{"query": "mutation { createReview(input: {bookId: \"b5\", rating: 0, body: \"Zero\"}) { review { id } userErrors { field } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.createReview.review" == null
jsonpath "$.data.createReview.userErrors[0].field[1]" == "rating"

# C06 an unknown book is refused on the bookId field
POST {{url}}
Content-Type: application/json
X-Shelfie-User: u7
{"query": "mutation { createReview(input: {bookId: \"zz\", rating: 3, body: \"Who?\"}) { review { id } userErrors { field } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.createReview.review" == null
jsonpath "$.data.createReview.userErrors[0].field[1]" == "bookId"

# C07 a valid review is stored for the viewer
POST {{url}}
Content-Type: application/json
X-Shelfie-User: u7
{"query": "mutation { createReview(input: {bookId: \"b5\", rating: 5, body: \"Funny\"}) { review { id rating body author { id } book { id } } userErrors { message } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.createReview.userErrors" count == 0
jsonpath "$.data.createReview.review.rating" == 5
jsonpath "$.data.createReview.review.author.id" == "u7"
jsonpath "$.data.createReview.review.book.id" == "b5"

# C08 and the book now has it: the only review, so the average is its rating
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b5\") { averageRating reviews { rating author { handle } } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.averageRating" == 5.0
jsonpath "$.data.book.reviews" count == 1
jsonpath "$.data.book.reviews[0].author.handle" == "gracek"

# C09 the author's reviews include it too
POST {{url}}
Content-Type: application/json
{"query": "{ user(id: \"u7\") { reviews { book { id } } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.user.reviews[*].book.id" includes "b5"

# C10 the header names the viewer
POST {{url}}
Content-Type: application/json
X-Shelfie-User: u11
{"query": "{ viewer { name } }"}
HTTP 200
[Asserts]
jsonpath "$.data.viewer.name" == "Kemi Adeyemi"

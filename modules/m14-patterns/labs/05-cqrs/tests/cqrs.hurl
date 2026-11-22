# Lab 14.5: a command, then the read model catching up.

# C01 the command is accepted; the payload is honest: PENDING, and the book's figures do not count it yet
POST {{url}}
Content-Type: application/json
{"query": "mutation { createReview(input: {bookId: \"b1\", author: \"kim\", rating: 3, body: \"  Cold, and worth it.  \"}) { review { id status rating body } book { reviewCount averageRating } userErrors { message } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.createReview.userErrors" count == 0
jsonpath "$.data.createReview.review.status" == "PENDING"
jsonpath "$.data.createReview.review.body" == "Cold, and worth it."
jsonpath "$.data.createReview.book.reviewCount" == 2
jsonpath "$.data.createReview.book.averageRating" == 4.5
[Captures]
id: jsonpath "$.data.createReview.review.id"

# C02 within a few seconds the read model has it: PUBLISHED, counted, listed first
POST {{url}}
Content-Type: application/json
[Options]
retry: 20
retry-interval: 500
{"query": "{ review(id: \"{{id}}\") { status } book(id: \"b1\") { reviewCount averageRating reviews { id } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.review.status" == "PUBLISHED"
jsonpath "$.data.book.reviewCount" == 3
jsonpath "$.data.book.averageRating" == 4.0
jsonpath "$.data.book.reviews[0].id" == "{{id}}"

# C03 a refused command stores nothing
POST {{url}}
Content-Type: application/json
{"query": "mutation { createReview(input: {bookId: \"b2\", author: \"kim\", rating: 6, body: \"Too good\"}) { review { id } userErrors { field message } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.createReview.review" == null
jsonpath "$.data.createReview.userErrors[0].field[1]" == "rating"

# C04 an unknown review is null, not an error
POST {{url}}
Content-Type: application/json
{"query": "{ review(id: \"r-nope\") { status } }"}
HTTP 200
[Asserts]
jsonpath "$.data.review" == null
jsonpath "$.errors" not exists

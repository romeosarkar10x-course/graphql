# Game 10.10: the patched server must still be Shelfie.

# F01 The player can sign in
POST {{url}}
Content-Type: application/json
{"query": "mutation { login(username: \"ben\", password: \"ben-pw\") }"}
HTTP 200
[Captures]
ben: jsonpath "$.data.login"
[Asserts]
jsonpath "$.data.login" startsWith "ey"

# F02 Public clubs for anyone
POST {{url}}
Content-Type: application/json
{"query": "{ clubs(first: 12) { id name } }"}
HTTP 200
[Asserts]
jsonpath "$.data.clubs" count == 10

# F03 ben sees his own private club, its reviews and its welcome note
POST {{url}}
Authorization: Bearer {{ben}}
Content-Type: application/json
{"query": "{ club(id: \"c3\") { name welcomeNote reviews(first: 3) { body } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.name" == "Quiet Corner"
jsonpath "$.data.club.welcomeNote" == "Welcome to Quiet Corner!"
jsonpath "$.data.club.reviews" count == 3

# F04 Book search still finds books, by title
POST {{url}}
Content-Type: application/json
{"query": "{ searchBooks(titleContains: \"Lantern\") { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.searchBooks" count == 10

# F05 ben joins c1 with the right code (one honest attempt)
POST {{url}}
Authorization: Bearer {{ben}}
Content-Type: application/json
{"query": "mutation { joinClub(id: \"c1\", inviteCode: \"4821\") { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.joinClub.id" == "c1"

# F06 ben renames himself; that much of updateProfile is fine
POST {{url}}
Authorization: Bearer {{ben}}
Content-Type: application/json
{"query": "mutation { updateProfile(input: {name: \"Benedict\"}) { name } }"}
HTTP 200
[Asserts]
jsonpath "$.data.updateProfile.name" == "Benedict"

# F07 ben adds a review to his club with a normal JSON request
POST {{url}}
Authorization: Bearer {{ben}}
Content-Type: application/json
{"query": "mutation { addReview(clubId: \"c3\", bookId: \"b2\", rating: 4, body: \"still works\") { author { id } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.addReview.author.id" == "r2"

# F08 A normal review lookup works
POST {{url}}
Content-Type: application/json
{"query": "{ review(id: \"v31\") { id rating } }"}
HTTP 200
[Asserts]
jsonpath "$.data.review.id" == "v31"

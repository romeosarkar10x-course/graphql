# Lab 14.4: the book page over REST, gRPC and Open Library (a fake at {{fake}}, broken on purpose below).

# B00 Open Library is up
POST {{fake}}/_fake/reset
HTTP 200

# B01 one query, three systems
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b1\") { title authors isbn publishedYear reviews { rating body author { name } } openLibrary { url pages publishers subjects(first: 2) coverUrl } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.book.title" == "The Left Hand of Darkness"
jsonpath "$.data.book.isbn" == "9780441478125"
jsonpath "$.data.book.reviews" count == 3
jsonpath "$.data.book.reviews[0].body" == "Estraven deserved better."
jsonpath "$.data.book.reviews[0].author.name" == "Hugo Silva"
jsonpath "$.data.book.openLibrary.pages" == 304
jsonpath "$.data.book.openLibrary.publishers[0]" == "Ace Books"
jsonpath "$.data.book.openLibrary.subjects" count == 2
jsonpath "$.data.book.openLibrary.subjects[1]" == "Hugo Award Winner"
jsonpath "$.data.book.openLibrary.coverUrl" == "https://covers.openlibrary.org/b/id/6989406-M.jpg"
jsonpath "$.data.book.openLibrary.url" contains "/books/OL20525469M"

# B02 missing facts are null, not errors; an unknown book is null
POST {{url}}
Content-Type: application/json
{"query": "{ b2: book(id: \"b2\") { openLibrary { pages coverUrl subjects } } b3: book(id: \"b3\") { openLibrary { pages } } none: book(id: \"b999\") { title } }"}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.b2.openLibrary.pages" == 245
jsonpath "$.data.b2.openLibrary.coverUrl" == null
jsonpath "$.data.b2.openLibrary.subjects" count == 3
jsonpath "$.data.b3.openLibrary.pages" == null
jsonpath "$.data.none" == null

# B03 arguments reach the gRPC-backed field
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b1\") { reviews(last: 1) { id } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.reviews" count == 1
jsonpath "$.data.book.reviews[0].id" == "r5"

# F01 Open Library hangs for 5 s: the page answers within 1.5 s, without it
POST {{fake}}/_fake/mode?set=slow&ms=5000
HTTP 200

POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b4\") { title reviews { rating } openLibrary { pages } } }"}
HTTP 200
[Asserts]
duration < 1500
jsonpath "$.data.book.title" == "The Name of the Rose"
jsonpath "$.data.book.reviews" exists
jsonpath "$.data.book.openLibrary" == null
jsonpath "$.errors[0].path[1]" == "openLibrary"
jsonpath "$.errors[0].extensions.code" == "UPSTREAM_UNAVAILABLE"

# F02 Open Library answers an HTML maintenance page with 200
POST {{fake}}/_fake/mode?set=garbage
HTTP 200

POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b5\") { title openLibrary { pages } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.title" == "Good Omens"
jsonpath "$.data.book.openLibrary" == null
jsonpath "$.errors[0].extensions.code" == "UPSTREAM_UNAVAILABLE"

# F03 Open Library is down (503): every page still renders
POST {{fake}}/_fake/mode?set=down
HTTP 200

POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b6\") { title openLibrary { pages } } }"}
HTTP 200
[Asserts]
duration < 1500
jsonpath "$.data.book.title" == "Project Hail Mary"
jsonpath "$.errors[0].extensions.code" == "UPSTREAM_UNAVAILABLE"

# F04 twenty more pages while it is down: a breaker stops calling it
POST {{fake}}/_fake/reset
HTTP 200

POST {{fake}}/_fake/mode?set=down
HTTP 200

POST {{url}}
Content-Type: application/json
[Options]
repeat: 20
{"query": "{ book(id: \"b7\") { title openLibrary { pages } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.title" == "The Dispossessed"

GET {{fake}}/_fake/stats
HTTP 200
[Asserts]
jsonpath "$.requests" <= 8

# F05 Open Library is back: within 15 s the field is back too
POST {{fake}}/_fake/reset
HTTP 200

POST {{url}}
Content-Type: application/json
[Options]
retry: 30
retry-interval: 500
{"query": "{ book(id: \"b8\") { openLibrary { pages } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.openLibrary.pages" == 352

# F06 and the calls say who is calling (Open Library asks API users to)
GET {{fake}}/_fake/stats
HTTP 200
[Asserts]
jsonpath "$.userAgents[0]" contains "Shelfie"

# Mini-project 14.10. The gateway runs with gateway.ttl=PT5S; the fake Open Library at {{fake}} counts every call.

# G00 a clean count
POST {{fake}}/_fake/reset
HTTP 200

# G01 three aliases, a list, a repeat and an unknown ISBN: one Open Library call, each ISBN once
POST {{url}}
Content-Type: application/json
{"query": "{ a: book(isbn: \"9780441478125\") { isbn title authors publishers publishDate pages subjects(first: 2) coverUrl stale fetchedAt } b: book(isbn: \"9781635575637\") { title coverUrl } none: book(isbn: \"9789999999999\") { title } list: books(isbns: [\"9780679731726\", \"9780441478125\"]) { title pages } }"}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.a.title" == "The Left Hand of Darkness"
jsonpath "$.data.a.authors[0]" == "Ursula K. Le Guin"
jsonpath "$.data.a.publishers[0]" == "Ace Books"
jsonpath "$.data.a.publishDate" == "2003"
jsonpath "$.data.a.pages" == 304
jsonpath "$.data.a.subjects" count == 2
jsonpath "$.data.a.stale" == false
jsonpath "$.data.a.fetchedAt" matches /^\d{4}-\d\d-\d\dT/
jsonpath "$.data.b.coverUrl" == null
jsonpath "$.data.none" == null
jsonpath "$.data.list[0].title" == "The Remains of the Day"
jsonpath "$.data.list[0].pages" == null
jsonpath "$.data.list[1].pages" == 304

GET {{fake}}/_fake/stats
HTTP 200
[Asserts]
jsonpath "$.requests" == 1
jsonpath "$.bibkeys[0]" count == 4

# G02 the same page again, and the unknown ISBN again: from the cache, no call
POST {{url}}
Content-Type: application/json
{"query": "{ a: book(isbn: \"9780441478125\") { title } none: book(isbn: \"9789999999999\") { title } list: books(isbns: [\"9781635575637\"]) { title } }"}
HTTP 200
[Asserts]
jsonpath "$.data.a.title" == "The Left Hand of Darkness"
jsonpath "$.data.none" == null

GET {{fake}}/_fake/stats
HTTP 200
[Asserts]
jsonpath "$.requests" == 1

# G03 search, then the same search with other capitals: one call
POST {{url}}
Content-Type: application/json
{"query": "{ search(text: \"le guin\", first: 3) { total hits { key title authors firstPublishYear editionCount } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.search.total" == 716
jsonpath "$.data.search.hits" count == 3
jsonpath "$.data.search.hits[0].title" == "A Wizard of Earthsea"
jsonpath "$.data.search.hits[0].firstPublishYear" == 1968

POST {{url}}
Content-Type: application/json
{"query": "{ search(text: \"Le Guin\", first: 3) { total } }"}
HTTP 200
[Asserts]
jsonpath "$.data.search.total" == 716

GET {{fake}}/_fake/stats
HTTP 200
[Asserts]
jsonpath "$.searches" count == 1

# G04 Open Library hangs: an ISBN it never answered is an error within 1.5 s, the page goes on
POST {{fake}}/_fake/mode?set=slow&ms=5000
HTTP 200

POST {{url}}
Content-Type: application/json
{"query": "{ a: book(isbn: \"9780441478125\") { title stale } e: book(isbn: \"9780593135204\") { title } }"}
HTTP 200
[Asserts]
duration < 1500
jsonpath "$.data.a.title" == "The Left Hand of Darkness"
jsonpath "$.data.a.stale" == false
jsonpath "$.data.e" == null
jsonpath "$.errors[0].path[0]" == "e"
jsonpath "$.errors[0].extensions.code" == "UPSTREAM_UNAVAILABLE"

# G05 down, and the cached answers too old: served anyway, marked stale, with no error
POST {{fake}}/_fake/mode?set=down
HTTP 200

POST {{url}}
Content-Type: application/json
[Options]
delay: 5500
{"query": "{ a: book(isbn: \"9780441478125\") { title stale } f: book(isbn: \"9780385353304\") { title } }"}
HTTP 200
[Asserts]
jsonpath "$.data.a.title" == "The Left Hand of Darkness"
jsonpath "$.data.a.stale" == true
jsonpath "$.data.f" == null
jsonpath "$.errors" count == 1
jsonpath "$.errors[0].path[0]" == "f"

# G06 back up: after the cooldown the next page fetches again, fresh
POST {{fake}}/_fake/reset
HTTP 200

POST {{url}}
Content-Type: application/json
[Options]
delay: 2500
{"query": "{ a: book(isbn: \"9780441478125\") { stale } f: book(isbn: \"9780385353304\") { title } }"}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.a.stale" == false
jsonpath "$.data.f.title" == "Station Eleven"

# G07 a 429 with Retry-After: the gateway waits, and the next pages do not call at all
POST {{fake}}/_fake/reset
HTTP 200

POST {{fake}}/_fake/mode?set=ratelimit
HTTP 200

POST {{url}}
Content-Type: application/json
{"query": "{ g: book(isbn: \"9780593318171\") { title } }"}
HTTP 200
[Asserts]
jsonpath "$.data.g" == null
jsonpath "$.errors[0].extensions.code" == "UPSTREAM_UNAVAILABLE"

POST {{url}}
Content-Type: application/json
[Options]
repeat: 5
{"query": "{ h: book(isbn: \"9781324064824\") { title } }"}
HTTP 200
[Asserts]
jsonpath "$.data.h" == null

GET {{fake}}/_fake/stats
HTTP 200
[Asserts]
jsonpath "$.requests" == 1
jsonpath "$.userAgents[0]" contains "Shelfie"

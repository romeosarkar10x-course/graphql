# Scenario 2: "After a rename, the club page shows the old title until the server restarts."
# Club 1 reads book 32.
POST {{url}}
Content-Type: application/json
{"query": "{ club(id: 1) { currentBook { id } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.currentBook.id" == "32"

POST {{url}}
Content-Type: application/json
{"query": "mutation { renameBook(id: 32, title: \"Thirty-Two, Renamed\") { title } }"}
HTTP 200

POST {{url}}
Content-Type: application/json
{"query": "{ club(id: 1) { name currentBook { title } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.currentBook.title" == "Thirty-Two, Renamed"

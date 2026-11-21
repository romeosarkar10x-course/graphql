# Scenario 4: "The same page, asked the same way twice, still shows the title from before the rename."
POST {{url}}
Content-Type: application/json
{"query": "{ club(id: 2) { currentBook { title } } }"}
HTTP 200

POST {{url}}
Content-Type: application/json
{"query": "mutation { renameBook(id: 63, title: \"Sixty-Three, Renamed\") { title } }"}
HTTP 200

POST {{url}}
Content-Type: application/json
{"query": "{ club(id: 2) { currentBook { title } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.currentBook.title" == "Sixty-Three, Renamed"

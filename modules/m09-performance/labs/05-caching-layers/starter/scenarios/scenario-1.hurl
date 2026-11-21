# Scenario 1: "I renamed a book and the same request still shows the old title."
# Reader 5 reviews book 11, renames it, and reviews it again, in one mutation request.
POST {{url}}
X-Shelfie-User: 5
Content-Type: application/json
{"query": "mutation { before: addReview(bookId: 11, rating: 4, body: \"first\") { book { title } } rename: renameBook(id: 11, title: \"Eleven, Renamed\") { title } after: addReview(bookId: 11, rating: 5, body: \"second\") { book { title } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.rename.title" == "Eleven, Renamed"
jsonpath "$.data.after.book.title" == "Eleven, Renamed"

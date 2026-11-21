# Incident 4

**Ticket:** "Rating a book from the new web form never works. The old form did."

**Request**

```graphql
mutation RateBook($bookId: ID!, $rating: Int!) {
  rateBook(bookId: $bookId, rating: $rating) { id }
}
```

Variables: `{"rating":"5","bookId":"b1"}`

**Response**

```json
{
  "errors" : [ {
    "message" : "Variable 'rating' has an invalid value: Expected a value that can be converted to type 'Int' but it was a 'String'",
    "locations" : [ {
      "line" : 1,
      "column" : 33
    } ],
    "extensions" : {
      "classification" : "ValidationError"
    }
  } ]
}
```

**Server log (excerpt)**

```
2026-11-19T21:13:58.120Z INFO  [HikariPool] shelfie - Pool stats (total=10, active=1, idle=9, waiting=0)
2026-11-19T21:14:00.170Z INFO  [GraphQlHttpHandler] POST /graphql operation=RateBook
2026-11-19T21:14:00.172Z WARN  [GraphQlHttpHandler] error ValidationError: Variable 'rating' has an invalid value: Expected a value that can be converted to type 'Int' but it was a 'String'
```

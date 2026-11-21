# Incident 5

**Ticket:** "The page for The Left Hand of Darkness never loads. Other books are fine."

**Request**

```graphql
query BookPage {
  book(id: "b1") {
    title
    reviews { rating }
  }
}
```

**Response**

```json
{
  "errors" : [ {
    "message" : "Execution has been asked to be cancelled",
    "extensions" : {
      "classification" : "ExecutionAborted"
    }
  } ]
}
```

**Server log (excerpt)**

```
2026-11-19T21:13:58.120Z INFO  [HikariPool] shelfie - Pool stats (total=10, active=1, idle=9, waiting=0)
2026-11-19T21:14:00.173Z INFO  [Budget] execution budget 500 ms (shelfie.graphql.timeout)
2026-11-19T21:14:00.176Z INFO  [GraphQlHttpHandler] POST /graphql operation=BookPage
2026-11-19T21:14:00.178Z DEBUG [ReviewRepository] select r.* from review r where r.book_id = ?
2026-11-19T21:14:01.079Z DEBUG [ReviewRepository] reviews of b1 fetched in 900 ms
2026-11-19T21:14:01.080Z WARN  [GraphQlHttpHandler] error ExecutionAborted: Execution has been asked to be cancelled
```

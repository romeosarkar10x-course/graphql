# Incident 2

**Ticket:** "Club c1's meetup list shows an error, and support only has 'Internal error'."

**Request**

```graphql
query ClubMeetups {
  club(id: "c1") {
    name
    meetups { title startsAt }
  }
}
```

**Response**

```json
{
  "errors" : [ {
    "message" : "Internal error",
    "locations" : [ {
      "line" : 4,
      "column" : 21
    } ],
    "path" : [ "club", "meetups", 2, "startsAt" ],
    "extensions" : {
      "code" : "INTERNAL",
      "errorId" : "42707928",
      "classification" : "DataFetchingException"
    }
  } ],
  "data" : {
    "club" : null
  }
}
```

**Server log (excerpt)**

```
2026-11-19T21:13:58.120Z INFO  [HikariPool] shelfie - Pool stats (total=10, active=1, idle=9, waiting=0)
2026-11-19T21:14:00.146Z ERROR [ErrorResolver] errorId=5be13d2a path=/book/coverUrl
    java.lang.IllegalStateException: S3 GetObject failed: connect timed out
        at dev.shelfie.boot.ErrorController.coverUrl(ErrorController.java:61)
2026-11-19T21:14:00.155Z INFO  [GraphQlHttpHandler] POST /graphql operation=ClubMeetups
2026-11-19T21:14:00.160Z ERROR [ErrorResolver] errorId=42707928 path=/club/meetups[2]/startsAt
    java.time.format.DateTimeParseException: Text '2026-11-31' could not be parsed: Invalid date 'NOVEMBER 31'
        at java.base/java.time.format.DateTimeFormatter.createError(DateTimeFormatter.java:2079)
        at java.base/java.time.format.DateTimeFormatter.parse(DateTimeFormatter.java:2014)
        at java.base/java.time.LocalDate.parse(LocalDate.java:437)
```

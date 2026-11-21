# Incident 3

**Ticket:** "After Tuesday's deploy the iOS app (version 4.2, released in June) shows 'Something went wrong' on every club. Android 5.0 works."

**Request**

```graphql
query ClubHeader {
  club(id: "c1") {
    name
    memberCount
  }
}
```

**Response**

```json
{
  "errors" : [ {
    "message" : "Validation error (FieldUndefined@[club/memberCount]) : Field 'memberCount' in type 'Club' is undefined",
    "locations" : [ {
      "line" : 4,
      "column" : 5
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
2026-11-19T21:14:00.165Z INFO  [GraphQlHttpHandler] POST /graphql operation=ClubHeader
2026-11-19T21:14:00.166Z WARN  [GraphQlHttpHandler] error ValidationError: Validation error (FieldUndefined@[club/memberCount]) : Field 'memberCount' in type 'Club' is undefined
```

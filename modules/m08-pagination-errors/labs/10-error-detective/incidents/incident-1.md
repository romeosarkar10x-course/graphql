# Incident 1

**Ticket:** "The Midnight Readers page is completely blank since this morning's data import. Other clubs are fine."

**Request**

```graphql
query ClubPage {
  club(id: "c1") {
    name
    members { name handle }
  }
}
```

**Response**

```json
{
  "errors" : [ {
    "message" : "The field at path '/club/members[4]/handle' was declared as a non null type, but the code involved in retrieving data has wrongly returned a null value.  The graphql specification requires that the parent field be set to null, or if that is non nullable that it bubble up null to its parent and so on. The non-nullable type is 'String' within parent type 'User'",
    "path" : [ "club", "members", 4, "handle" ],
    "extensions" : {
      "classification" : "NullValueInNonNullableField"
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
2026-11-19T21:14:00.089Z INFO  [GraphQlHttpHandler] POST /graphql operation=ClubPage
2026-11-19T21:14:00.120Z WARN  [GraphQlHttpHandler] error NullValueInNonNullableField: The field at path '/club/members[4]/handle' was declared as a non null type, but the code involved in retrieving data has wrongly returned a null value.  The graphql specification requires that the parent field be set to null, or if that is non nullable that it bubble up null to its parent and so on. The non-nullable type is 'String' within parent type 'User'
```

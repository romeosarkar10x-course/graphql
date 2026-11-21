# Incident 6

**Ticket:** "Since the 'faster owner lookup' change, every club page is blank."

**Request**

```graphql
query ClubOwner {
  club(id: "c1") {
    name
    owner { name handle }
  }
}
```

**Response**

```json
{
  "errors" : [ {
    "message" : "The field at path '/club/owner/name' was declared as a non null type, but the code involved in retrieving data has wrongly returned a null value.  The graphql specification requires that the parent field be set to null, or if that is non nullable that it bubble up null to its parent and so on. The non-nullable type is 'String' within parent type 'User'",
    "path" : [ "club", "owner", "name" ],
    "extensions" : {
      "classification" : "NullValueInNonNullableField"
    }
  }, {
    "message" : "The field at path '/club/owner/handle' was declared as a non null type, but the code involved in retrieving data has wrongly returned a null value.  The graphql specification requires that the parent field be set to null, or if that is non nullable that it bubble up null to its parent and so on. The non-nullable type is 'String' within parent type 'User'",
    "path" : [ "club", "owner", "handle" ],
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
2026-11-19T21:14:01.084Z INFO  [GraphQlHttpHandler] POST /graphql operation=ClubOwner
2026-11-19T21:14:01.086Z WARN  [GraphQlHttpHandler] error NullValueInNonNullableField: The field at path '/club/owner/name' was declared as a non null type, but the code involved in retrieving data has wrongly returned a null value.  The graphql specification requires that the parent field be set to null, or if that is non nullable that it bubble up null to its parent and so on. The non-nullable type is 'String' within parent type 'User'
2026-11-19T21:14:01.087Z WARN  [GraphQlHttpHandler] error NullValueInNonNullableField: The field at path '/club/owner/handle' was declared as a non null type, but the code involved in retrieving data has wrongly returned a null value.  The graphql specification requires that the parent field be set to null, or if that is non nullable that it bubble up null to its parent and so on. The non-nullable type is 'String' within parent type 'User'
```

# Hints, lab 7.5

1. The union's member types are the interface's object types without `implements FeedItem`; the
   shared fields stay on each of them, because a union has no fields of its own.
2. In the union client nothing can be selected on `FeedItem` except `__typename`: every field goes inside
   `... on ReviewPosted { ... }`. In the enum client there are no fragments at all: one flat selection.
3. "The v1 client is still valid against v2" fails when v2 renamed or removed something the v1 client
   selects. v2 should be v1 plus additions only: a new member or implementation, a new enum value, a new
   nullable field.
